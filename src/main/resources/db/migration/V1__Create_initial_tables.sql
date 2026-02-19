-- Create books table
CREATE TABLE books (
    id UUID PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    author VARCHAR(500),
    description TEXT,
    genre VARCHAR(255),
    page_count INTEGER,
    current_page INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    pdf_path VARCHAR(1000) NOT NULL,
    thumbnail_path VARCHAR(1000),
    cover_url VARCHAR(1000),
    file_hash VARCHAR(64) UNIQUE,
    date_added TIMESTAMP,
    last_read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('UNREAD', 'READING', 'FINISHED'))
);

-- Create notes table
CREATE TABLE notes (
    id UUID PRIMARY KEY,
    book_id UUID NOT NULL,
    page_number INTEGER NOT NULL,
    content TEXT NOT NULL,
    color VARCHAR(20),
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_notes_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

-- Create preferences table
CREATE TABLE preferences (
    id UUID PRIMARY KEY,
    theme VARCHAR(50) NOT NULL DEFAULT 'light',
    font_family VARCHAR(100) DEFAULT 'serif',
    font_size VARCHAR(10) DEFAULT 'md',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_books_status ON books(status);
CREATE INDEX idx_books_date_added ON books(date_added DESC);
CREATE INDEX idx_books_last_read_at ON books(last_read_at DESC);
CREATE INDEX idx_books_file_hash ON books(file_hash);
CREATE INDEX idx_notes_book_id ON notes(book_id);
CREATE INDEX idx_notes_page_number ON notes(book_id, page_number);
CREATE INDEX idx_notes_created_at ON notes(created_at DESC);
