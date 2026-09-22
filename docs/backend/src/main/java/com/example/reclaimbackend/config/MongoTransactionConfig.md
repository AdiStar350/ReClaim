# MongoTransactionConfig.java

## Overview
This is a **Spring configuration** class that turns on **MongoDB transactions** so several database writes can succeed or fail together (see `docs/GLOSSARY.md`). A configuration class is a place Spring looks for beans to create at startup. Without a `MongoTransactionManager` bean, `@Transactional` on services would have nothing to drive multi-document work on MongoDB.

## Concepts used here
- **Bean** — an object Spring creates and manages; see `docs/GLOSSARY.md`.
- **Dependency injection** — Spring passes `MongoTemplate` into the bean method; see `docs/GLOSSARY.md`.
- **MongoDB** — the database this transaction manager talks to; see `docs/GLOSSARY.md`.
- **Transaction** — a group of writes that must all commit or all roll back together.
- **Annotation** — `@Configuration`, `@EnableTransactionManagement`, and `@Bean` tell Spring how to wire this; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring Context** (`@Configuration`, `@Bean`): marks this as a config class and registers `transactionManager` as a bean.
- **Spring Data MongoDB** (`MongoTransactionManager`, `MongoTemplate`): `MongoTransactionManager` runs transactions against MongoDB; `MongoTemplate` supplies the database factory the manager needs.
- **Spring Transactions** (`@EnableTransactionManagement`): turns on `@Transactional` support so services can opt into transactions.

## Fields
*(none — this class only defines a bean method.)*

## Functions

### `public MongoTransactionManager transactionManager(MongoTemplate mongoTemplate)`
```java
@Bean
public MongoTransactionManager transactionManager(MongoTemplate mongoTemplate) {
    return new MongoTransactionManager(mongoTemplate.getMongoDatabaseFactory());
}
```
- **What it does:** creates the Spring bean that manages MongoDB transactions, using the same database connection factory as `MongoTemplate`.
- **Where it's called:** by Spring at startup when it builds beans. Services then use it indirectly via `@Transactional` — currently `ClaimService.reviewClaim`.
- **Why it's written this way:** Spring Data MongoDB needs an explicit `MongoTransactionManager` bean; building it from `MongoTemplate`'s factory keeps transactions on the same MongoDB connection setup as the rest of the app.
- **Step by step:**
  1. Spring injects the existing `MongoTemplate` bean.
  2. Read that template's `MongoDatabaseFactory`.
  3. Wrap it in a new `MongoTransactionManager` and return it as a `@Bean`.
