package com.goldenowl.gscores.repository;

import com.goldenowl.gscores.entity.ThiSinh;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ThiSinhRepository extends JpaRepository<ThiSinh, Long> {
    Optional<ThiSinh> findBySbd(String sbd);
}
