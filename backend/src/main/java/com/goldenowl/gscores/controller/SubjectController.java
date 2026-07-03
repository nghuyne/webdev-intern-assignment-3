package com.goldenowl.gscores.controller;

import com.goldenowl.gscores.dto.SubjectDto;
import com.goldenowl.gscores.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public List<SubjectDto> getSubjects() {
        return subjectService.getAllSubjects();
    }
}
