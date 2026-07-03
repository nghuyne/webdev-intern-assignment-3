package com.goldenowl.gscores.dto;

import java.util.List;

public record ScoreResponse(String sbd, String maNgoaiNgu, String tenNgoaiNgu, List<ScoreDto> scores) {
}
