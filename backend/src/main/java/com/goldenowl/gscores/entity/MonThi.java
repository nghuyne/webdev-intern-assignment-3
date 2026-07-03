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
@Table(name = "mon_thi")
@Getter
@Setter
public class MonThi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_mon", nullable = false, unique = true, length = 30)
    private String maMon;

    @Column(name = "ten_mon", nullable = false, length = 100)
    private String tenMon;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
