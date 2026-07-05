package com.goldenowl.gscores.controller;

import com.goldenowl.gscores.support.AbstractApiIT;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubjectControllerApiIT extends AbstractApiIT {

    @Test
    void getSubjects_returnsAllNineReferenceSubjectsOrderedById() throws Exception {
        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[0].maMon").value("toan"))
                .andExpect(jsonPath("$[0].tenMon").value("Toán"));
    }
}
