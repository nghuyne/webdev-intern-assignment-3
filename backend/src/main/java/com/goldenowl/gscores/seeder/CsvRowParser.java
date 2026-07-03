package com.goldenowl.gscores.seeder;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses one line of dataset/diem_thi_thpt_2024.csv against a header-derived column mapping.
 * Subject column names in the header are used verbatim as mon_thi.ma_mon values.
 */
public final class CsvRowParser {

    private CsvRowParser() {
    }

    public record ParsedRow(String sbd, Map<String, BigDecimal> scores, String maNgoaiNgu) {
    }

    public record ColumnMapping(int sbdIndex, int maNgoaiNguIndex, Map<Integer, String> subjectColumns,
                                 int columnCount) {
    }

    public static ColumnMapping parseHeader(String headerLine) {
        String[] columns = headerLine.split(",", -1);
        int sbdIndex = -1;
        int maNgoaiNguIndex = -1;
        Map<Integer, String> subjectColumns = new LinkedHashMap<>();

        for (int i = 0; i < columns.length; i++) {
            String name = columns[i].trim();
            if ("sbd".equals(name)) {
                sbdIndex = i;
            } else if ("ma_ngoai_ngu".equals(name)) {
                maNgoaiNguIndex = i;
            } else {
                subjectColumns.put(i, name);
            }
        }

        if (sbdIndex == -1) {
            throw new IllegalStateException("CSV header missing required 'sbd' column");
        }
        return new ColumnMapping(sbdIndex, maNgoaiNguIndex, subjectColumns, columns.length);
    }

    public static ParsedRow parseLine(String line, ColumnMapping mapping) {
        String[] fields = line.split(",", -1);
        if (fields.length != mapping.columnCount()) {
            throw new IllegalArgumentException(
                    "expected " + mapping.columnCount() + " columns, got " + fields.length);
        }

        String sbd = fields[mapping.sbdIndex()].trim();
        if (sbd.isEmpty()) {
            throw new IllegalArgumentException("blank sbd");
        }

        String maNgoaiNgu = mapping.maNgoaiNguIndex() >= 0
                ? fields[mapping.maNgoaiNguIndex()].trim()
                : "";

        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : mapping.subjectColumns().entrySet()) {
            String raw = fields[entry.getKey()].trim();
            if (!raw.isEmpty()) {
                scores.put(entry.getValue(), new BigDecimal(raw));
            }
        }

        return new ParsedRow(sbd, scores, maNgoaiNgu);
    }
}
