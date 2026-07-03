package com.goldenowl.gscores.service;

import com.goldenowl.gscores.dto.SubjectDto;
import com.goldenowl.gscores.repository.MonThiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectService {

    private final MonThiRepository monThiRepository;

    public List<SubjectDto> getAllSubjects() {
        return monThiRepository.findAll(Sort.by("id")).stream()
                .map(m -> new SubjectDto(m.getMaMon(), m.getTenMon()))
                .toList();
    }
}
