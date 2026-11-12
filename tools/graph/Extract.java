import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Phase 1 static extractor, built for navigating endpoint -> controller -> service -> repository -> store:
 *
 *   (Endpoint {accepts, returns, statuses})-[HANDLED_BY]->(Method {layer, params, returns})
 *   (Method)-[CALLS {passes, returns}]->(Method)
 *   (Method)-[READS|WRITES {technology, command, key, valueType, summary}]->(Store)
 *
 * Only public methods (and endpoint handlers) are nodes. A private helper's calls and data access are folded into
 * the public method that reaches it, so the graph stays at the level a reader navigates.
 * Usage: java -cp javaparser-core.jar Extract.java <src/main/java dir> <out.cypher> [<markdown vault dir>]
 */
public class Extract {
    static final Map<String, String> KIND = Map.of(
            "RestController", "controller", "Controller", "controller", "Service", "service",
            "Repository", "repository", "Configuration", "config", "Component", "component");
    static final Map<String, String> HTTP = Map.of(
            "GetMapping", "GET", "PostMapping", "POST", "PutMapping", "PUT",
            "DeleteMapping", "DELETE", "PatchMapping", "PATCH");
    // opsFor<Type>().<method> -> Redis command and whether it reads or writes
    static final Map<String, String[]> REDIS = Map.of(
            "opsForValue.set", new String[]{"WRITES", "SET"}, "opsForValue.get", new String[]{"READS", "GET"},
            "opsForValue.multiGet", new String[]{"READS", "MGET"}, "opsForSet.add", new String[]{"WRITES", "SADD"},
            "opsForSet.members", new String[]{"READS", "SMEMBERS"}, "opsForSet.remove", new String[]{"WRITES", "SREM"});
    static final Map<String, String[]> FILES = Map.of(
            "copy", new String[]{"WRITES", "Files.copy"}, "write", new String[]{"WRITES", "Files.write"},
            "createDirectories", new String[]{"WRITES", "Files.createDirectories"},
            "exists", new String[]{"READS", "Files.exists"}, "isReadable", new String[]{"READS", "Files.isReadable"});
    static final Map<String, String> STATUS = Map.of("created", "201", "ok", "200", "notFound", "404", "badRequest", "400",
            "UNSUPPORTED_MEDIA_TYPE", "415", "INTERNAL_SERVER_ERROR", "500", "NOT_FOUND", "404", "BAD_REQUEST", "400");
    static final String UPLOAD_STORE = "filesystem:upload.dir";

    record Cls(String name, String kind, String base, ClassOrInterfaceDeclaration decl) {}

    static final Map<String, Map<String, String>> methods = new LinkedHashMap<>(); // method id -> props
    static final Set<String> used = new LinkedHashSet<>();                         // method ids that get a node
    static final Map<String, Map<String, String>> endpoints = new LinkedHashMap<>();
    static final Map<String, Set<String>> deps = new LinkedHashMap<>();           // class -> injected component classes
    static final Set<String> stores = new LinkedHashSet<>();
    static final List<String> edges = new ArrayList<>();
    static final Map<String, String[]> callEdges = new LinkedHashMap<>();       // from|to -> {from,to,passes,returns}
    static final Map<String, Map<String, String>> dataEdges = new LinkedHashMap<>(); // aggregated store edges

