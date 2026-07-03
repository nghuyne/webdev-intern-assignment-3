package com.goldenowl.gscores.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.seeder")
@Getter
@Setter
public class SeederProperties {

    private boolean enabled = false;
    private String csvPath = "../dataset/diem_thi_thpt_2024.csv";
    private String fileName = "diem_thi_thpt_2024.csv";
    private int batchSize = 2000;
}
