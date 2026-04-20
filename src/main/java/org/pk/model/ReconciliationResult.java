package org.pk.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReconciliationResult {
    private final long sourceRecordCount;
    private final long targetRecordCount;
    private final long matchingRecords;
    private final long missingInTarget;
    private final long extraInTarget;
    private final long mismatchedRecords;
    private final List<MismatchDetail> mismatchDetails;
    private final List<Map<String, String>> sourceOnlyRecords;
    private final List<Map<String, String>> targetOnlyRecords;
    private final List<Map<String, String>> duplicateRecords;
    private final long sourceDuplicateKeys;
    private final long targetDuplicateKeys;
    private final long recordsCompared;
    private final long processingTimeMs;
    private final String keyColumn;
    private final String delimiter;
    private final boolean hasHeaders;
    private final boolean keyAutoDetected;
    
    public ReconciliationResult(long sourceRecordCount, long targetRecordCount,
                             long matchingRecords, long missingInTarget,
                             long extraInTarget, long mismatchedRecords,
                             List<MismatchDetail> mismatchDetails,
                             List<Map<String, String>> sourceOnlyRecords,
                             List<Map<String, String>> targetOnlyRecords,
                             List<Map<String, String>> duplicateRecords,
                             long sourceDuplicateKeys, long targetDuplicateKeys,
                             long recordsCompared,
                             long processingTimeMs,
                             String keyColumn, String delimiter, boolean hasHeaders, boolean keyAutoDetected) {
        this.sourceRecordCount = sourceRecordCount;
        this.targetRecordCount = targetRecordCount;
        this.matchingRecords = matchingRecords;
        this.missingInTarget = missingInTarget;
        this.extraInTarget = extraInTarget;
        this.mismatchedRecords = mismatchedRecords;
        this.mismatchDetails = new ArrayList<>(mismatchDetails);
        this.sourceOnlyRecords = new ArrayList<>(sourceOnlyRecords);
        this.targetOnlyRecords = new ArrayList<>(targetOnlyRecords);
        this.duplicateRecords = new ArrayList<>(duplicateRecords);
        this.sourceDuplicateKeys = sourceDuplicateKeys;
        this.targetDuplicateKeys = targetDuplicateKeys;
        this.recordsCompared = recordsCompared;
        this.processingTimeMs = processingTimeMs;
        this.keyColumn = keyColumn;
        this.delimiter = delimiter;
        this.hasHeaders = hasHeaders;
        this.keyAutoDetected = keyAutoDetected;
    }
    
    public long getSourceRecordCount() {
        return sourceRecordCount;
    }
    
    public long getTargetRecordCount() {
        return targetRecordCount;
    }
    
    public long getMatchingRecords() {
        return matchingRecords;
    }
    
    public long getMissingInTarget() {
        return missingInTarget;
    }
    
    public long getExtraInTarget() {
        return extraInTarget;
    }
    
    public long getMismatchedRecords() {
        return mismatchedRecords;
    }
    
    public List<MismatchDetail> getMismatchDetails() {
        return mismatchDetails;
    }
    
    public List<Map<String, String>> getSourceOnlyRecords() {
        return sourceOnlyRecords;
    }
    
    public List<Map<String, String>> getTargetOnlyRecords() {
        return targetOnlyRecords;
    }
    
    public List<Map<String, String>> getDuplicateRecords() {
        return duplicateRecords;
    }
    
    public long getSourceDuplicateKeys() {
        return sourceDuplicateKeys;
    }
    
    public long getTargetDuplicateKeys() {
        return targetDuplicateKeys;
    }
    
    public long getRecordsCompared() {
        return recordsCompared;
    }
    
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public String getKeyColumn() {
        return keyColumn;
    }
    
    public String getDelimiter() {
        return delimiter;
    }
    
    public boolean isHasHeaders() {
        return hasHeaders;
    }

    public boolean isKeyAutoDetected() {
        return keyAutoDetected;
    }

    public double getMatchPercentage() {
        return sourceRecordCount > 0 ? (double) matchingRecords / sourceRecordCount * 100.0 : 0.0;
    }
    
    public boolean isPerfectMatch() {
        return matchingRecords == sourceRecordCount && 
               matchingRecords == targetRecordCount && 
               mismatchedRecords == 0;
    }
    
    @Override
    public String toString() {
        return String.format(
            "ReconciliationResult{\n" +
            "  Source Records: %d\n" +
            "  Target Records: %d\n" +
            "  Records Compared: %d\n" +
            "  Matching Records: %d (%.2f%%)\n" +
            "  Missing in Target: %d\n" +
            "  Extra in Target: %d\n" +
            "  Mismatched Records: %d\n" +
            "  Processing Time: %d ms\n" +
            "}",
            sourceRecordCount, targetRecordCount, recordsCompared, matchingRecords, getMatchPercentage(),
            missingInTarget, extraInTarget, mismatchedRecords, processingTimeMs
        );
    }
}
