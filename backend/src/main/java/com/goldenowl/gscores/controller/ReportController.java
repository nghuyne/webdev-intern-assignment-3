package com.goldenowl.gscores.controller;

import com.goldenowl.gscores.dto.BandCountDto;
import com.goldenowl.gscores.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/band-counts")
    public List<BandCountDto> getBandCounts() {
        return reportService.getBandCounts();
    }
}
