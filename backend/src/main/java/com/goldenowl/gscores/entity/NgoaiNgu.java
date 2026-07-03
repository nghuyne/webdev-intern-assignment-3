package com.goldenowl.gscores.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "ngoai_ngu")
@Getter
@Setter
public class NgoaiNgu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_ngoai_ngu", nullable = false, unique = true, length = 10)
    private String maNgoaiNgu;

    @Column(name = "ten_ngoai_ngu", length = 100)
    private String tenNgoaiNgu;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
