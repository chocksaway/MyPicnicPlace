# Brief: build a queryable knowledge graph of this Spring Boot system

## Goal

Build a knowledge graph of this application that I can stand on at any point
and traverse: "what touches this table?", "what happens when this endpoint is
called?", "what does this Kafka topic eventually cause?". The point is to
understand the system, not to make a pretty diagram. Every relationship must have
a label, so each link reads as a sentence (subject, verb, object).

## Core idea

Generalised from a CRUD matrix (endpoints × tables, with C/R/U/D in the cells),
turned into a graph:

- **Entities (nodes):** endpoints, controllers, services, repositories, JPA entities,
  tables, Kafka topics / queues, external HTTP clients, scheduled jobs, caches
- **Labelled relationships (edges):** `CALLS`, `READS`, `WRITES`, `PUBLISHES`,
  `CONSUMES`, `MAPS_TO`, `CACHES`, `SCHEDULES`
- **Time / evidence:** where each fact came from (static analysis vs runtime trace),
  plus counts from runtime where available
- **The why:** free-text notes / links to design decisions as node properties,
  added by hand. That is where the understanding lives.

## Proposed schema (keep it consistent from day one)

```
(:Endpoint {method, path})
(:Component {name, kind})          // kind: controller | service | repository | listener | job | client
(:Entity {name})                   // JPA @Entity class
(:Table {name})
(:Topic {name})
(:External {name, baseUrl})

(:Endpoint)-[:HANDLED_BY]->(:Component)
(:Component)-[:CALLS]->(:Component)
(:Component)-[:READS|WRITES]->(:Table)
(:Entity)-[:MAPS_TO]->(:Table)
(:Component)-[:PUBLISHES]->(:Topic)
(:Topic)-[:CONSUMED_BY]->(:Component)
(:Component)-[:CALLS_EXTERNAL]->(:External)

Edge properties: source ('static' | 'runtime'), count (runtime), note
```

## How to populate it

### Phase 1: static analysis (start here)
Write a small extractor (JavaParser, Spoon, or ArchUnit's class importer) that finds:
- `@RestController` / `@RequestMapping` / `@GetMapping` etc. → Endpoint + HANDLED_BY
- constructor-injected dependencies between `@Service`, `@Component`, `@Repository` → CALLS
- `@Entity` / `@Table` → Entity MAPS_TO Table
- Spring Data repository methods (`find*`, `save`, `delete*`, `@Query`, `@Modifying`) → READS / WRITES
- `KafkaTemplate.send(...)` / `@KafkaListener(topics=...)` (or the equivalent for our broker) → PUBLISHES / CONSUMED_BY
- `RestTemplate` / `WebClient` / Feign clients → CALLS_EXTERNAL
- `@Scheduled` → job components

Output to Cypher (or CSV) and load into Neo4j (local Docker is fine).

### Phase 2: runtime truth (optional, later)
- Run the app with the OpenTelemetry Java agent; JDBC spans carry the SQL.
- Make sure trace context propagates through Kafka headers so async flows join up.
- Aggregate traces into endpoint → table edges with read/write counts and
  `source: 'runtime'`. Diff against static edges: mismatches are interesting.

### Phase 3: ask questions in plain English
- An LLM turns a question into Cypher, runs it, and explains the subgraph (GraphRAG).

## Questions the graph must answer (acceptance tests)

```cypher
// Which endpoints eventually write to table X?
MATCH (e:Endpoint)-[:HANDLED_BY]->(:Component)-[:CALLS*0..5]->(:Component)-[:WRITES]->(t:Table {name:'orders'})
RETURN DISTINCT e.method, e.path;

// Everything an endpoint touches, including through events
MATCH p=(e:Endpoint {path:'/claims', method:'POST'})-[:HANDLED_BY|CALLS|PUBLISHES|CONSUMED_BY|READS|WRITES*1..8]->(x)
RETURN p;

// Hidden coupling: tables written by more than one component
MATCH (c:Component)-[:WRITES]->(t:Table)
WITH t, collect(DISTINCT c.name) AS writers WHERE size(writers) > 1
RETURN t.name, writers;

// Gaps: entities/tables nothing reads, endpoints nothing tests, etc.
MATCH (t:Table) WHERE NOT ()-[:READS]->(t) RETURN t.name;
```

## Known limits to keep in mind
- Static analysis misses dynamic dispatch, reflection and config-driven wiring.
- Runtime traces only show paths that were exercised (rare branches, batch jobs missing).
- Caches, file storage and external APIs are easy to forget, so model them deliberately.
- The graph shows *what*, never *why*. Leave room for hand-written notes.

## What I'd like from you first
1. Look around this codebase and tell me which of the above node/edge types actually
   exist here (controllers, Kafka or other broker, JPA vs JDBC, external clients).
2. Propose any schema changes that fit this app better.
3. Then build the Phase 1 extractor and load the result into Neo4j, starting with
   endpoints → components → tables only. Add events and externals once that works.
