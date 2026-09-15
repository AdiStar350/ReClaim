```java
public static void main(String[] args) {
    SpringApplication.run(ReclaimBackendApplication.class, args);
}
```

What that function does: I start the Spring Boot app with this class as the entry point.
What the function is responsible for: I am the process main. I boot the backend.
Where it works: I am started by the JVM when the jar or IDE run configuration starts. I am unused except as the app entry.
Why I chose to write the code of the function the way its written: I used the usual one-line Spring start so config, beans, and Mongo all load from here.
More: I use SpringApplication.run and @SpringBootApplication on the class so component scan covers com.example.reclaimbackend.
