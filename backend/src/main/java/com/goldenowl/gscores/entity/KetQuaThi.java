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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ket_qua_thi",
        uniqueConstraints = @UniqueConstraint(columnNames = {"thi_sinh_id", "mon_thi_id"}))
@Getter
@Setter
public class KetQuaThi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thi_sinh_id", nullable = false)
    private ThiSinh thiSinh;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mon_thi_id", nullable = false)
    private MonThi monThi;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal diem;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
