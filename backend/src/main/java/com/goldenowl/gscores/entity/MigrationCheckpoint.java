package com.goldenowl.gscores.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "migration_checkpoint")
@Getter
@Setter
public class MigrationCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, unique = true, length = 255)
    private String fileName;

    @Column(name = "last_line_offset", nullable = false)
    private Long lastLineOffset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CheckpointStatus status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