    public static void main(String[] a) throws Exception {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        List<Cls> classes = new ArrayList<>();
        try (Stream<Path> s = Files.walk(Path.of(a[0]))) {
            for (Path p : s.filter(f -> f.toString().endsWith(".java")).sorted().toList()) {
                CompilationUnit cu = StaticJavaParser.parse(p);
                for (ClassOrInterfaceDeclaration d : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                    String kind = d.getAnnotations().stream().map(x -> KIND.get(x.getNameAsString()))
                            .filter(Objects::nonNull).findFirst().orElse(null);
                    if (kind != null) classes.add(new Cls(d.getNameAsString(), kind, basePath(d), d));
                }
            }
        }
        Map<String, Cls> byName = classes.stream().collect(Collectors.toMap(Cls::name, c -> c));

        for (Cls c : classes) {
            Map<String, String> fieldTypes = new HashMap<>(); // injected field name -> component class
            for (ConstructorDeclaration k : c.decl.getConstructors())
                for (Parameter p : k.getParameters())
                    if (byName.containsKey(p.getType().asString())) fieldTypes.put(p.getNameAsString(), p.getType().asString());
            deps.put(c.name, new LinkedHashSet<>(fieldTypes.values()));
            Map<String, String> keys = keyConstants(c);
            String valueType = valueType(c);

            for (CallableDeclaration<?> m : publicCallables(c)) {
                String mid = c.name + "." + (m instanceof ConstructorDeclaration ? "constructor" : m.getNameAsString());
                methods.put(mid, props("layer", c.kind, "component", c.name,
                        "params", params(m), "returns", returnType(m)));
                List<CallableDeclaration<?>> reach = reachable(c, m);
                for (CallableDeclaration<?> r : reach) {
                    endpoints(c, m, r, mid);
                    calls(c, r, mid, fieldTypes, byName);
                    data(c, r, mid, keys, valueType, reach);
                }
            }
        }
        write(Path.of(a[1]));
        if (a.length > 2) writeVault(Path.of(a[2]));
    }

    // ---- reachability: a public method plus the private same-class helpers it (transitively) calls

    static List<CallableDeclaration<?>> reachable(Cls c, CallableDeclaration<?> start) {
        List<CallableDeclaration<?>> out = new ArrayList<>(List.of(start));
        for (int i = 0; i < out.size(); i++) {
            CallableDeclaration<?> cur = out.get(i);
            Set<String> names = new LinkedHashSet<>();
            for (MethodCallExpr call : cur.findAll(MethodCallExpr.class))
                if (call.getScope().isEmpty() || call.getScope().get() instanceof ThisExpr) names.add(call.getNameAsString());
            for (MethodReferenceExpr ref : cur.findAll(MethodReferenceExpr.class))
                if (ref.getScope() instanceof ThisExpr) names.add(ref.getIdentifier());
            for (String n : names)
                for (MethodDeclaration p : c.decl.getMethodsByName(n))
                    if (p.isPrivate() && !out.contains(p)) out.add(p);
        }
        return out;
    }

    static List<CallableDeclaration<?>> publicCallables(Cls c) {
        List<CallableDeclaration<?>> l = new ArrayList<>();
        c.decl.getMethods().stream().filter(MethodDeclaration::isPublic).forEach(l::add);
        c.decl.getConstructors().stream().filter(ConstructorDeclaration::isPublic).forEach(l::add);
        return l;
    }

    // ---- endpoints

    static void endpoints(Cls c, CallableDeclaration<?> top, CallableDeclaration<?> m, String mid) {
        if (m != top) return; // annotations and resource handlers live on the public method itself
        for (AnnotationExpr an : m.getAnnotations()) {
            String verb = HTTP.get(an.getNameAsString());
            if (verb == null) continue;
            endpoint(verb, c.base + annotationValue(an), mid, props("accepts", accepts(m), "returns", returnType(m),
                    "statuses", statuses(m)));
        }
        for (MethodCallExpr call : m.findAll(MethodCallExpr.class))
            if (call.getNameAsString().equals("addResourceHandler") && call.getArgument(0) instanceof StringLiteralExpr sl) {
                endpoint("GET", sl.getValue(), mid, props("accepts", "-", "returns", "file bytes (static resource)", "statuses", "200, 404"));
                storeEdge(mid, "READS", UPLOAD_STORE, "filesystem", "serve " + sl.getValue(), "upload.dir", "file");
            }
    }

    static void endpoint(String verb, String path, String mid, Map<String, String> p) {
        String id = verb + " " + path;
        p.put("method", verb);
        p.put("path", path);
        p.put("name", id);
        p.put("handler", mid);
        endpoints.put(id, p);
        used.add(mid);
        edges.add("MATCH (e:Endpoint {name:'%s'}), (m:Method {name:'%s'}) MERGE (e)-[r:HANDLED_BY]->(m) SET r.source='static'"
                .formatted(q(id), q(mid)));
    }

