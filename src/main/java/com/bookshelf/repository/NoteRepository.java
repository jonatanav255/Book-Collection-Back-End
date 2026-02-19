package com.bookshelf.repository;

import com.bookshelf.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {

    List<Note> findByBookIdOrderByPageNumberAsc(UUID bookId);

    List<Note> findByBookIdOrderByCreatedAtDesc(UUID bookId);

    void deleteByBookId(UUID bookId);
}
