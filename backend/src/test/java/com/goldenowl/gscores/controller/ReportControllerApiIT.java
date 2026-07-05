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

class ReportControllerApiIT extends AbstractApiIT {

    @Test
    void getBandCounts_noCompletedCheckpoint_computesBandsFromPostgresForEverySubject() throws Exception {
        MonThi toan = monThiRepository.findByMaMon("toan").orElseThrow();
        seedScore("11111111", toan, "9.00"); // gioi: >= 8
        seedScore("22222222", toan, "6.50"); // kha: [6, 8)
        seedScore("33333333", toan, "5.00"); // trungBinh: [4, 6)
        seedScore("44444444", toan, "3.00"); // yeu: < 4

        mockMvc.perform(get("/api/report/band-counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                // toan has id=1 (first row inserted by V2 migration) so it sorts first.
                .andExpect(jsonPath("$[0].maMon").value("toan"))
                .andExpect(jsonPath("$[0].gioi").value(1))
                .andExpect(jsonPath("$[0].kha").value(1))
                .andExpect(jsonPath("$[0].trungBinh").value(1))
                .andExpect(jsonPath("$[0].yeu").value(1));
    }

    @Test
    void getBandCounts_subjectWithNoScores_returnsZeroCountsInsteadOfNull() throws Exception {
        mockMvc.perform(get("/api/report/band-counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].gioi").value(0))
                .andExpect(jsonPath("$[0].kha").value(0))
                .andExpect(jsonPath("$[0].trungBinh").value(0))
                .andExpect(jsonPath("$[0].yeu").value(0));
    }

    private void seedScore(String sbd, MonThi monThi, String diem) {
        ThiSinh thiSinh = new ThiSinh();
        thiSinh.setSbd(sbd);
        thiSinh = thiSinhRepository.save(thiSinh);

        KetQuaThi kq = new KetQuaThi();
        kq.setThiSinh(thiSinh);
        kq.setMonThi(monThi);
        kq.setDiem(new BigDecimal(diem));
        ketQuaThiRepository.save(kq);
    }
}