    static String accepts(CallableDeclaration<?> m) {
        List<String> out = new ArrayList<>();
        for (Parameter p : m.getParameters()) {
            String where = p.isAnnotationPresent("RequestBody") ? "body" : p.isAnnotationPresent("PathVariable") ? "path"
                    : p.isAnnotationPresent("RequestParam") ? "param" : null;
            if (where != null) out.add(where + ": " + p.getType() + " " + p.getNameAsString());
        }
        return out.isEmpty() ? "-" : String.join("; ", out);
    }

    static String statuses(CallableDeclaration<?> m) {
        Set<String> out = new TreeSet<>();
        for (MethodCallExpr call : m.findAll(MethodCallExpr.class)) {
            String scope = call.getScope().map(Node::toString).orElse("");
            if (scope.equals("ResponseEntity") && STATUS.containsKey(call.getNameAsString())) out.add(STATUS.get(call.getNameAsString()));
            if (scope.equals("ResponseEntity") && call.getNameAsString().equals("status") && !call.getArguments().isEmpty())
                out.add(STATUS.getOrDefault(call.getArgument(0).toString().replace("HttpStatus.", ""), call.getArgument(0).toString()));
        }
        return out.isEmpty() ? "200" : String.join(", ", out);
    }

    // ---- calls between public methods of different components

    static void calls(Cls c, CallableDeclaration<?> m, String mid, Map<String, String> fieldTypes, Map<String, Cls> byName) {
        for (MethodCallExpr call : m.findAll(MethodCallExpr.class)) {
            if (call.getScope().isEmpty() || !(call.getScope().get() instanceof NameExpr n)) continue;
            String target = fieldTypes.get(n.getNameAsString());
            if (target == null) continue;
            for (MethodDeclaration t : byName.get(target).decl().getMethodsByName(call.getNameAsString()))
                if (t.isPublic()) {
                    String to = target + "." + t.getNameAsString();
                    used.add(mid);
                    used.add(to);
                    String passes = call.getArguments().stream().map(Node::toString).map(Extract::shorten).collect(Collectors.joining(", "));
                    callEdges.put(mid + "|" + to, new String[]{mid, to, passes, t.getType().asString()});
                    edges.add(("MATCH (a:Method {name:'%s'}), (b:Method {name:'%s'}) MERGE (a)-[r:CALLS]->(b) " +
                            "SET r.source='static', r.passes='%s', r.returns='%s'")
                            .formatted(q(mid), q(to), q(passes), q(t.getType().asString())));
                    break;
                }
        }
    }

    // ---- store access

    static void data(Cls c, CallableDeclaration<?> m, String mid, Map<String, String> keys, String valueType,
                     List<CallableDeclaration<?>> reach) {
        boolean transactional = reach.stream().anyMatch(r -> r.findAll(MethodCallExpr.class).stream()
                .anyMatch(x -> x.getNameAsString().equals("multi")));
        for (MethodCallExpr call : m.findAll(MethodCallExpr.class)) {
            String n = call.getNameAsString();
            String scope = call.getScope().map(Node::toString).orElse("");
            String ops = scope.contains("opsFor") ? scope.substring(scope.indexOf("opsFor")).replace("()", "") : "";
            String[] hit = REDIS.get(ops + "." + n);
            if (hit == null && n.equals("delete") && scope.matches(".*(emplate|ops)")) hit = new String[]{"WRITES", "DEL"};
            if (hit != null) {
                for (String k : keysIn(call, m, keys)) {
                    String key = keys.get(k) + (k.endsWith("PREFIX") ? "<id>" : "");
                    boolean isSet = ops.equals("opsForSet");
                    storeEdge(mid, hit[0], "redis:" + key, "Redis", hit[1] + (transactional && hit[0].equals("WRITES") ? " (MULTI/EXEC)" : ""),
                            key, isSet ? "set of ids" : valueType);
                }
            } else if (scope.equals("Files") && FILES.containsKey(n)) {
                storeEdge(mid, FILES.get(n)[0], UPLOAD_STORE, "filesystem", FILES.get(n)[1], "upload.dir", "file");
            }
        }
    }

