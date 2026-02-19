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

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

    Optional<Book> findByFileHash(String fileHash);

    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Book> searchBooks(@Param("search") String search);

    List<Book> findByStatus(ReadingStatus status);

    @Query("SELECT b FROM Book b ORDER BY " +
           "CASE WHEN :sortBy = 'title' THEN b.title END ASC, " +
           "CASE WHEN :sortBy = 'dateAdded' THEN b.dateAdded END DESC, " +
           "CASE WHEN :sortBy = 'lastRead' THEN b.lastReadAt END DESC, " +
           "CASE WHEN :sortBy = 'progress' THEN (CAST(b.currentPage AS double) / CAST(b.pageCount AS double)) END DESC")
    List<Book> findAllSorted(@Param("sortBy") String sortBy);

    @Query("SELECT b FROM Book b WHERE b.status = :status ORDER BY " +
           "CASE WHEN :sortBy = 'title' THEN b.title END ASC, " +
           "CASE WHEN :sortBy = 'dateAdded' THEN b.dateAdded END DESC, " +
           "CASE WHEN :sortBy = 'lastRead' THEN b.lastReadAt END DESC, " +
           "CASE WHEN :sortBy = 'progress' THEN (CAST(b.currentPage AS double) / CAST(b.pageCount AS double)) END DESC")
    List<Book> findByStatusSorted(@Param("status") ReadingStatus status, @Param("sortBy") String sortBy);

    @Query("SELECT b FROM Book b WHERE b.lastReadAt IS NOT NULL ORDER BY b.lastReadAt DESC LIMIT 1")
    Optional<Book> findMostRecentlyRead();

    long countByStatus(ReadingStatus status);
}
