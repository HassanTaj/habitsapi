# Habits API

A small Spring Boot REST API for saving and managing habits. It uses MySQL for habit data and JWT bearer tokens to protect the API routes.

## Start-up requirements

- Java 25 (the Gradle build uses this toolchain).
- A MySQL server that is already running.
- Environment variables for the API login and MySQL connection.

Set these values in the shell or IDE run configuration. Do not put real passwords in this file or commit them to Git.

| Environment variable | Used for |
| --- | --- |
| `HABITS_API_USERNAME` | Username accepted by `/api/auth/login` |
| `HABITS_API_PASSWORD` | Password accepted by `/api/auth/login` |
| `MYSQL_URL` | Full MySQL JDBC URL, including the database name |
| `MYSQL_USERNAME` | MySQL account name |
| `MYSQL_PASSWORD` | MySQL account password |

Examples of `MYSQL_URL`:

- If the API runs on your computer and Docker publishes MySQL port `3306`: `jdbc:mysql://localhost:3306/habitsapi`
- If the API also runs in Docker on the same network: `jdbc:mysql://<mysql-service-name>:3306/habitsapi`

Use the MySQL container's service name as the host when connecting from another container. `mysql-network` is the Docker network name, which is not necessarily the MySQL host name.

The MySQL database and `habits` table must already exist. This project does not currently include database migrations or a schema creation setting. The MySQL JDBC driver is included in Gradle, and Spring Boot reads the URL, username, and password from `spring.datasource.*` in `src/main/resources/application.properties`.

If the `habits` table is missing, connect to the existing database named at the end of `MYSQL_URL` and run this once:

```sql
CREATE TABLE IF NOT EXISTS habits (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    frequency VARCHAR(255),
    is_done BOOLEAN
);
```

Start the API from PowerShell with:

```powershell
.\gradlew.bat bootRun
```

The app listens on `http://localhost:8080` by default.

## API routes

| Method | Path | What it does | Login required |
| --- | --- | --- | --- |
| `POST` | `/api/auth/login` | Checks the configured API username and password, then returns a JWT | No |
| `POST` | `/api/auth/logout` | Blocks the JWT used for this request | Yes |
| `GET` | `/api/habits` | Lists habits | Yes |
| `GET` | `/api/habits/{id}` | Gets one habit | Yes |
| `POST` | `/api/habits` | Saves a new habit | Yes |
| `PUT` | `/api/habits/{id}` | Replaces a habit's editable fields | Yes |
| `DELETE` | `/api/habits/{id}` | Deletes a habit | Yes |

Send this JSON body to log in:

```json
{
  "username": "your-api-username",
  "password": "your-api-password"
}
```

The response contains `accessToken`, `tokenType` (`Bearer`), and `expiresIn` (seconds). Send the token on protected requests like this:

```http
Authorization: Bearer <accessToken>
```

In Swagger UI, use the **Authorize** button and enter the token. The login route and Swagger pages are public; the habit routes and logout require a valid bearer token.

## How the pieces fit together

```text
Client
  -> Spring Security checks the request
     -> AuthController handles login and logout
     -> HabitsController handles habit requests
        -> IHabitsRepository reads or writes Habit rows in MySQL
```

- `controllers/` turns HTTP requests into application actions and responses.
- `config/SecurityConfig` sets public and protected URLs, checks passwords, and creates and checks JWTs.
- `config/OpenApiConfig` gives Swagger the API title and JWT button.
- `security/TokenRevocationStore` remembers which token IDs have logged out.
- `data/entities/Habit` describes the columns in the `habits` table; `FrequencyEnum` lists valid schedule values.
- `data/repositories/IHabitsRepository` uses Spring Data JPA to do common database work without handwritten SQL for each operation.
- `application.properties` connects Spring Boot settings to environment variables.

## What the project uses and why

| Technology | Why it is here |
| --- | --- |
| Spring Boot | Starts the application and connects its web, security, and database parts. |
| Spring MVC | Provides REST routes such as `GET /api/habits`. |
| Spring Data JPA | Reads and writes Java entities in MySQL. |
| MySQL Connector/J | Lets Java connect to the MySQL server. |
| Spring Security | Checks login credentials and requires JWTs for protected routes. |
| JWT / HS256 | A signed token proves that the client logged in; HS256 uses a shared secret key. |
| Bean Validation | Rejects blank habit titles and blank login fields. |
| Springdoc OpenAPI | Builds the API description and Swagger UI from the controllers. |

## Development security notes

- The API login uses one in-memory account from `HABITS_API_USERNAME` and `HABITS_API_PASSWORD`. Spring stores a BCrypt hash of the password while the app is running. These are API login details, not MySQL credentials.
- A new random JWT signing key is created every time the app starts. This is convenient for development, but all existing tokens stop working after a restart. A deployed app needs a stable key stored in a secret manager.
- Logout puts that token's unique ID in an in-memory list until it expires. This works immediately in one running app process. The list disappears on restart, and separate app instances do not share it; multiple instances need shared signing and revocation storage.
- `compose.yaml` intentionally defines no services. It will not create a MySQL or PostgreSQL container; start and manage the MySQL server separately.