    static void storeEdge(String mid, String rel, String store, String tech, String command, String key, String valueType) {
        stores.add(store);
        used.add(mid);
        String id = mid + "|" + rel + "|" + store;
        Map<String, String> p = dataEdges.computeIfAbsent(id, x -> props("mid", mid, "rel", rel, "store", store,
                "technology", tech, "command", "", "key", key, "valueType", valueType));
        String cur = p.get("command");
        if (!Arrays.asList(cur.split(", ")).contains(command)) p.put("command", cur.isEmpty() ? command : cur + ", " + command);
    }

    // ---- output

    static void write(Path out) throws Exception {
        List<String> l = new ArrayList<>();
        endpoints.values().forEach(p -> l.add("MERGE (e:Endpoint {name:'%s'}) SET e += %s;".formatted(q(p.get("name")), map(p))));
        methods.forEach((id, p) -> {
            if (used.contains(id)) l.add("MERGE (m:Method {name:'%s'}) SET m += %s;".formatted(q(id), map(p)));
        });
        stores.forEach(s -> l.add("MERGE (s:Store {name:'%s'}) SET s.technology='%s';".formatted(q(s), q(s.split(":", 2)[0]))));
        l.addAll(edges.stream().map(e -> e + ";").toList());
        dataEdges.values().forEach(p -> {
            String summary = "%s %s %s%s".formatted(p.get("technology"), p.get("rel").equals("READS") ? "READ" : "WRITE",
                    p.get("command"), p.get("key").isEmpty() ? "" : " on " + p.get("key"));
            p.put("summary", summary);
            l.add(("MATCH (a:Method {name:'%s'}), (b:Store {name:'%s'}) MERGE (a)-[r:%s]->(b) SET r.source='static', r.technology='%s', " +
                    "r.command='%s', r.key='%s', r.valueType='%s', r.summary='%s';")
                    .formatted(q(p.get("mid")), q(p.get("store")), p.get("rel"), q(p.get("technology")), q(p.get("command")),
                            q(p.get("key")), q(p.get("valueType")), q(summary)));
        });
        Files.write(out, l);
        System.out.println(endpoints.size() + " endpoints, " + used.size() + " methods, " + stores.size() + " stores, "
                + (edges.size() + dataEdges.size()) + " edges -> " + out);
    }

    // ---- markdown vault: one note per endpoint / method / store; hand-written text below the generated block survives re-runs

    static final String START = "<!-- generated:start -->", END = "<!-- generated:end -->";

