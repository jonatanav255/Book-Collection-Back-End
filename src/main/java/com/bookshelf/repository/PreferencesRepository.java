package com.bookshelf.repository;

import com.bookshelf.model.Preferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreferencesRepository extends JpaRepository<Preferences, UUID> {

    Optional<Preferences> findFirstByOrderByCreatedAtAsc();
}
