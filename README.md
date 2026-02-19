# BookShelf Backend API

A Spring Boot REST API for managing a personal digital bookshelf with PDF books, notes, reading progress, and customizable preferences.

## Features

- **PDF Upload & Management**: Upload PDF books, extract metadata, generate thumbnails
- **Google Books Integration**: Auto-fetch book metadata (cover, author, description, genre)
- **Reading Progress Tracking**: Auto-bookmark, reading status (unread/reading/finished)
- **Notes System**: Page-specific notes with colors, pinning, and markdown export
- **User Preferences**: Theme, font family, and font size customization
- **Library Statistics**: Track reading progress and completion stats

## Tech Stack

- **Java 17+**
- **Spring Boot 3.2.1**
- **PostgreSQL** (database)
- **Apache PDFBox** (PDF processing & thumbnail generation)
- **Flyway** (database migrations)
- **Google Books API** (metadata enrichment)

## Prerequisites

- Java 17 or higher
- PostgreSQL 12 or higher
- Maven 3.6 or higher

## Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd Book-Collection-Back-End
```

### 2. Set up PostgreSQL database

```bash
# Create database
createdb bookshelf

# Or using psql
psql -U postgres
CREATE DATABASE bookshelf;
```

### 3. Configure environment variables

Copy `.env.example` to `.env` and update the values:

```bash
cp .env.example .env
```

Edit `.env` with your configuration:

```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=bookshelf
DB_USERNAME=postgres
DB_PASSWORD=your_password

# Optional: Add Google Books API key for enhanced metadata
GOOGLE_BOOKS_API_KEY=your_api_key_here
```

### 4. Create storage directories

```bash
mkdir -p data/bookshelf/pdfs
mkdir -p data/bookshelf/thumbnails
```

### 5. Build and run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Or with environment variables
export $(cat .env | xargs) && mvn spring-boot:run
```

The API will start on `http://localhost:8080`

## API Endpoints

### Books

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/books` | Upload a PDF book |
| `GET` | `/api/books` | List all books (with optional filters) |
| `GET` | `/api/books/stats` | Get library statistics |
| `GET` | `/api/books/{id}` | Get book details |
| `PUT` | `/api/books/{id}` | Update book metadata |
| `DELETE` | `/api/books/{id}` | Delete a book |
| `GET` | `/api/books/{id}/pdf` | Stream PDF file |
| `GET` | `/api/books/{id}/thumbnail` | Get thumbnail image |
| `GET` | `/api/books/{id}/progress` | Get reading progress |
| `PUT` | `/api/books/{id}/progress` | Update reading progress |

### Notes

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/books/{bookId}/notes` | List all notes for a book |
| `POST` | `/api/books/{bookId}/notes` | Create a note |
| `PUT` | `/api/notes/{noteId}` | Update a note |
| `DELETE` | `/api/notes/{noteId}` | Delete a note |
| `GET` | `/api/books/{bookId}/notes/export` | Export notes as markdown |

### Preferences

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/preferences` | Get user preferences |
| `PUT` | `/api/preferences` | Update preferences |

### Google Books

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/books/lookup?query={query}` | Search Google Books API |

## Query Parameters

### GET /api/books

- `search` - Search by title or author
- `sortBy` - Sort by: `title`, `dateAdded`, `lastRead`, `progress`
- `status` - Filter by: `unread`, `reading`, `finished`

Example: `/api/books?search=Java&sortBy=dateAdded&status=reading`

### GET /api/books/{bookId}/notes

- `sortBy` - Sort by: `page` (default), `date`

## Example Usage

### Upload a Book

```bash
curl -X POST http://localhost:8080/api/books \
  -F "file=@/path/to/book.pdf"
```

### Create a Note

```bash
curl -X POST http://localhost:8080/api/books/{bookId}/notes \
  -H "Content-Type: application/json" \
  -d '{
    "pageNumber": 42,
    "content": "Important insight about chapter 3",
    "color": "yellow",
    "pinned": true
  }'
```

### Update Reading Progress

```bash
curl -X PUT http://localhost:8080/api/books/{bookId}/progress \
  -H "Content-Type: application/json" \
  -d '{
    "currentPage": 150,
    "status": "READING"
  }'
```

## Database Schema

The application uses Flyway for database migrations. The initial schema includes:

- **books** - Book metadata and reading progress
- **notes** - Page-specific notes
- **preferences** - User preferences

Migrations are located in `src/main/resources/db/migration/`

## Configuration

Application properties can be configured in:
- `src/main/resources/application.yml` (default)
- `src/main/resources/application-dev.yml` (development profile)
- Environment variables (recommended for sensitive data)

### Key Configuration Properties

```yaml
bookshelf:
  storage:
    pdf-directory: ./data/bookshelf/pdfs
    thumbnail-directory: ./data/bookshelf/thumbnails

google:
  books:
    api:
      key: ${GOOGLE_BOOKS_API_KEY:}

spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

## Security Notes

- Never commit the `.env` file or API keys to version control
- The Google Books API key should be stored server-side only
- File upload size is limited to 100MB by default
- Only `.pdf` files are accepted for upload
- File hashes are used for duplicate detection

## Error Handling

The API returns structured error responses:

```json
{
  "timestamp": "2024-01-20T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Book not found with id: 123e4567-e89b-12d3-a456-426614174000"
}
```

## Development

### Running with development profile

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Running tests

```bash
mvn test
```

## License

This project is part of the BookShelf application suite.
