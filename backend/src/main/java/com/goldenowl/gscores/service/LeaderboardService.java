package com.goldenowl.gscores.service;

import com.goldenowl.gscores.config.SeederProperties;
import com.goldenowl.gscores.dto.LeaderboardEntryDto;
import com.goldenowl.gscores.entity.CheckpointStatus;
import com.goldenowl.gscores.repository.KetQuaThiRepository;
import com.goldenowl.gscores.repository.MigrationCheckpointRepository;
import com.goldenowl.gscores.seeder.CsvScoreSeederService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reads the Group A leaderboard from the Redis sorted set populated at seed time
 * (CsvScoreSeederService.GROUP_A_LEADERBOARD_KEY). Redis is only trusted once
 * migration_checkpoint for the seed file is COMPLETED - while a seed is still
 * PENDING/IN_PROGRESS (or failed), the sorted set can hold a partial batch range
 * (see CsvScoreSeederService.processBatch), so a ZCARD-only check would silently
 * serve wrong rankings instead of falling back to Postgres.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaderboardService {

    private static final int TOP_N = 10;

    private final KetQuaThiRepository ketQuaThiRepository;
    private final MigrationCheckpointRepository checkpointRepository;
    private final SeederProperties seederProperties;
    private final StringRedisTemplate redisTemplate;

    public List<LeaderboardEntryDto> getGroupATop10() {
        if (isSeedComplete()) {
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(CsvScoreSeederService.GROUP_A_LEADERBOARD_KEY, 0, TOP_N - 1);
            if (tuples != null && !tuples.isEmpty()) {
                return toDtos(tuples);
            }
        }

        return fallbackFromPostgres();
    }

    private boolean isSeedComplete() {
        return checkpointRepository.findByFileName(seederProperties.getFileName())
                .map(checkpoint -> checkpoint.getStatus() == CheckpointStatus.COMPLETED)
                .orElse(false);
    }

    private List<LeaderboardEntryDto> toDtos(Set<ZSetOperations.TypedTuple<String>> tuples) {
        List<LeaderboardEntryDto> result = new ArrayList<>(tuples.size());
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            result.add(new LeaderboardEntryDto(rank++, tuple.getValue(), BigDecimal.valueOf(tuple.getScore())));
        }
        return result;
    }

    private List<LeaderboardEntryDto> fallbackFromPostgres() {
        List<Object[]> rows = ketQuaThiRepository.findTop10GroupA();
        List<LeaderboardEntryDto> result = new ArrayList<>(rows.size());
        int rank = 1;
        for (Object[] row : rows) {
            result.add(new LeaderboardEntryDto(rank++, (String) row[0], (BigDecimal) row[1]));
        }
        return result;
    }
}
