package com.goldenowl.gscores.service;

import com.goldenowl.gscores.dto.ScoreDto;
import com.goldenowl.gscores.dto.ScoreResponse;
import com.goldenowl.gscores.entity.NgoaiNgu;
import com.goldenowl.gscores.entity.ThiSinh;
import com.goldenowl.gscores.exception.ResourceNotFoundException;
import com.goldenowl.gscores.repository.KetQuaThiRepository;
import com.goldenowl.gscores.repository.ThiSinhRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoreService {

    private final ThiSinhRepository thiSinhRepository;
    private final KetQuaThiRepository ketQuaThiRepository;

    public ScoreResponse getScoresBySbd(String sbd) {
        ThiSinh thiSinh = thiSinhRepository.findBySbd(sbd)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh với SBD " + sbd));

        List<ScoreDto> scores = ketQuaThiRepository.findAllByThiSinhSbd(sbd).stream()
                .map(k -> new ScoreDto(k.getMonThi().getMaMon(), k.getMonThi().getTenMon(), k.getDiem()))
                .toList();

        NgoaiNgu ngoaiNgu = thiSinh.getNgoaiNgu();
        String maNgoaiNgu = ngoaiNgu != null ? ngoaiNgu.getMaNgoaiNgu() : null;
        String tenNgoaiNgu = ngoaiNgu != null ? ngoaiNgu.getTenNgoaiNgu() : null;

        return new ScoreResponse(sbd, maNgoaiNgu, tenNgoaiNgu, scores);
    }
}
