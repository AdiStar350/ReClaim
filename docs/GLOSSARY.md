# ReClaim — Concepts Glossary

This file explains the general words and ideas used all over the project, in plain
language for someone who knows basic Java (classes, objects, methods, inheritance)
and basic data structures (lists, maps), but is new to web/Android frameworks.

The project has two parts:

- **Backend** (`reclaim-backend`): a **Spring Boot** program that runs on a server.
  It stores data and answers requests over the internet.
- **Android app** (`reclaim-project`): the phone app the user taps on. It talks to
  the backend over the internet.

Each per-file doc links back here instead of re-explaining these terms every time.

---

## Web / backend words

- **Client and server.** The *client* asks for something (the phone app). The
  *server* answers (the backend). They talk over the internet.
- **API (Application Programming Interface).** A set of "doors" the server opens so
  other programs can ask it to do things. Our API speaks over HTTP using JSON.
- **HTTP.** The language of the web. A client sends a *request*, the server sends a
  *response*.
- **HTTP method.** The verb of a request. `GET` = read, `POST` = create/send,
  `PUT`/`PATCH` = update, `DELETE` = remove.
- **Endpoint / route.** One specific API door, written as a method + path, e.g.
  `POST /api/auth/login`.
- **HTTP status code.** A number in the response saying how it went. `200` OK,
  `201` created, `400` bad request, `401` not logged in, `403` not allowed,
  `404` not found, `409` conflict, `429` too many tries, `500` server error,
  `502/503/504` server/gateway not reachable.
- **JSON.** A text format for data that looks like nested maps and lists, e.g.
  `{"email":"a@b.com"}`. Both sides send and read JSON.
- **REST.** A common style for APIs: use HTTP methods on URLs that name "resources"
  (users, items, claims) and pass JSON.

## Spring Boot building blocks

- **Spring Boot.** A Java framework that wires the backend together and runs a web
  server for us, so we write mostly our own logic.
- **Bean.** Any object that Spring creates and manages for us.
- **Dependency injection (DI).** Instead of building helper objects ourselves with
  `new`, we ask for them (usually through the constructor) and Spring hands them in.
- **Annotation.** A tag starting with `@` that gives Spring (or another library)
  instructions, e.g. `@RestController`, `@Service`, `@Autowired`.
- **Controller.** A class that receives HTTP requests and returns responses. It is
  the "front desk." Marked `@RestController` with `@PostMapping`/`@GetMapping`.
- **Service.** A class with the real logic (checking passwords, saving data). The
  controller calls the service. Marked `@Service`.
- **Repository.** An interface that reads and writes the database for one type of
  data. We just declare method names and Spring Data writes the query. Marked
  `@Repository` or extends `MongoRepository`.
- **Model / Entity.** A plain class that mirrors one kind of stored data (a `User`,
  an `Item`). One object = one record in the database.
- **DTO (Data Transfer Object).** A small class used only to carry data in or out of
  the API (for example `LoginRequest`, `LoginResponse`). It keeps the API shape
  separate from the database model, so we don't leak internal fields.
- **Validation.** Annotations like `@NotBlank`, `@Email` on a DTO that make Spring
  reject bad input before our code runs.

## Data, security, and services

- **MongoDB.** A **NoSQL** database that stores **documents** (JSON-like records) in
  **collections** (like tables). No fixed columns; each document is flexible.
- **Authentication vs authorization.** *Authentication* = proving who you are
  (login). *Authorization* = what you are allowed to do once known.
- **JWT (JSON Web Token).** A signed string the server gives you after login. The app
  sends it back on later requests (header `Authorization: Bearer <token>`) to prove
  it is logged in, so the server doesn't store a session.
- **Password hashing.** We never store the real password. We store a scrambled,
  one-way version (a *hash*). Login re-hashes the input and compares.
- **OTP (One-Time Password).** A short 6-digit code emailed to the user to prove they
  own the email, used for sign-up and password reset.
- **SMTP.** The protocol for sending email. We use Gmail's SMTP server.
- **Firebase.** Google cloud services we use for: **Storage** (holding uploaded
  photos) and **FCM** (Firebase Cloud Messaging, for push notifications).
- **Lombok.** A library that writes boilerplate for us at compile time. `@Getter`,
  `@Setter`, `@Data`, `@Builder`, `@NoArgsConstructor` add getters, setters,
  builders, and constructors so we don't type them.
- **Maven.** The build tool for the backend. `pom.xml` lists the libraries and
  Maven downloads them and compiles the code.

---

## Android words

- **Activity.** One full screen with logic (e.g. `LoginActivity`). It has a lifecycle
  (`onCreate`, `onStart`, `onStop`...).
- **Fragment.** A reusable piece of a screen that lives inside an Activity (e.g.
  `HomeFragment`), also with its own lifecycle.
- **Layout XML.** A file in `res/layout` that describes what a screen looks like
  (the views and how they're arranged). Java code "inflates" it to real objects.
- **View / ViewGroup.** A *View* is one UI widget (button, text). A *ViewGroup* holds
  other views (a container). Examples: `TextView`, `Button`, `LinearLayout`.
- **ConstraintLayout / LinearLayout / FrameLayout.** Common ViewGroups that position
  their children by rules, in a row/column, or stacked.
- **ViewBinding.** A generated class (e.g. `ActivityLoginBinding`) that gives typed
  access to the views in a layout, so we skip `findViewById`.
- **Context.** An Android object that represents "the app right now"; needed to reach
  resources, start screens, show toasts.
- **Intent.** A message used to start another Activity or pass data between screens.
- **RecyclerView.** A scrollable list that recycles row views for performance.
- **Adapter + ViewHolder.** The Adapter feeds data to a RecyclerView; a ViewHolder
  holds the views for one row so they can be reused.
- **ViewModel.** A class that holds screen data and survives rotation. Part of Android
  Jetpack.
- **LiveData.** A data holder you can *observe*; when it changes, the UI updates.
- **SharedPreferences.** A tiny key–value store on the phone for small data (like the
  saved JWT token).
- **WorkManager.** Runs background jobs reliably (we use it to sync offline reports).
- **Toast.** A small popup message on screen.

## Networking libraries (Android side)

- **Retrofit.** A library that turns a Java interface into API calls. We write method
  signatures with annotations (`@POST("api/auth/login")`) and Retrofit builds the
  HTTP request and parses the JSON response.
- **Call / Callback / enqueue.** A Retrofit `Call<T>` is one pending request.
  `enqueue` runs it in the background and calls `onResponse`/`onFailure`.
- **OkHttp.** The lower-level HTTP client Retrofit uses. An **Interceptor** is a hook
  that can inspect or change every request/response (we use one to add a header and
  detect when the backend is unreachable).
- **Gson.** A library that converts between Java objects and JSON. `@SerializedName`
  maps a Java field to a JSON key.
- **Glide.** A library that loads images from a URL into an `ImageView`.
- **Google Places / Maps.** Google services for address autocomplete and maps in the
  report screen.
