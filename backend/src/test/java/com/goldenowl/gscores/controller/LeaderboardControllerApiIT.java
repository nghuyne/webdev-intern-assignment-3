package com.goldenowl.gscores.controller;

import com.goldenowl.gscores.entity.KetQuaThi;
import com.goldenowl.gscores.entity.MonThi;
import com.goldenowl.gscores.entity.ThiSinh;
import com.goldenowl.gscores.support.AbstractApiIT;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeaderboardControllerApiIT extends AbstractApiIT {

    @Test
    void getGroupATop10_noCompletedCheckpoint_returnsCandidatesRankedByTotalScoreDesc() throws Exception {
        seedGroupACandidate("10000001", "9.00", "9.00", "9.00"); // 27.00
        seedGroupACandidate("10000002", "8.00", "8.00", "8.00"); // 24.00

        mockMvc.perform(get("/api/leaderboard/group-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].hang").value(1))
                .andExpect(jsonPath("$[0].sbd").value("10000001"))
                .andExpect(jsonPath("$[0].tongDiem").value(27.00))
                .andExpect(jsonPath("$[1].hang").value(2))
                .andExpect(jsonPath("$[1].sbd").value("10000002"));
    }

    @Test
    void getGroupATop10_candidateMissingASubject_isExcludedFromRanking() throws Exception {
        seedGroupACandidate("20000001", "9.00", "9.00", "9.00");

        // Missing "hoa_hoc" (chemistry): the INNER JOIN based query must not return this
        // candidate even though they have 2 of the 3 Group A subjects.
        MonThi toan = monThiRepository.findByMaMon("toan").orElseThrow();
        MonThi vatLi = monThiRepository.findByMaMon("vat_li").orElseThrow();
        ThiSinh partial = new ThiSinh();
        partial.setSbd("20000002");
        partial = thiSinhRepository.save(partial);
        saveScore(partial, toan, "9.00");
        saveScore(partial, vatLi, "9.00");

        mockMvc.perform(get("/api/leaderboard/group-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sbd").value("20000001"));
    }

    @Test
    void getGroupATop10_tieOnTotalScore_breaksTiesBySbdAscending() throws Exception {
        seedGroupACandidate("30000002", "8.00", "8.00", "8.00"); // tied 24.00
        seedGroupACandidate("30000001", "8.00", "8.00", "8.00"); // tied 24.00, smaller sbd

        mockMvc.perform(get("/api/leaderboard/group-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sbd").value("30000001"))
                .andExpect(jsonPath("$[1].sbd").value("30000002"));
    }

    private void seedGroupACandidate(String sbd, String toanDiem, String lyDiem, String hoaDiem) {
        MonThi toan = monThiRepository.findByMaMon("toan").orElseThrow();
        MonThi vatLi = monThiRepository.findByMaMon("vat_li").orElseThrow();
        MonThi hoaHoc = monThiRepository.findByMaMon("hoa_hoc").orElseThrow();

        ThiSinh thiSinh = new ThiSinh();
        thiSinh.setSbd(sbd);
        thiSinh = thiSinhRepository.save(thiSinh);

        saveScore(thiSinh, toan, toanDiem);
        saveScore(thiSinh, vatLi, lyDiem);
        saveScore(thiSinh, hoaHoc, hoaDiem);
    }

    private void saveScore(ThiSinh thiSinh, MonThi monThi, String diem) {
        KetQuaThi kq = new KetQuaThi();
        kq.setThiSinh(thiSinh);
        kq.setMonThi(monThi);
        kq.setDiem(new BigDecimal(diem));
        ketQuaThiRepository.save(kq);
    }
}
