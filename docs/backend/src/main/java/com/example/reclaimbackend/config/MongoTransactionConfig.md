```java
@Bean
public MongoTransactionManager transactionManager(MongoTemplate mongoTemplate) {
    return new MongoTransactionManager(mongoTemplate.getMongoDatabaseFactory());
}
```

What that function does: I give Spring a Mongo transaction manager so methods marked transactional can save together.
What the function is responsible for: I am the transactionManager bean used by @Transactional work.
Where it works: I am injected by Spring. ClaimService.reviewClaim is @Transactional so it can use me. I am unused except as a bean.
Why I chose to write the code of the function the way its written: I built the manager from MongoTemplate's factory so it uses the same database connection as the rest of the app.
More: I use Spring Data MongoTransactionManager and MongoTemplate. @EnableTransactionManagement on the class turns transactions on.