    static void writeVault(Path dir) throws Exception {
        Files.createDirectories(dir);
        for (var e : endpoints.values()) {
            StringBuilder b = new StringBuilder("# " + e.get("name") + "\n\n");
            b.append("- **Accepts:** ").append(e.get("accepts")).append("\n- **Returns:** `").append(e.get("returns"))
                    .append("`\n- **Status codes:** ").append(e.get("statuses")).append("\n\n## Flow\n\nFollow the link to the first step; each step links to the next.\n\n");
            flow(b, e.get("handler"), 0, new HashSet<>());
            note(dir, slug(e.get("name")), b.toString());
        }
        for (String mid : methods.keySet()) {
            if (!used.contains(mid)) continue;
            var m = methods.get(mid);
            StringBuilder b = new StringBuilder("# " + mid + "\n\n");
            b.append("- **Layer:** ").append(m.get("layer")).append("\n- **Signature:** `").append(m.get("returns")).append(" ")
                    .append(mid.substring(mid.indexOf('.') + 1)).append("(").append(m.get("params")).append(")`\n");
            if (!m.get("layer").equals("controller")) // controllers stay as plain method notes, no class parent
                section(b, "Part of", List.of(link(m.get("component"), m.get("component"))));
            section(b, "Exposed as", endpoints.values().stream().filter(e -> e.get("handler").equals(mid))
                    .map(e -> link(slug(e.get("name")), e.get("name"))).toList());
            section(b, "Called by", callEdges.values().stream().filter(c -> c[1].equals(mid))
                    .map(c -> link(c[0], c[0]) + " passing `" + c[2] + "`").toList());
            section(b, "Calls", callEdges.values().stream().filter(c -> c[0].equals(mid))
                    .map(c -> link(c[1], c[1]) + " passing `" + c[2] + "`, returns `" + c[3] + "`").toList());
            section(b, "Touches stores", dataEdges.values().stream().filter(d -> d.get("mid").equals(mid))
                    .map(d -> storeLine(d, true)).toList());
            note(dir, mid, b.toString());
        }
        for (String cls : methods.values().stream().map(m -> m.get("component")).distinct().toList()) {
            List<String> ops = methods.entrySet().stream()
                    .filter(m -> used.contains(m.getKey()) && m.getValue().get("component").equals(cls)).map(Map.Entry::getKey).toList();
            if (ops.isEmpty() || methods.get(ops.get(0)).get("layer").equals("controller")) continue;
            var first = methods.get(ops.get(0));
            StringBuilder b = new StringBuilder("# " + cls + "\n\n- **Layer:** " + first.get("layer") + "\n");
            if (!deps.get(cls).isEmpty()) b.append("- **Calls repository:** ").append(deps.get(cls).stream().map(d -> link(d, d)).collect(Collectors.joining(", "))).append("\n");
            section(b, "Operations", ops.stream().map(id -> link(id, id) + " — `" + methods.get(id).get("returns") + " "
                    + id.substring(id.indexOf('.') + 1) + "(" + methods.get(id).get("params") + ")`").toList());
            note(dir, cls, b.toString());
        }
        for (String store : stores) {
            StringBuilder b = new StringBuilder("# " + store + "\n");
            for (String rel : List.of("READS", "WRITES"))
                section(b, rel.equals("READS") ? "Read by" : "Written by", dataEdges.values().stream()
                        .filter(d -> d.get("store").equals(store) && d.get("rel").equals(rel))
                        .map(d -> link(d.get("mid"), d.get("mid")) + " (" + d.get("command") + ", " + d.get("valueType") + ")").toList());
            note(dir, slug(store), b.toString());
        }
        System.out.println("markdown vault -> " + dir);
    }

    /** Indented call tree from a handler: each call with what it passes/returns, each store hit with its operation. */
    static void flow(StringBuilder b, String mid, int depth, Set<String> path) {
        String pad = "  ".repeat(depth);
        b.append(pad).append("- ").append(depth == 0 ? link(mid, mid) : "**" + mid + "**").append(" _(").append(methods.get(mid).get("layer")).append(")_\n");
        if (!path.add(mid)) return;
        for (var c : callEdges.values())
            if (c[0].equals(mid)) {
                b.append(pad).append("  - passes `").append(c[2]).append("` → returns `").append(c[3]).append("`\n");
                flow(b, c[1], depth + 2, path);
            }
        for (var d : dataEdges.values())
            if (d.get("mid").equals(mid)) b.append(pad).append("  - ").append(storeLine(d, false)).append("\n");
        path.remove(mid);
    }

    /** Linked only from method notes, so each step links to its next hop and the endpoint note doesn't fan out. */
    static String storeLine(Map<String, String> d, boolean linked) {
        return "**" + d.get("technology") + " " + (d.get("rel").equals("READS") ? "READ" : "WRITE") + "** `" + d.get("command")
                + "` " + (linked ? link(slug(d.get("store")), d.get("store")) : d.get("store")) + " (" + d.get("valueType") + ")";
    }

    static void section(StringBuilder b, String title, List<String> items) {
        if (items.isEmpty()) return;
        b.append("\n## ").append(title).append("\n\n");
        items.forEach(i -> b.append("- ").append(i).append("\n"));
    }

