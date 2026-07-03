-- Fixed reference data: subject list and foreign-language codes used by
-- dataset/diem_thi_thpt_2024.csv (ma_mon values match the CSV header names).

INSERT INTO mon_thi (ma_mon, ten_mon) VALUES
    ('toan', 'Toán'),
    ('ngu_van', 'Ngữ văn'),
    ('ngoai_ngu', 'Ngoại ngữ'),
    ('vat_li', 'Vật lí'),
    ('hoa_hoc', 'Hóa học'),
    ('sinh_hoc', 'Sinh học'),
    ('lich_su', 'Lịch sử'),
    ('dia_li', 'Địa lí'),
    ('gdcd', 'Giáo dục công dân')
ON CONFLICT (ma_mon) DO NOTHING;

INSERT INTO ngoai_ngu (ma_ngoai_ngu, ten_ngoai_ngu) VALUES
    ('N1', 'Tiếng Anh'),
    ('N2', 'Tiếng Nga'),
    ('N3', 'Tiếng Pháp'),
    ('N4', 'Tiếng Trung'),
    ('N5', 'Tiếng Đức'),
    ('N6', 'Tiếng Nhật'),
    ('N7', 'Tiếng Hàn')
ON CONFLICT (ma_ngoai_ngu) DO NOTHING;
