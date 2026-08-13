### OrderService Task
The primary objective of this assignment is to build a robust backend Java web application for managing orders, items, and inventory. The service is built using Java 21, Spring Boot 4.0, Spring Data JPA, and Maven. It utilizes Liquibase for database migrations, integrates with Redis for caching, and is fully containerized using Docker. The service communicates synchronously with UserService using Feign/WebClient, protected by Resilience4j Circuit Breaker, and secured via JWT validation.

### Technical Stack
* **Java 21** & **Maven**

* **Spring Boot** (Data JPA, Web, Validation, Security)

* **Liquibase** (Database migrations)

* **PostgreSQL** (Primary database)

* **Redis** (Caching layer)

* **MapStruct** (DTO/Entity mapping)

* **Docker & Docker Compose** (Containerization)

* **GitHub Actions** (CI/CD Pipeline)

* **WireMock + Testcontainers** (Integration Testing)

### Getting Started
You can run the application on your local machine or via Docker.

### Step 1: Prerequisites
Ensure you have the following installed:

* JDK 21

* Maven 4.1+

* Docker & Docker Compose

### Step 2: Build the Project
Open a terminal, navigate to the project root folder (where pom.xml is located), and build the project:

```bash
mvn clean package -Dmaven.skip.test=true
```
This produces the executable JAR artifact in the target/ directory.

### Step 3: Run the Application

### Running via Docker Compose
This will automatically spin up PostgreSQL, Redis, and the Spring Boot application using the docker Spring profile:

```bash
docker-compose up --build
```

### Database Architecture & Schema
Database schema creation is fully automated using **Liquibase** changelogs.

* Performance is optimized using **database indexes** on frequently queried columns.

* **JPA Auditing** is enabled globally to automatically populate `createdAt` and `updatedAt` timestamps for all entities.

Soft Delete is implemented for orders using Hibernate annotations.

### Tables & Relations
1. **orders**

* Columns: `id`, `user_id`, `status`, `total_price`, `deleted`, `created_at`, `updated_at`

2. **order_items**

* Columns: `id`, `order_id`, `item_id`, `quantity`, `created_at`, `updated_at`

3. **items**

* Columns: `id`, `name`, `price`, `created_at`, `updated_at`

### Deployment & CI/CD Pipeline
Profiles
local: Optimized for running the application on a local development machine.

docker: Configured for containerized environments within docker-compose.

### CI/CD Pipeline (GitHub Actions)
On every git push to the main branches, the automated workflow executes the following steps:

Build: Compiles code and verifies dependencies.

Testing: Runs unit and integration tests (JUnit, Mockito, WireMock, Testcontainers).

Code Analysis: Executes static code analysis via SonarQube/SonarCloud to ensure code quality.

Artifact Creation: Builds the final Docker image and pushes it to the registry.