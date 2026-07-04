package com.goldenowl.gscores.repository;

import com.goldenowl.gscores.entity.KetQuaThi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KetQuaThiRepository extends JpaRepository<KetQuaThi, Long> {

    // Dung cho GET /api/scores/{sbd}. Join qua thi_sinh.sbd (unique index), khong can cot sbd rieng tren bang nay.
    @Query("SELECT k FROM KetQuaThi k " +
            "JOIN FETCH k.monThi " +
            "WHERE k.thiSinh.sbd = :sbd " +
            "ORDER BY k.monThi.id")
    List<KetQuaThi> findAllByThiSinhSbd(@Param("sbd") String sbd);

    // Fallback report band-count khi Redis miss. k.monThi.id chi doc cot FK mon_thi_id, khong join bang mon_thi,
    // nen dung thang duoc idx_ket_qua_thi_mon_diem (mon_thi_id, diem).
    @Query("SELECT " +
            "SUM(CASE WHEN k.diem >= 8 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN k.diem >= 6 AND k.diem < 8 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN k.diem >= 4 AND k.diem < 6 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN k.diem < 4 THEN 1 ELSE 0 END) " +
            "FROM KetQuaThi k WHERE k.monThi.id = :monThiId")
    List<Object[]> countBandsByMonThiId(@Param("monThiId") Long monThiId);

    // Fallback top 10 khoi A khi Redis mat du lieu. ma_mon khop dung ten cot CSV goc: toan, vat_li, hoa_hoc.
    @Query(value = """
            SELECT t.sbd AS sbd,
                   (toan.diem + ly.diem + hoa.diem) AS tong_diem
            FROM thi_sinh t
            JOIN ket_qua_thi toan ON toan.thi_sinh_id = t.id
                AND toan.mon_thi_id = (SELECT id FROM mon_thi WHERE ma_mon = 'toan')
            JOIN ket_qua_thi ly ON ly.thi_sinh_id = t.id
                AND ly.mon_thi_id = (SELECT id FROM mon_thi WHERE ma_mon = 'vat_li')
            JOIN ket_qua_thi hoa ON hoa.thi_sinh_id = t.id
                AND hoa.mon_thi_id = (SELECT id FROM mon_thi WHERE ma_mon = 'hoa_hoc')
            ORDER BY tong_diem DESC, t.sbd ASC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> findTop10GroupA();

    // Dung de xac minh ZSET leaderboard:groupA con nguyen ven khi no tra ve it hon TOP_N
    // member (xem LeaderboardService#fromRedis) - so ZCARD voi tong so thi sinh khoi A that
    // trong Postgres truoc khi tin day la toan bo tap hop thay vi Redis bi mat du lieu.
    @Query(value = """
            SELECT COUNT(*)
            FROM thi_sinh t
            JOIN ket_qua_thi toan ON toan.thi_sinh_id = t.id
                AND toan.mon_thi_id = (SELECT id FROM mon_thi WHERE ma_mon = 'toan')
            JOIN ket_qua_thi ly ON ly.thi_sinh_id = t.id
                AND ly.mon_thi_id = (SELECT id FROM mon_thi WHERE ma_mon = 'vat_li')
            JOIN ket_qua_thi hoa ON hoa.thi_sinh_id = t.id
                AND hoa.mon_thi_id = (SELECT id FROM mon_thi WHERE ma_mon = 'hoa_hoc')
            """, nativeQuery = true)
    long countGroupA();
}
