package com.goldenowl.gscores.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Runs the CSV seeder on startup when app.seeder.enabled=true. Off by default so the
 * seeder never runs as a side effect of starting the API for normal request handling.
 */
@Component
@ConditionalOnProperty(prefix = "app.seeder", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SeederRunner implements CommandLineRunner {

    private final CsvScoreSeederService seederService;

    @Override
    public void run(String... args) throws Exception {
        log.info("app.seeder.enabled=true, starting CSV seeder");
        seederService.run();
    }
}
