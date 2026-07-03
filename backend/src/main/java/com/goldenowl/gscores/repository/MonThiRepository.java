package com.goldenowl.gscores.repository;

import com.goldenowl.gscores.entity.MonThi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonThiRepository extends JpaRepository<MonThi, Long> {
    Optional<MonThi> findByMaMon(String maMon);
}
