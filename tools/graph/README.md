# System map

Static extractor for the map described in `system-knowledge-graph-brief.md`.  It reads `src/main/java` with
JavaParser and produces:

- a folder of linked Markdown notes (an Obsidian vault): one note per endpoint, method, class and store
- a Cypher file, if you want to load the same data into Neo4j

```bash
java -cp ~/.m2/repository/com/github/javaparser/javaparser-core/3.28.0/javaparser-core-3.28.0.jar \
  tools/graph/Extract.java src/main/java tools/graph/out/graph.cypher docs/vault
```

The third argument (the vault folder) is optional.  Open `docs/vault` in Obsidian with *Manage vaults* then
*Open folder as vault*.

Each generated note sits between `<!-- generated:start -->` and `<!-- generated:end -->`.  Anything written
below the end marker survives a re-run.

## What is mapped

`Endpoint -> controller method -> service method -> repository method -> store (Redis key or upload directory)`

- Only public methods are notes.  Private helpers are folded into the public method that calls them.
- Each call shows what it passes and returns.  Each store access shows the Redis command or file operation.
- Services and repositories have a class note linking to their operations.  Controllers do not.

## Neo4j (optional)

```bash
docker run -d --name mpp-neo4j -p 127.0.0.1:7474:7474 -p 127.0.0.1:7687:7687 -e NEO4J_AUTH=none neo4j:5
docker exec mpp-neo4j cypher-shell "MATCH (n) DETACH DELETE n"
docker exec -i mpp-neo4j cypher-shell < tools/graph/out/graph.cypher
```

Browse at http://localhost:7474.  The Neo4j graph is `(Endpoint)-[HANDLED_BY]->(Method)-[CALLS]->(Method)-[READS|WRITES]->(Store)`.
