package com.goldenowl.gscores.service;

import com.goldenowl.gscores.config.SeederProperties;
import com.goldenowl.gscores.dto.BandCountDto;
import com.goldenowl.gscores.entity.CheckpointStatus;
import com.goldenowl.gscores.entity.MonThi;
import com.goldenowl.gscores.repository.KetQuaThiRepository;
import com.goldenowl.gscores.repository.MigrationCheckpointRepository;
import com.goldenowl.gscores.repository.MonThiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Band counts are cache-aside in Redis (report:band_counts:{monThiId}, 1h TTL): a hit
 * short-circuits Postgres entirely, a miss falls back to the indexed count query. The
 * result is only backfilled into Redis once migration_checkpoint for the seed file is
 * COMPLETED - checked before the count query runs, not after, so a count that reads a
 * still-COMPLETED checkpoint is guaranteed to see fully-seeded data (checkpoint status
 * only moves IN_PROGRESS -> COMPLETED, and COMPLETED is only committed once every batch
 * insert has committed). The TTL is a second line of defense against any other path that
 * could stale the cache.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final String BAND_COUNT_KEY_PREFIX = "report:band_counts:";
    private static final String FIELD_GIOI = "gioi";
    private static final String FIELD_KHA = "kha";
    private static final String FIELD_TRUNG_BINH = "trungBinh";
    private static final String FIELD_YEU = "yeu";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final MonThiRepository monThiRepository;
    private final KetQuaThiRepository ketQuaThiRepository;
    private final MigrationCheckpointRepository checkpointRepository;
    private final SeederProperties seederProperties;
    private final StringRedisTemplate redisTemplate;

    public List<BandCountDto> getBandCounts() {
        return monThiRepository.findAll(Sort.by("id")).stream()
                .map(this::getBandCountForSubject)
                .toList();
    }

    private BandCountDto getBandCountForSubject(MonThi subject) {
        String key = BAND_COUNT_KEY_PREFIX + subject.getId();
        Map<Object, Object> cached = redisTemplate.opsForHash().entries(key);
        if (!cached.isEmpty()) {
            return new BandCountDto(
                    subject.getMaMon(), subject.getTenMon(),
                    Long.parseLong(cached.get(FIELD_GIOI).toString()),
                    Long.parseLong(cached.get(FIELD_KHA).toString()),
                    Long.parseLong(cached.get(FIELD_TRUNG_BINH).toString()),
                    Long.parseLong(cached.get(FIELD_YEU).toString()));
        }

        boolean seedComplete = isSeedComplete();

        Object[] row = ketQuaThiRepository.countBandsByMonThiId(subject.getId()).get(0);
        long gioi = toLong(row[0]);
        long kha = toLong(row[1]);
        long trungBinh = toLong(row[2]);
        long yeu = toLong(row[3]);

        if (seedComplete) {
            redisTemplate.opsForHash().putAll(key, Map.of(
                    FIELD_GIOI, String.valueOf(gioi),
                    FIELD_KHA, String.valueOf(kha),
                    FIELD_TRUNG_BINH, String.valueOf(trungBinh),
                    FIELD_YEU, String.valueOf(yeu)));
            redisTemplate.expire(key, CACHE_TTL);
        }

        return new BandCountDto(subject.getMaMon(), subject.getTenMon(), gioi, kha, trungBinh, yeu);
    }

    private boolean isSeedComplete() {
        return checkpointRepository.findByFileName(seederProperties.getFileName())
                .map(checkpoint -> checkpoint.getStatus() == CheckpointStatus.COMPLETED)
                .orElse(false);
    }

    private long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
