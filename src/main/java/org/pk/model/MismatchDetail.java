package org.pk.model;

import java.util.Map;

public class MismatchDetail {
    private final String columnName;
    private final String sourceValue;
    private final String targetValue;
    private final double mismatchPercentage;
    private final String reconKeyColumn;
    private final String keyValue;
    private final Map<String, String> sourceRecord;
    private final Map<String, String> targetRecord;

    public MismatchDetail(String columnName, String sourceValue, String targetValue, double mismatchPercentage,
                          String reconKeyColumn, String keyValue, Map<String, String> sourceRecord, Map<String, String> targetRecord) {
        this.columnName = columnName;
        this.sourceValue = sourceValue;
        this.targetValue = targetValue;
        this.mismatchPercentage = mismatchPercentage;
        this.reconKeyColumn = reconKeyColumn;
        this.keyValue = keyValue;
        this.sourceRecord = sourceRecord;
        this.targetRecord = targetRecord;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getSourceValue() {
        return sourceValue;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public double getMismatchPercentage() {
        return mismatchPercentage;
    }

    public String getReconKeyColumn() {
        return reconKeyColumn;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public Map<String, String> getSourceRecord() {
        return sourceRecord;
    }

    public Map<String, String> getTargetRecord() {
        return targetRecord;
    }

    @Override
    public String toString() {
        return String.format("MismatchDetail{columnName='%s', sourceValue='%s', targetValue='%s', mismatchPercentage=%.2f%%}",
                columnName, sourceValue, targetValue, mismatchPercentage);
    }
}
