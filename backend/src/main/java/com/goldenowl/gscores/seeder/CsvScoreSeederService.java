package com.goldenowl.gscores.seeder;

import com.goldenowl.gscores.config.SeederProperties;
import com.goldenowl.gscores.entity.CheckpointStatus;
import com.goldenowl.gscores.entity.MigrationCheckpoint;
import com.goldenowl.gscores.entity.MonThi;
import com.goldenowl.gscores.entity.NgoaiNgu;
import com.goldenowl.gscores.repository.MigrationCheckpointRepository;
import com.goldenowl.gscores.repository.MonThiRepository;
import com.goldenowl.gscores.repository.NgoaiNguRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.springframework.core.io.ClassPathResource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads dataset/diem_thi_thpt_2024.csv in batches, resuming from migration_checkpoint,
 * unpivots each row into ket_qua_thi inserts, and pushes Group A (Toan/Ly/Hoa) totals
 * into a Redis sorted set as each batch commits.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CsvScoreSeederService {

    public static final String GROUP_A_LEADERBOARD_KEY = "leaderboard:groupA";
    private static final List<String> GROUP_A_SUBJECTS = List.of("toan", "vat_li", "hoa_hoc");

    private final SeederProperties properties;
    private final MigrationCheckpointRepository checkpointRepository;
    private final MonThiRepository monThiRepository;
    private final NgoaiNguRepository ngoaiNguRepository;
    private final KetQuaThiBatchInsertService batchInsertService;
    private final StringRedisTemplate redisTemplate;

    public void run() throws IOException {
        MigrationCheckpoint checkpoint = checkpointRepository.findByFileName(properties.getFileName())
                .orElseGet(this::createInitialCheckpoint);
        if (checkpoint.getStatus() == CheckpointStatus.COMPLETED) {
            log.info("Seed already completed for {}, nothing to do", properties.getFileName());
            return;
        }

        Map<String, Long> monThiIds = monThiRepository.findAll().stream()
                .collect(Collectors.toMap(MonThi::getMaMon, MonThi::getId));
        Map<String, Long> ngoaiNguIds = ngoaiNguRepository.findAll().stream()
                .collect(Collectors.toMap(NgoaiNgu::getMaNgoaiNgu, NgoaiNgu::getId));

        String rawPath = properties.getCsvPath();
        long skip = checkpoint.getLastLineOffset();
        log.info("Starting seed for {} from line offset {}", properties.getFileName(), skip);

        try (BufferedReader reader = openCsv(rawPath)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalStateException("CSV file is empty: " + rawPath);
            }
            CsvRowParser.ColumnMapping mapping = CsvRowParser.parseHeader(headerLine);

            for (long i = 0; i < skip; i++) {
                if (reader.readLine() == null) {
                    throw new IllegalStateException(
                            "Checkpoint offset " + skip + " exceeds file length for " + rawPath);
                }
            }

            long offset = skip;
            int batchSize = properties.getBatchSize();
            List<String> batchLines = new ArrayList<>(batchSize);
            String line;
            while ((line = reader.readLine()) != null) {
                batchLines.add(line);
                if (batchLines.size() >= batchSize) {
                    offset += batchLines.size();
                    processBatch(batchLines, mapping, monThiIds, ngoaiNguIds, offset);
                    log.info("Processed {} lines so far", offset);
                    batchLines.clear();
                }
            }
            if (!batchLines.isEmpty()) {
                offset += batchLines.size();
                processBatch(batchLines, mapping, monThiIds, ngoaiNguIds, offset);
            }
        }

        markCompleted();
        log.info("Seed completed for {}", properties.getFileName());
    }

    private void processBatch(List<String> lines,
                               CsvRowParser.ColumnMapping mapping,
                               Map<String, Long> monThiIds,
                               Map<String, Long> ngoaiNguIds,
                               long newLineOffset) {
        List<CsvRowParser.ParsedRow> rows = new ArrayList<>(lines.size());
        for (String line : lines) {
            try {
                rows.add(CsvRowParser.parseLine(line, mapping));
            } catch (RuntimeException e) {
                log.warn("Skipping malformed CSV line ({}): {}", e.getMessage(), line);
            }
        }

        // Redis must be written before the Postgres transaction that advances the
        // checkpoint commits. If the process crashes between them, checkpoint would move
        // past a batch whose Group A scores never reached the sorted set - and a resumed
        // run never revisits an already-committed line range, so that gap would be
        // permanent. Writing Redis first means a crash before the Postgres commit just
        // gets the whole batch retried on resume (both writes are idempotent).
        writeGroupAScoresToRedis(rows);
        batchInsertService.insertBatch(rows, monThiIds, ngoaiNguIds, properties.getFileName(), newLineOffset);
    }

    private void writeGroupAScoresToRedis(List<CsvRowParser.ParsedRow> rows) {
        List<GroupAScore> groupAScores = new ArrayList<>();
        for (CsvRowParser.ParsedRow row : rows) {
            if (row.scores().keySet().containsAll(GROUP_A_SUBJECTS)) {
                BigDecimal total = GROUP_A_SUBJECTS.stream()
                        .map(row.scores()::get)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                groupAScores.add(new GroupAScore(row.sbd(), total));
            }
        }
        if (groupAScores.isEmpty()) {
            return;
        }

        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
        for (GroupAScore score : groupAScores) {
            tuples.add(ZSetOperations.TypedTuple.of(score.sbd(), score.total().doubleValue()));
        }
        redisTemplate.opsForZSet().add(GROUP_A_LEADERBOARD_KEY, tuples);
    }

    /**
     * Supports two path formats:
     * - "classpath:dataset/foo.csv" → reads from jar/classpath (Railway / embedded)
     * - "/absolute/path/foo.csv"    → reads from filesystem (Docker Compose local mount)
     */
    private BufferedReader openCsv(String path) throws IOException {
        if (path.startsWith("classpath:")) {
            String classpathLocation = path.substring("classpath:".length());
            return new BufferedReader(new InputStreamReader(
                    new ClassPathResource(classpathLocation).getInputStream(), StandardCharsets.UTF_8));
        }
        return Files.newBufferedReader(Path.of(path), StandardCharsets.UTF_8);
    }

    private MigrationCheckpoint createInitialCheckpoint() {
        MigrationCheckpoint checkpoint = new MigrationCheckpoint();
        checkpoint.setFileName(properties.getFileName());
        checkpoint.setLastLineOffset(0L);
        checkpoint.setStatus(CheckpointStatus.IN_PROGRESS);
        checkpoint.setUpdatedAt(Instant.now());
        return checkpointRepository.save(checkpoint);
    }

    private void markCompleted() {
        MigrationCheckpoint checkpoint = checkpointRepository.findByFileName(properties.getFileName())
                .orElseThrow(() -> new IllegalStateException("Missing checkpoint for " + properties.getFileName()));
        checkpoint.setStatus(CheckpointStatus.COMPLETED);
        checkpoint.setUpdatedAt(Instant.now());
        checkpointRepository.save(checkpoint);
    }
}
