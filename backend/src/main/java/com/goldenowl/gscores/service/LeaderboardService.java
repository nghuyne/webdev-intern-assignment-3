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
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Reads the Group A leaderboard from the Redis sorted set populated at seed time
 * (CsvScoreSeederService.GROUP_A_LEADERBOARD_KEY). Redis is only trusted once
 * migration_checkpoint for the seed file is COMPLETED - while a seed is still
 * PENDING/IN_PROGRESS (or failed), the sorted set can hold a partial batch range
 * (see CsvScoreSeederService.processBatch), so a ZCARD-only check would silently
 * serve wrong rankings instead of falling back to Postgres. A COMPLETED checkpoint
 * doesn't guarantee the ZSET is still intact either - if it comes back with fewer
 * than TOP_N members, that count is checked against KetQuaThiRepository#countGroupA
 * before being trusted as "the whole set" (see #fromRedis), otherwise data lost from
 * Redis after seeding (crash without persistence, eviction, stray ZREM) would be
 * served as a truncated but "complete" leaderboard.
 *
 * Tie-break rule (must match KetQuaThiRepository#findTop10GroupA): tong_diem DESC,
 * sbd ASC. Redis's own ZREVRANGE tie-break is member DESC (opposite of what we
 * want), so a plain reverseRangeWithScores(0, 9) can silently cut off the correct
 * winner when a tie spans the rank-10 boundary. We first read the score at rank 10,
 * then re-fetch every member with that score or higher so no tied candidate is
 * missed, and re-sort in Java before truncating to TOP_N.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaderboardService {

    private static final int TOP_N = 10;

    private static final Comparator<ZSetOperations.TypedTuple<String>> TIE_BREAK_ORDER =
            Comparator.comparing(ZSetOperations.TypedTuple<String>::getScore).reversed()
                    .thenComparing(ZSetOperations.TypedTuple::getValue);

    private final KetQuaThiRepository ketQuaThiRepository;
    private final MigrationCheckpointRepository checkpointRepository;
    private final SeederProperties seederProperties;
    private final StringRedisTemplate redisTemplate;

    public List<LeaderboardEntryDto> getGroupATop10() {
        if (isSeedComplete()) {
            List<LeaderboardEntryDto> result = fromRedis();
            if (result != null) {
                return result;
            }
        }

        return fallbackFromPostgres();
    }

    private boolean isSeedComplete() {
        return checkpointRepository.findByFileName(seederProperties.getFileName())
                .map(checkpoint -> checkpoint.getStatus() == CheckpointStatus.COMPLETED)
                .orElse(false);
    }

    private List<LeaderboardEntryDto> fromRedis() {
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<String>> top10 = zSetOperations
                .reverseRangeWithScores(CsvScoreSeederService.GROUP_A_LEADERBOARD_KEY, 0, TOP_N - 1);
        if (top10 == null || top10.isEmpty()) {
            return null;
        }
        if (top10.size() < TOP_N) {
            // reverseRangeWithScores(0, TOP_N - 1) is an index range, not a LIMIT: a result
            // shorter than TOP_N means the ZSET truly has only this many members right now,
            // so top10.size() here already equals ZCARD. That's only trustworthy as "the whole
            // set" if Redis didn't lose members (crash without persistence, eviction, stray
            // ZREM) after the checkpoint was marked COMPLETED - verify against the real
            // candidate count in Postgres before serving it as complete.
            if (top10.size() != ketQuaThiRepository.countGroupA()) {
                return null;
            }
            return toDtos(sortByTieBreakOrder(top10));
        }

        double cutoffScore = top10.stream()
                .mapToDouble(ZSetOperations.TypedTuple::getScore)
                .min()
                .orElseThrow();
        Set<ZSetOperations.TypedTuple<String>> candidates = zSetOperations.reverseRangeByScoreWithScores(
                CsvScoreSeederService.GROUP_A_LEADERBOARD_KEY, cutoffScore, Double.POSITIVE_INFINITY);
        List<ZSetOperations.TypedTuple<String>> sorted =
                sortByTieBreakOrder(candidates != null ? candidates : top10);
        return toDtos(sorted.subList(0, Math.min(TOP_N, sorted.size())));
    }

    private List<ZSetOperations.TypedTuple<String>> sortByTieBreakOrder(Set<ZSetOperations.TypedTuple<String>> tuples) {
        List<ZSetOperations.TypedTuple<String>> sorted = new ArrayList<>(tuples);
        sorted.sort(TIE_BREAK_ORDER);
        return sorted;
    }

    private List<LeaderboardEntryDto> toDtos(List<ZSetOperations.TypedTuple<String>> tuples) {
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
