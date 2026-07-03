package com.goldenowl.gscores.service;

import com.goldenowl.gscores.config.SeederProperties;
import com.goldenowl.gscores.dto.LeaderboardEntryDto;
import com.goldenowl.gscores.entity.CheckpointStatus;
import com.goldenowl.gscores.entity.MigrationCheckpoint;
import com.goldenowl.gscores.repository.KetQuaThiRepository;
import com.goldenowl.gscores.repository.MigrationCheckpointRepository;
import com.goldenowl.gscores.seeder.CsvScoreSeederService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    private static final String FILE_NAME = "diem_thi_thpt_2024.csv";

    @Mock
    private KetQuaThiRepository ketQuaThiRepository;
    @Mock
    private MigrationCheckpointRepository checkpointRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private LeaderboardService leaderboardService;

    @BeforeEach
    void setUp() {
        SeederProperties seederProperties = new SeederProperties();
        seederProperties.setFileName(FILE_NAME);
        leaderboardService = new LeaderboardService(ketQuaThiRepository, checkpointRepository,
                seederProperties, redisTemplate);
    }

    @Test
    void completed_readsTop10FromRedisZset_skipsPostgres() {
        givenCheckpointStatus(CheckpointStatus.COMPLETED);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        // LinkedHashSet preserves the descending-score order ZREVRANGE would return.
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>(List.of(
                ZSetOperations.TypedTuple.of("sbd001", 27.5),
                ZSetOperations.TypedTuple.of("sbd002", 26.0)));
        when(zSetOperations.reverseRangeWithScores(CsvScoreSeederService.GROUP_A_LEADERBOARD_KEY, 0, 9))
                .thenReturn(tuples);

        List<LeaderboardEntryDto> result = leaderboardService.getGroupATop10();

        assertThat(result).containsExactly(
                new LeaderboardEntryDto(1, "sbd001", BigDecimal.valueOf(27.5)),
                new LeaderboardEntryDto(2, "sbd002", BigDecimal.valueOf(26.0)));
        verify(ketQuaThiRepository, never()).findTop10GroupA();
    }

    @ParameterizedTest
    @EnumSource(value = CheckpointStatus.class, names = {"IN_PROGRESS", "PENDING", "FAILED"})
    void notCompleted_fallsBackToPostgres_neverTouchesRedisEvenIfDataPresent(CheckpointStatus status) {
        givenCheckpointStatus(status);
        when(ketQuaThiRepository.findTop10GroupA()).thenReturn(List.<Object[]>of(
                new Object[]{"sbd001", BigDecimal.valueOf(27.5)}));

        List<LeaderboardEntryDto> result = leaderboardService.getGroupATop10();

        assertThat(result).containsExactly(
                new LeaderboardEntryDto(1, "sbd001", BigDecimal.valueOf(27.5)));
        verify(ketQuaThiRepository).findTop10GroupA();
        // isSeedComplete() short-circuits before any ZSET read, regardless of what
        // the sorted set actually holds (e.g. a stale partial batch, ZCARD > 0).
        verify(redisTemplate, never()).opsForZSet();
    }

    // Missing-subject exclusion (Toan/Ly/Hoa) is enforced upstream, not in this service:
    // CsvScoreSeederService#writeGroupAScoresToRedis only pushes candidates whose CSV row
    // contains all 3 Group A subjects, and KetQuaThiRepository#findTop10GroupA relies on an
    // INNER JOIN per subject. These tests confirm the service is a faithful pass-through of
    // whatever the data source already filtered - it must not silently add or drop entries.

    @Test
    void completed_candidateMissingASubject_isAbsentBecauseRedisNeverIndexedIt() {
        givenCheckpointStatus(CheckpointStatus.COMPLETED);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        // sbd003 (missing Hoa) was never added to the ZSET by the seeder, so it can't appear here.
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>(List.of(
                ZSetOperations.TypedTuple.of("sbd001", 27.5),
                ZSetOperations.TypedTuple.of("sbd002", 26.0)));
        when(zSetOperations.reverseRangeWithScores(CsvScoreSeederService.GROUP_A_LEADERBOARD_KEY, 0, 9))
                .thenReturn(tuples);

        List<LeaderboardEntryDto> result = leaderboardService.getGroupATop10();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(LeaderboardEntryDto::sbd).doesNotContain("sbd003");
    }

    @Test
    void notCompleted_candidateMissingASubject_isAbsentBecausePostgresJoinExcludesIt() {
        givenCheckpointStatus(CheckpointStatus.PENDING);
        // sbd003 (missing Hoa) fails the INNER JOIN in findTop10GroupA, so the repo never returns it.
        when(ketQuaThiRepository.findTop10GroupA()).thenReturn(List.<Object[]>of(
                new Object[]{"sbd001", BigDecimal.valueOf(27.5)},
                new Object[]{"sbd002", BigDecimal.valueOf(26.0)}));

        List<LeaderboardEntryDto> result = leaderboardService.getGroupATop10();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(LeaderboardEntryDto::sbd).doesNotContain("sbd003");
    }

    private void givenCheckpointStatus(CheckpointStatus status) {
        MigrationCheckpoint checkpoint = new MigrationCheckpoint();
        checkpoint.setFileName(FILE_NAME);
        checkpoint.setStatus(status);
        when(checkpointRepository.findByFileName(FILE_NAME)).thenReturn(Optional.of(checkpoint));
    }
}
