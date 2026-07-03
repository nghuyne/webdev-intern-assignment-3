package com.goldenowl.gscores.controller;

import com.goldenowl.gscores.dto.ScoreResponse;
import com.goldenowl.gscores.service.ScoreService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
@Validated
public class ScoreController {

    private final ScoreService scoreService;

    @GetMapping("/{sbd}")
    public ScoreResponse getScoreBySbd(
            @PathVariable @Pattern(regexp = "\\d{8}", message = "SBD phải gồm đúng 8 chữ số") String sbd) {
        return scoreService.getScoresBySbd(sbd);
    }
}
