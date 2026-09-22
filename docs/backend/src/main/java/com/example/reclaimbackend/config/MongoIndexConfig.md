# MongoIndexConfig.java

## Overview
This is a **Spring configuration** class that creates **indexes** on the `Item` collection when the backend starts (see `docs/GLOSSARY.md`). Indexes are like a table of contents for MongoDB: they make common filters and sorts faster. The class runs once after construction (`@PostConstruct`) and quietly logs a warning if index creation fails, so a bad index setup does not crash startup.

## Concepts used here
- **Bean / configuration** — Spring creates this class and injects `MongoTemplate`; see `docs/GLOSSARY.md`.
- **MongoDB** — indexes are created on the items collection; see `docs/GLOSSARY.md`.
- **Model / entity** — indexes target fields on `Item`; see `docs/GLOSSARY.md`.
- **Index** — a MongoDB structure that speeds up queries that filter or sort on certain fields.
- **PostConstruct** — a method Spring calls once after the bean is built and dependencies are set.

## Libraries and imports
- **Spring** (`@Configuration`, `MongoTemplate`, `Sort`): marks the config class; `MongoTemplate` talks to MongoDB; `Sort.Direction` sets ascending/descending index keys.
- **Spring Data MongoDB** (`Index`): describes which fields to index together.
- **Jakarta** (`@PostConstruct`): runs `initIndexes` after the bean is ready.
- **Lombok** (`@RequiredArgsConstructor`, `@Slf4j`): constructor injection for `mongoTemplate`, and a `log` field for warnings.
- **Project model** (`Item`): the entity whose collection gets the indexes.

## Fields
- `mongoTemplate` (`MongoTemplate`) — Spring Data helper used to create indexes on the `Item` collection.

## Functions

### `public void initIndexes()`
```java
@PostConstruct
public void initIndexes() {
    try {
        mongoTemplate.indexOps(Item.class)
                .ensureIndex(new Index()
                        .on("category", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC)
                        .on("type", Sort.Direction.ASC)
                        .on("reportedAt", Sort.Direction.DESC));

        mongoTemplate.indexOps(Item.class)
                .ensureIndex(new Index()
                        .on("latitude", Sort.Direction.ASC)
                        .on("longitude", Sort.Direction.ASC));

        mongoTemplate.indexOps(Item.class)
                .ensureIndex(new Index()
                        .on("targetLostReportId", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC));
    } catch (Exception ex) {
        log.warn("Could not create MongoDB indexes on startup: {}", ex.getMessage());
    }
}
```
- **What it does:** ensures three compound indexes exist on items: (1) category/status/type/reportedAt for filtered lists, (2) latitude/longitude for map-style lookups, (3) targetLostReportId/status for matching found items to a lost report.
- **Where it's called:** by Spring once after this bean is constructed (`@PostConstruct`). Not called from application code.
- **Why it's written this way:** `ensureIndex` is idempotent (safe to run every startup). The try/catch keeps a transient MongoDB problem from blocking the whole server; search still works, just potentially slower.
- **Step by step:**
  1. Open index operations for the `Item` collection.
  2. Ensure the filter/sort compound index (category, status, type, reportedAt descending).
  3. Ensure the geo-ish coordinate index (latitude, longitude).
  4. Ensure the lost-report match index (targetLostReportId, status).
  5. On any exception, log a warning and continue.
