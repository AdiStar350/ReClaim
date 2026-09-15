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

What that function does: I add indexes on items at startup so search and filters stay faster.
What the function is responsible for: I create compound indexes on category/status/type/date, lat/lng, and targeted-found fields.
Where it works: I run after Spring builds the bean because of @PostConstruct. I am unused except at startup.
Why I chose to write the code of the function the way its written: I catch errors and log a warning so a failed index does not stop the app from starting.
More: I use MongoTemplate.indexOps, Spring Data Index, Sort.Direction, and Lombok @Slf4j for log.warn.
