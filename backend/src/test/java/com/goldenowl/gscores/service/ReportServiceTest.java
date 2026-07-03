package com.goldenowl.gscores.service;

import com.goldenowl.gscores.config.SeederProperties;
import com.goldenowl.gscores.dto.BandCountDto;
import com.goldenowl.gscores.entity.CheckpointStatus;
import com.goldenowl.gscores.entity.MigrationCheckpoint;
import com.goldenowl.gscores.entity.MonThi;
import com.goldenowl.gscores.repository.KetQuaThiRepository;
import com.goldenowl.gscores.repository.MigrationCheckpointRepository;
import com.goldenowl.gscores.repository.MonThiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final String FILE_NAME = "diem_thi_thpt_2024.csv";
    private static final Long SUBJECT_ID = 1L;
    private static final String CACHE_KEY = "report:band_counts:" + SUBJECT_ID;

    @Mock
    private MonThiRepository monThiRepository;
    @Mock
    private KetQuaThiRepository ketQuaThiRepository;
    @Mock
    private MigrationCheckpointRepository checkpointRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        SeederProperties seederProperties = new SeederProperties();
        seederProperties.setFileName(FILE_NAME);
        reportService = new ReportService(monThiRepository, ketQuaThiRepository, checkpointRepository,
                seederProperties, redisTemplate);

        MonThi toan = new MonThi();
        toan.setId(SUBJECT_ID);
        toan.setMaMon("toan");
        toan.setTenMon("Toan");

        when(monThiRepository.findAll(any(Sort.class))).thenReturn(List.of(toan));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void completedAndRedisHit_readsFromRedis_skipsPostgres() {
        // Cache-aside reads Redis unconditionally (only the write-back is gated on the
        // checkpoint), so a hit short-circuits before isSeedComplete() is ever consulted.
        when(hashOperations.entries(CACHE_KEY)).thenReturn(Map.<Object, Object>of(
                "gioi", "10", "kha", "20", "trungBinh", "30", "yeu", "5"));

        List<BandCountDto> result = reportService.getBandCounts();

        assertThat(result).hasSize(1);
        BandCountDto dto = result.get(0);
        assertThat(dto.maMon()).isEqualTo("toan");
        assertThat(dto.gioi()).isEqualTo(10);
        assertThat(dto.kha()).isEqualTo(20);
        assertThat(dto.trungBinh()).isEqualTo(30);
        assertThat(dto.yeu()).isEqualTo(5);

        verify(ketQuaThiRepository, never()).countBandsByMonThiId(anyLong());
        verify(hashOperations, never()).putAll(anyString(), anyMap());
        verify(checkpointRepository, never()).findByFileName(anyString());
    }

    @ParameterizedTest
    @EnumSource(value = CheckpointStatus.class, names = {"IN_PROGRESS", "PENDING", "FAILED"})
    void notCompleted_alwaysQueriesPostgres_neverBackfillsCache(CheckpointStatus status) {
        givenCheckpointStatus(status);
        // A re-seed is expected to DEL report:* first, so the cache read observes a miss here.
        when(hashOperations.entries(CACHE_KEY)).thenReturn(Map.of());
        when(ketQuaThiRepository.countBandsByMonThiId(SUBJECT_ID))
                .thenReturn(List.<Object[]>of(new Object[]{8L, 15L, 25L, 2L}));

        List<BandCountDto> result = reportService.getBandCounts();

        assertThat(result).hasSize(1);
        BandCountDto dto = result.get(0);
        assertThat(dto.gioi()).isEqualTo(8);
        assertThat(dto.kha()).isEqualTo(15);
        assertThat(dto.trungBinh()).isEqualTo(25);
        assertThat(dto.yeu()).isEqualTo(2);

        verify(ketQuaThiRepository).countBandsByMonThiId(SUBJECT_ID);
        verify(hashOperations, never()).putAll(anyString(), anyMap());
    }

    @Test
    void completedButRedisMiss_fallsBackToPostgres_andBackfillsCacheUnderCorrectKey() {
        givenCheckpointStatus(CheckpointStatus.COMPLETED);
        when(hashOperations.entries(CACHE_KEY)).thenReturn(Map.of());
        when(ketQuaThiRepository.countBandsByMonThiId(SUBJECT_ID))
                .thenReturn(List.<Object[]>of(new Object[]{3L, 4L, 5L, 6L}));

        List<BandCountDto> result = reportService.getBandCounts();

        assertThat(result).hasSize(1);
        BandCountDto dto = result.get(0);
        assertThat(dto.gioi()).isEqualTo(3);
        assertThat(dto.kha()).isEqualTo(4);
        assertThat(dto.trungBinh()).isEqualTo(5);
        assertThat(dto.yeu()).isEqualTo(6);

        verify(ketQuaThiRepository).countBandsByMonThiId(SUBJECT_ID);
        verify(hashOperations).putAll(CACHE_KEY, Map.of(
                "gioi", "3", "kha", "4", "trungBinh", "5", "yeu", "6"));
    }

    private void givenCheckpointStatus(CheckpointStatus status) {
        MigrationCheckpoint checkpoint = new MigrationCheckpoint();
        checkpoint.setFileName(FILE_NAME);
        checkpoint.setStatus(status);
        when(checkpointRepository.findByFileName(FILE_NAME)).thenReturn(Optional.of(checkpoint));
    }
}
