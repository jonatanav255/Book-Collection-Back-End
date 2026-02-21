package com.bookshelf.repository;

import com.bookshelf.model.Book;
import com.bookshelf.model.ReadingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Book entity database operations
 * Includes custom queries for search, filtering, sorting, and statistics
 */
@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

    /**
     * Find book by SHA-256 file hash for duplicate detection
     */
    Optional<Book> findByFileHash(String fileHash);

    /**
     * Search books by title or author (case-insensitive)
     */
    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Book> searchBooks(@Param("search") String search);

    /**
     * Find books by reading status
     */
    List<Book> findByStatus(ReadingStatus status);

    /**
     * Find all books with dynamic sorting
     */
    @Query("SELECT b FROM Book b ORDER BY " +
           "CASE WHEN :sortBy = 'title' THEN b.title END ASC, " +
           "CASE WHEN :sortBy = 'dateAdded' THEN b.dateAdded END DESC, " +
           "CASE WHEN :sortBy = 'lastRead' THEN b.lastReadAt END DESC, " +
           "CASE WHEN :sortBy = 'progress' THEN (CAST(b.currentPage AS double) / CAST(b.pageCount AS double)) END DESC")
    List<Book> findAllSorted(@Param("sortBy") String sortBy);

    /**
     * Find books by status with dynamic sorting
     */
    @Query("SELECT b FROM Book b WHERE b.status = :status ORDER BY " +
           "CASE WHEN :sortBy = 'title' THEN b.title END ASC, " +
           "CASE WHEN :sortBy = 'dateAdded' THEN b.dateAdded END DESC, " +
           "CASE WHEN :sortBy = 'lastRead' THEN b.lastReadAt END DESC, " +
           "CASE WHEN :sortBy = 'progress' THEN (CAST(b.currentPage AS double) / CAST(b.pageCount AS double)) END DESC")
    List<Book> findByStatusSorted(@Param("status") ReadingStatus status, @Param("sortBy") String sortBy);

    /**
     * Get the most recently read book for "Continue Reading" feature
     */
    @Query(value = "SELECT * FROM books WHERE last_read_at IS NOT NULL ORDER BY last_read_at DESC LIMIT 1", nativeQuery = true)
    Optional<Book> findMostRecentlyRead();

    /**
     * Get recently read books for featured section on homepage
     */
    @Query(value = "SELECT * FROM books WHERE last_read_at IS NOT NULL ORDER BY last_read_at DESC LIMIT :limit", nativeQuery = true)
    List<Book> findRecentlyReadBooks(@Param("limit") int limit);

    /**
     * Count books by reading status for statistics
     */
    long countByStatus(ReadingStatus status);
}
