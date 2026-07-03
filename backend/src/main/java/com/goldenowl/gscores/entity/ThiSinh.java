package com.goldenowl.gscores.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "thi_sinh")
@Getter
@Setter
public class ThiSinh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String sbd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ngoai_ngu_id")
    private NgoaiNgu ngoaiNgu;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