    static String link(String target, String label) {
        return target.equals(label) ? "[[" + target + "]]" : "[[" + target + "|" + label + "]]";
    }

    static String slug(String s) {
        return s.replaceAll("[^A-Za-z0-9.]+", "_").replaceAll("^_|_$", "");
    }

    static void note(Path dir, String name, String generated) throws Exception {
        Path f = dir.resolve(name + ".md");
        String tail = "\n\n## Notes\n\n";
        if (Files.exists(f)) {
            String old = Files.readString(f);
            int i = old.indexOf(END);
            if (i >= 0) tail = old.substring(i + END.length());
        }
        Files.writeString(f, START + "\n" + generated.stripTrailing() + "\n" + END + tail);
    }

    // ---- helpers

    static Map<String, String> keyConstants(Cls c) {
        Map<String, String> keys = new LinkedHashMap<>(); // constant name -> key pattern
        for (FieldDeclaration f : c.decl.getFields())
            if (f.isStatic() && f.isFinal())
                for (VariableDeclarator v : f.getVariables())
                    if (v.getInitializer().orElse(null) instanceof StringLiteralExpr s) keys.put(v.getNameAsString(), s.getValue());
        return keys;
    }

    /** Value type stored in Redis: RedisTemplate<String, T> or a JSON serializer for T. */
    static String valueType(Cls c) {
        for (FieldDeclaration f : c.decl.getFields())
            if (f.getElementType() instanceof ClassOrInterfaceType t && t.getTypeArguments().isPresent()) {
                var args = t.getTypeArguments().get();
                if (t.getNameAsString().equals("RedisTemplate") && args.size() == 2) return args.get(1).asString() + " (JSON)";
                if (t.getNameAsString().endsWith("Serializer") && args.size() == 1) return args.get(0).asString() + " (JSON)";
            }
        return "String";
    }

    static Set<String> keysIn(MethodCallExpr call, CallableDeclaration<?> m, Map<String, String> keys) {
        Set<String> out = new LinkedHashSet<>();
        for (Expression arg : call.getArguments())
            for (NameExpr n : arg.findAll(NameExpr.class)) {
                if (keys.containsKey(n.getNameAsString())) out.add(n.getNameAsString());
                for (VariableDeclarator v : m.findAll(VariableDeclarator.class)) // follow a local variable one hop
                    if (v.getNameAsString().equals(n.getNameAsString()) && v.getInitializer().isPresent())
                        for (NameExpr i : v.getInitializer().get().findAll(NameExpr.class))
                            if (keys.containsKey(i.getNameAsString())) out.add(i.getNameAsString());
            }
        return out;
    }

    static String params(CallableDeclaration<?> m) {
        return m.getParameters().stream().map(p -> p.getType() + " " + p.getNameAsString()).collect(Collectors.joining(", "));
    }

    static String returnType(CallableDeclaration<?> m) {
        return m instanceof MethodDeclaration md ? md.getType().asString() : "void";
    }

    static String basePath(ClassOrInterfaceDeclaration d) {
        return d.getAnnotationByName("RequestMapping").map(Extract::annotationValue).orElse("");
    }

    static String annotationValue(AnnotationExpr an) {
        if (an instanceof SingleMemberAnnotationExpr s && s.getMemberValue() instanceof StringLiteralExpr l) return l.getValue();
        if (an instanceof NormalAnnotationExpr n)
            for (MemberValuePair p : n.getPairs())
                if ((p.getNameAsString().equals("value") || p.getNameAsString().equals("path"))
                        && p.getValue() instanceof StringLiteralExpr l) return l.getValue();
        return "";
    }

    static Map<String, String> props(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    static String map(Map<String, String> p) {
        return p.entrySet().stream().map(e -> e.getKey() + ":'" + q(e.getValue()) + "'").collect(Collectors.joining(", ", "{", "}"));
    }

    static String shorten(String s) {
        s = s.replaceAll("\\s+", " ");
        return s.length() > 50 ? s.substring(0, 47) + "..." : s;
    }

    static String q(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }
}
