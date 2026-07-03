package com.goldenowl.gscores.repository;

import com.goldenowl.gscores.entity.NgoaiNgu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NgoaiNguRepository extends JpaRepository<NgoaiNgu, Long> {
    Optional<NgoaiNgu> findByMaNgoaiNgu(String maNgoaiNgu);
}
