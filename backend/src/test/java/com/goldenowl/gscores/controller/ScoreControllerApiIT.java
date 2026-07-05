package com.goldenowl.gscores.controller;

import com.goldenowl.gscores.entity.KetQuaThi;
import com.goldenowl.gscores.entity.MonThi;
import com.goldenowl.gscores.entity.NgoaiNgu;
import com.goldenowl.gscores.entity.ThiSinh;
import com.goldenowl.gscores.support.AbstractApiIT;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScoreControllerApiIT extends AbstractApiIT {

    @Test
    void getScoreBySbd_existingCandidateWithForeignLanguage_returnsScoresAndLanguage() throws Exception {
        NgoaiNgu tiengAnh = ngoaiNguRepository.findByMaNgoaiNgu("N1").orElseThrow();
        ThiSinh thiSinh = new ThiSinh();
        thiSinh.setSbd("12345678");
        thiSinh.setNgoaiNgu(tiengAnh);
        thiSinh = thiSinhRepository.save(thiSinh);

        MonThi toan = monThiRepository.findByMaMon("toan").orElseThrow();
        KetQuaThi kq = new KetQuaThi();
        kq.setThiSinh(thiSinh);
        kq.setMonThi(toan);
        kq.setDiem(new BigDecimal("8.75"));
        ketQuaThiRepository.save(kq);

        mockMvc.perform(get("/api/scores/{sbd}", "12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sbd").value("12345678"))
                .andExpect(jsonPath("$.maNgoaiNgu").value("N1"))
                .andExpect(jsonPath("$.tenNgoaiNgu").value("Tiếng Anh"))
                .andExpect(jsonPath("$.scores.length()").value(1))
                .andExpect(jsonPath("$.scores[0].maMon").value("toan"))
                .andExpect(jsonPath("$.scores[0].diem").value(8.75));
    }

    @Test
    void getScoreBySbd_candidateWithoutForeignLanguageOrScores_returnsNullLanguageAndEmptyScores() throws Exception {
        ThiSinh thiSinh = new ThiSinh();
        thiSinh.setSbd("87654321");
        thiSinhRepository.save(thiSinh);

        mockMvc.perform(get("/api/scores/{sbd}", "87654321"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sbd").value("87654321"))
                .andExpect(jsonPath("$.maNgoaiNgu").value(nullValue()))
                .andExpect(jsonPath("$.tenNgoaiNgu").value(nullValue()))
                .andExpect(jsonPath("$.scores").isEmpty());
    }

    @Test
    void getScoreBySbd_unknownSbd_returns404WithApiError() throws Exception {
        mockMvc.perform(get("/api/scores/{sbd}", "99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/scores/99999999"));
    }

    @Test
    void getScoreBySbd_sbdNotEightDigits_returns400() throws Exception {
        mockMvc.perform(get("/api/scores/{sbd}", "abc123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
