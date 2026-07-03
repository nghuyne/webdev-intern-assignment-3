package com.goldenowl.gscores.controller;

import com.goldenowl.gscores.dto.LeaderboardEntryDto;
import com.goldenowl.gscores.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/group-a")
    public List<LeaderboardEntryDto> getGroupATop10() {
        return leaderboardService.getGroupATop10();
    }
}
