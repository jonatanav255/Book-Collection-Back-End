package com.bookshelf.repository;

import com.bookshelf.model.Book;
import com.bookshelf.model.ReadingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
     * Get the most recently read book for "Continue Reading" feature
     */
    @Query("SELECT b FROM Book b WHERE b.lastReadAt IS NOT NULL ORDER BY b.lastReadAt DESC LIMIT 1")
    Optional<Book> findMostRecentlyRead();

    /**
     * Get recently read books for featured section on homepage
     */
    @Query("SELECT b FROM Book b WHERE b.lastReadAt IS NOT NULL ORDER BY b.lastReadAt DESC LIMIT :limit")
    List<Book> findRecentlyReadBooks(@Param("limit") int limit);

    /**
     * Count books by reading status for statistics
     */
    long countByStatus(ReadingStatus status);

    @Query("SELECT COALESCE(SUM(b.pageCount), 0) FROM Book b")
    long sumTotalPages();

    @Query("SELECT COALESCE(SUM(b.currentPage), 0) FROM Book b")
    long sumTotalPagesRead();

    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Book> searchBooks(@Param("search") String search, Pageable pageable);

    Page<Book> findByStatus(ReadingStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE Book b SET b.status = :status, b.updatedAt = CURRENT_TIMESTAMP WHERE b.id IN :ids")
    int updateStatusByIdIn(@Param("ids") List<UUID> ids, @Param("status") ReadingStatus status);

    @Modifying
    @Query("UPDATE Book b SET b.status = :status, b.currentPage = 0, b.updatedAt = CURRENT_TIMESTAMP WHERE b.id IN :ids")
    int updateStatusAndResetPageByIdIn(@Param("ids") List<UUID> ids, @Param("status") ReadingStatus status);

    @Modifying
    @Query("UPDATE Book b SET b.status = :status, b.currentPage = b.pageCount, b.updatedAt = CURRENT_TIMESTAMP WHERE b.id IN :ids AND b.pageCount > 0")
    int updateStatusAndFinishPageByIdIn(@Param("ids") List<UUID> ids, @Param("status") ReadingStatus status);

    @Query("SELECT b FROM Book b WHERE b.status = :status AND (" +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Book> searchBooksByStatus(@Param("search") String search, @Param("status") ReadingStatus status, Pageable pageable);
}
