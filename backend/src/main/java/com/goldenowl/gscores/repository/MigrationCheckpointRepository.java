package com.goldenowl.gscores.repository;

import com.goldenowl.gscores.entity.MigrationCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MigrationCheckpointRepository extends JpaRepository<MigrationCheckpoint, Long> {
    Optional<MigrationCheckpoint> findByFileName(String fileName);
}
