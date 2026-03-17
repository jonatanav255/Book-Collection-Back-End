# BookShelf Backend API

A Spring Boot REST API for managing a personal digital bookshelf.

## Prerequisites

- Java 17+
- PostgreSQL 12+
- Maven 3.6+
- Google Cloud credentials (`google-cloud-key.json` in project root)

## Setup

```bash
# Create the database
createdb bookshelf

# Start the server (sets up Google Cloud credentials automatically)
./start-server.sh
```

The API will start on `http://localhost:8080`

## Running Tests

```bash
mvn test
```
