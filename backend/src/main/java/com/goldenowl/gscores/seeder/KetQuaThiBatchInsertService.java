package com.goldenowl.gscores.seeder;

import com.goldenowl.gscores.entity.CheckpointStatus;
import com.goldenowl.gscores.entity.MigrationCheckpoint;
import com.goldenowl.gscores.repository.MigrationCheckpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Upserts one batch of parsed CSV rows into thi_sinh/ket_qua_thi and advances the
 * migration_checkpoint row in the same transaction, so a crash mid-batch never leaves
 * the checkpoint ahead of the data it describes.
 */
@Service
@RequiredArgsConstructor
public class KetQuaThiBatchInsertService {

    private static final String UPSERT_THI_SINH =
            "INSERT INTO thi_sinh (sbd, ngoai_ngu_id) VALUES (?, ?) " +
                    "ON CONFLICT (sbd) DO UPDATE SET sbd = EXCLUDED.sbd";

    private static final String SELECT_THI_SINH_IDS =
            "SELECT id, sbd FROM thi_sinh WHERE sbd = ANY(?)";

    private static final String INSERT_KET_QUA_THI =
            "INSERT INTO ket_qua_thi (thi_sinh_id, mon_thi_id, diem) VALUES (?, ?, ?) " +
                    "ON CONFLICT (thi_sinh_id, mon_thi_id) DO NOTHING";

    private final JdbcTemplate jdbcTemplate;
    private final MigrationCheckpointRepository checkpointRepository;

    @Transactional
    public void insertBatch(List<CsvRowParser.ParsedRow> rows,
                             Map<String, Long> monThiIdByMaMon,
                             Map<String, Long> ngoaiNguIdByCode,
                             String fileName,
                             long newLineOffset) {
        if (!rows.isEmpty()) {
            upsertThiSinhAndKetQuaThi(rows, monThiIdByMaMon, ngoaiNguIdByCode);
        }
        advanceCheckpoint(fileName, newLineOffset);
    }

    private void upsertThiSinhAndKetQuaThi(List<CsvRowParser.ParsedRow> rows,
                                            Map<String, Long> monThiIdByMaMon,
                                            Map<String, Long> ngoaiNguIdByCode) {
        List<Object[]> thiSinhArgs = rows.stream()
                .map(r -> new Object[]{r.sbd(), ngoaiNguIdByCode.get(r.maNgoaiNgu())})
                .toList();
        jdbcTemplate.batchUpdate(UPSERT_THI_SINH, thiSinhArgs);

        String[] sbds = rows.stream().map(CsvRowParser.ParsedRow::sbd).distinct().toArray(String[]::new);
        Map<String, Long> thiSinhIdBySbd = jdbcTemplate.query(
                con -> {
                    var ps = con.prepareStatement(SELECT_THI_SINH_IDS);
                    ps.setArray(1, con.createArrayOf("varchar", sbds));
                    return ps;
                },
                rs -> {
                    Map<String, Long> map = new HashMap<>();
                    while (rs.next()) {
                        map.put(rs.getString("sbd"), rs.getLong("id"));
                    }
                    return map;
                });

        List<Object[]> ketQuaArgs = new ArrayList<>();
        for (CsvRowParser.ParsedRow row : rows) {
            Long thiSinhId = thiSinhIdBySbd.get(row.sbd());
            if (thiSinhId == null) {
                throw new IllegalStateException("thi_sinh id not found after upsert for sbd=" + row.sbd());
            }
            for (Map.Entry<String, BigDecimal> entry : row.scores().entrySet()) {
                Long monThiId = monThiIdByMaMon.get(entry.getKey());
                if (monThiId == null) {
                    throw new IllegalStateException("Unknown subject column: " + entry.getKey());
                }
                ketQuaArgs.add(new Object[]{thiSinhId, monThiId, entry.getValue()});
            }
        }
        if (!ketQuaArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(INSERT_KET_QUA_THI, ketQuaArgs);
        }
    }

    private void advanceCheckpoint(String fileName, long newLineOffset) {
        MigrationCheckpoint checkpoint = checkpointRepository.findByFileName(fileName)
                .orElseThrow(() -> new IllegalStateException("Missing migration_checkpoint row for " + fileName));
        checkpoint.setLastLineOffset(newLineOffset);
        checkpoint.setStatus(CheckpointStatus.IN_PROGRESS);
        checkpoint.setUpdatedAt(Instant.now());
        checkpointRepository.save(checkpoint);
    }
}
