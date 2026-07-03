-- G-Scores THPT 2024: core schema
-- Source columns (dataset/diem_thi_thpt_2024.csv):
-- sbd, toan, ngu_van, ngoai_ngu, vat_li, hoa_hoc, sinh_hoc, lich_su, dia_li, gdcd, ma_ngoai_ngu

CREATE TABLE ngoai_ngu (
    id             BIGSERIAL PRIMARY KEY,
    ma_ngoai_ngu   VARCHAR(10)  NOT NULL,
    ten_ngoai_ngu  VARCHAR(100),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_ngoai_ngu_ma UNIQUE (ma_ngoai_ngu)
);

CREATE TABLE mon_thi (
    id          BIGSERIAL PRIMARY KEY,
    ma_mon      VARCHAR(30)  NOT NULL,
    ten_mon     VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_mon_thi_ma_mon UNIQUE (ma_mon)
);

CREATE TABLE thi_sinh (
    id            BIGSERIAL PRIMARY KEY,
    sbd           VARCHAR(20) NOT NULL,
    ngoai_ngu_id  BIGINT REFERENCES ngoai_ngu (id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_thi_sinh_sbd UNIQUE (sbd)
);

-- Bo cot sbd denormalize. Lookup theo sbd di qua join thi_sinh (da co unique index).
CREATE TABLE ket_qua_thi (
    id            BIGSERIAL PRIMARY KEY,
    thi_sinh_id   BIGINT       NOT NULL REFERENCES thi_sinh (id),
    mon_thi_id    BIGINT       NOT NULL REFERENCES mon_thi (id),
    diem          NUMERIC(4,2) NOT NULL CHECK (diem >= 0 AND diem <= 10),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_ket_qua_thi_thi_sinh_mon UNIQUE (thi_sinh_id, mon_thi_id)
);

CREATE TABLE migration_checkpoint (
    id                 BIGSERIAL PRIMARY KEY,
    file_name          VARCHAR(255) NOT NULL,
    last_line_offset   BIGINT       NOT NULL DEFAULT 0,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_migration_checkpoint_file_name UNIQUE (file_name)
);

-- Phuc vu report band-count: GROUP BY mon_thi_id, dem theo diem. Index-only scan.
CREATE INDEX idx_ket_qua_thi_mon_diem ON ket_qua_thi (mon_thi_id, diem);

-- Phuc vu top 10 khoi A: WHERE mon_thi_id IN (...) GROUP BY thi_sinh_id SUM(diem).
CREATE INDEX idx_ket_qua_thi_mon_thisinh_diem ON ket_qua_thi (mon_thi_id, thi_sinh_id, diem);

-- FK support con thieu
CREATE INDEX idx_thi_sinh_ngoai_ngu_id ON thi_sinh (ngoai_ngu_id);
