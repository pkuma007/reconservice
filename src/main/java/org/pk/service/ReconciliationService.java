package org.pk.service;

import org.pk.config.ReconciliationConfig;
import org.pk.model.ReconciliationResult;
import org.pk.reader.StreamingCsvReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ReconciliationService {
    private static final Logger logger = LoggerFactory.getLogger(ReconciliationService.class);
    
    private final ReconciliationConfig config;
    
    public ReconciliationService(ReconciliationConfig config) {
        this.config = config;
        validateConfig();
    }
    
    public ReconciliationResult reconcile(String sourceFilePath, String targetFilePath) throws IOException {
        logger.info("Starting reconciliation between source: {} and target: {}", sourceFilePath, targetFilePath);
        logger.info("Configuration - Key column: {}, Delimiter: {}, Has header: {}", 
                    config.getKeyColumn(), config.getDelimiter(), config.isHasHeader());
        
        long startTime = System.currentTimeMillis();
        
        StreamingCsvReader reader = new StreamingCsvReader(config);
        
        Map<String, Map<String, String>> sourceRecords = new ConcurrentHashMap<>();
        Map<String, Map<String, String>> targetRecords = new ConcurrentHashMap<>();
        Map<String, Integer> sourceKeyCounts = new ConcurrentHashMap<>();
        Map<String, Integer> targetKeyCounts = new ConcurrentHashMap<>();
        List<Map<String, String>> sourceDuplicateRecords = new ArrayList<>();
        List<Map<String, String>> targetDuplicateRecords = new ArrayList<>();
        
        AtomicLong sourceCount = new AtomicLong(0);
        AtomicLong targetCount = new AtomicLong(0);
        AtomicLong sourceDuplicates = new AtomicLong(0);
        AtomicLong targetDuplicates = new AtomicLong(0);
        
        reader.processFile(sourceFilePath, record -> {
            String key = extractKey(record, true);
            if (key != null) {
                sourceKeyCounts.put(key, sourceKeyCounts.getOrDefault(key, 0) + 1);
                sourceRecords.put(key, new HashMap<>(record));
                sourceDuplicateRecords.add(new HashMap<>(record)); // Collect all records
                sourceCount.incrementAndGet();
                if (sourceCount.get() <= 5) {
                    logger.debug("Source record {} - Key: {}, Record: {}", sourceCount.get(), key, record);
                }
            }
        });
        
        reader.processFile(targetFilePath, record -> {
            String key = extractKey(record, false);
            if (key != null) {
                targetKeyCounts.put(key, targetKeyCounts.getOrDefault(key, 0) + 1);
                targetRecords.put(key, new HashMap<>(record));
                targetDuplicateRecords.add(new HashMap<>(record)); // Collect all records
                targetCount.incrementAndGet();
                if (targetCount.get() <= 5) {
                    logger.debug("Target record {} - Key: {}, Record: {}", targetCount.get(), key, record);
                }
            }
        });
        
        logger.info("File reading complete - Source: {} records, Target: {} records", 
                    sourceCount.get(), targetCount.get());
        
        // Calculate duplicate counts for logging
        long sourceDuplicateCount = sourceKeyCounts.values().stream().filter(count -> count > 1).mapToLong(count -> count - 1).sum();
        long targetDuplicateCount = targetKeyCounts.values().stream().filter(count -> count > 1).mapToLong(count -> count - 1).sum();
        
        if (sourceDuplicateCount > 0) {
            logger.warn("Source file contains {} duplicate keys - this may affect reconciliation accuracy", sourceDuplicateCount);
        }
        
        if (targetDuplicateCount > 0) {
            logger.warn("Target file contains {} duplicate keys - this may affect reconciliation accuracy", targetDuplicateCount);
        }
        
        return compareRecords(sourceRecords, targetRecords, sourceKeyCounts, targetKeyCounts, sourceDuplicateRecords, targetDuplicateRecords, sourceCount.get(), targetCount.get(), 
                             System.currentTimeMillis() - startTime);
    }
    
    private ReconciliationResult compareRecords(Map<String, Map<String, String>> sourceRecords,
                                              Map<String, Map<String, String>> targetRecords,
                                              Map<String, Integer> sourceKeyCounts,
                                              Map<String, Integer> targetKeyCounts,
                                              List<Map<String, String>> sourceDuplicateRecords,
                                              List<Map<String, String>> targetDuplicateRecords,
                                              long sourceCount, long targetCount,
                                              long processingTime) {
        
        logger.info("Starting comparison - Source keys: {}, Target keys: {}", sourceRecords.keySet().size(), targetRecords.keySet().size());
        
        long matchingRecords = 0;
        long missingInTarget = 0;
        long extraInTarget = 0;
        long mismatchedRecords = 0;
        List<org.pk.model.MismatchDetail> mismatchDetails = new ArrayList<>();
        List<Map<String, String>> sourceOnlyRecords = new ArrayList<>();
        List<Map<String, String>> targetOnlyRecords = new ArrayList<>();
        List<Map<String, String>> duplicateRecords = new ArrayList<>();
        Set<String> mismatchedRecordKeys = new HashSet<>();
        
        Set<String> allKeys = new TreeSet<>();
        allKeys.addAll(sourceRecords.keySet());
        allKeys.addAll(targetRecords.keySet());
        
        for (String key : allKeys) {
            Map<String, String> sourceRecord = sourceRecords.get(key);
            Map<String, String> targetRecord = targetRecords.get(key);
            
            if (sourceRecord != null && targetRecord != null) {
                if (recordsMatch(sourceRecord, targetRecord)) {
                    matchingRecords++;
                } else {
                    mismatchedRecordKeys.add(key);
                    List<org.pk.model.MismatchDetail> recordMismatches = createMismatchDetails(key, sourceRecord, targetRecord);
                    mismatchedRecords++; // Count mismatched rows, not mismatched attributes
                    mismatchDetails.addAll(recordMismatches);
                }
            } else if (sourceRecord != null) {
                missingInTarget++;
                // Add key column name and key value to the record
                Map<String, String> recordWithKey = new HashMap<>(sourceRecord);
                recordWithKey.put("ReconKeyColumn", config.getKeyColumn());
                recordWithKey.put("keyValue", key);
                sourceOnlyRecords.add(recordWithKey);
                logger.debug("Missing in target - Key: {}, Record: {}", key, sourceRecord);
            } else {
                extraInTarget++;
                // Add key column name and key value to the record
                Map<String, String> recordWithKey = new HashMap<>(targetRecord);
                recordWithKey.put("ReconKeyColumn", config.getKeyColumn());
                recordWithKey.put("keyValue", key);
                targetOnlyRecords.add(recordWithKey);
                logger.debug("Extra in target - Key: {}, Record: {}", key, targetRecord);
            }
        }
        
        // Add duplicate records to duplicateRecords list for UI display
        // Add all records where the key appears more than once in the same file
        for (Map<String, String> record : sourceDuplicateRecords) {
            String key = extractKey(record, true);
            int count = sourceKeyCounts.getOrDefault(key, 0);
            if (count > 1) {
                Map<String, String> recordWithKey = new HashMap<>(record);
                recordWithKey.put("ReconKeyColumn", config.getKeyColumn());
                recordWithKey.put("keyValue", key);
                recordWithKey.put("Source", "Source");
                recordWithKey.put("DuplicateCount", String.valueOf(count));
                duplicateRecords.add(recordWithKey);
            }
        }
        
        for (Map<String, String> record : targetDuplicateRecords) {
            String key = extractKey(record, false);
            int count = targetKeyCounts.getOrDefault(key, 0);
            if (count > 1) {
                Map<String, String> recordWithKey = new HashMap<>(record);
                recordWithKey.put("ReconKeyColumn", config.getKeyColumn());
                recordWithKey.put("keyValue", key);
                recordWithKey.put("Source", "Target");
                recordWithKey.put("DuplicateCount", String.valueOf(count));
                duplicateRecords.add(recordWithKey);
            }
        }
        
        // Calculate duplicate counts (total records that are part of duplicate keys)
        long sourceDuplicateCount = sourceKeyCounts.values().stream().filter(count -> count > 1).mapToLong(count -> count).sum();
        long targetDuplicateCount = targetKeyCounts.values().stream().filter(count -> count > 1).mapToLong(count -> count).sum();
        
        // Adjust counts to include duplicates as missing/extra
        // If there are duplicates in source, they should be counted as "missing" from target perspective
        // If there are duplicates in target, they should be counted as "extra" from source perspective
        long adjustedMissingInTarget = missingInTarget + sourceDuplicateCount;
        long adjustedExtraInTarget = extraInTarget + targetDuplicateCount;
        
        // Adjust matching records to account for duplicates
        // The matching should be: total records - (missing + extra + mismatched)
        long adjustedMatchingRecords = sourceCount - adjustedMissingInTarget - mismatchedRecords;
        
        // Calculate records compared (unique keys that were actually compared)
        long recordsCompared = matchingRecords + missingInTarget + extraInTarget + mismatchedRecordKeys.size();
        
        logger.info("Comparison complete - Matching: {}, Missing: {}, Extra: {}, Mismatched: {}", 
                    matchingRecords, missingInTarget, extraInTarget, mismatchedRecords);
        logger.info("Adjusted for duplicates - Matching: {}, Missing: {}, Extra: {}", 
                    adjustedMatchingRecords, adjustedMissingInTarget, adjustedExtraInTarget);
        logger.info("Records compared: {}", recordsCompared);
        
        return new ReconciliationResult(sourceCount, targetCount, adjustedMatchingRecords,
                                       missingInTarget, extraInTarget, mismatchedRecords,
                                       mismatchDetails, sourceOnlyRecords, targetOnlyRecords, duplicateRecords,
                                       sourceDuplicateCount, targetDuplicateCount,
                                       recordsCompared,
                                       processingTime,
                                       config.getKeyColumn(),
                                       String.valueOf(config.getDelimiter()),
                                       config.isHasHeader(),
                                       config.isKeyAutoDetected());
    }
    
    private String extractKey(Map<String, String> record, boolean isSource) {
        // Use advanced key configuration if enabled
        List<String> keyColumns = isSource ? config.getSourceKeyColumns() : config.getTargetKeyColumns();
        if (keyColumns != null && !keyColumns.isEmpty()) {
            return generateKey(record, keyColumns, config.getConcatenationSeparator(), config.getColumnMapping());
        }
        
        // Fall back to legacy key column logic
        String keyColumn = config.getKeyColumn();
        if (keyColumn == null || keyColumn.trim().isEmpty()) {
            logger.warn("No key column specified, using first available column as key");
            return record.isEmpty() ? null : record.values().iterator().next();
        }
        
        // Check if it's a composite key (comma-separated columns)
        if (keyColumn.contains(",")) {
            return extractCompositeKey(keyColumn, record);
        }
        
        String key = record.get(keyColumn);
        if (key == null) {
            logger.warn("Key column '{}' not found in record: {}", keyColumn, record.keySet());
            return null;
        }
        
        return config.isIgnoreCase() ? key.toLowerCase() : key;
    }

    private String generateKey(Map<String, String> record, List<String> keyColumns, String separator, Map<String, String> columnMapping) {
        if (keyColumns == null || keyColumns.isEmpty()) {
            return null;
        }
        
        if (keyColumns.size() == 1) {
            String column = keyColumns.get(0);
            String actualColumn = columnMapping != null && columnMapping.containsKey(column) ? columnMapping.get(column) : column;
            String key = record.get(actualColumn);
            if (key == null) {
                logger.warn("Key column '{}' not found in record: {}", actualColumn, record.keySet());
                return null;
            }
            return config.isIgnoreCase() ? key.trim().toLowerCase() : key.trim();
        }
        
        StringBuilder compositeKey = new StringBuilder();
        for (int i = 0; i < keyColumns.size(); i++) {
            String column = keyColumns.get(i);
            String actualColumn = columnMapping != null && columnMapping.containsKey(column) ? columnMapping.get(column) : column;
            String value = record.get(actualColumn);
            
            if (value == null) {
                logger.warn("Key column '{}' not found in record: {}", actualColumn, record.keySet());
                return null;
            }
            
            if (i > 0) compositeKey.append(separator != null ? separator : "|");
            compositeKey.append(config.isIgnoreCase() ? value.trim().toLowerCase() : value.trim());
        }
        
        return compositeKey.toString();
    }
    
    private String extractCompositeKey(String keyColumns, Map<String, String> record) {
        String[] columns = keyColumns.split(",");
        StringBuilder compositeKey = new StringBuilder();
        
        for (int i = 0; i < columns.length; i++) {
            String column = columns[i].trim();
            String value = record.get(column);
            
            if (value == null) {
                logger.warn("Composite key column '{}' not found in record: {}", column, record.keySet());
                return null;
            }
            
            if (i > 0) compositeKey.append("|");
            compositeKey.append(config.isIgnoreCase() ? value.trim().toLowerCase() : value.trim());
        }
        
        return compositeKey.toString();
    }
    
    private boolean recordsMatch(Map<String, String> sourceRecord, Map<String, String> targetRecord) {
        Set<String> allColumns = new TreeSet<>();
        allColumns.addAll(sourceRecord.keySet());
        allColumns.addAll(targetRecord.keySet());
        
        for (String column : allColumns) {
            String sourceValue = sourceRecord.get(column);
            String targetValue = targetRecord.get(column);
            
            if (config.isIgnoreCase()) {
                if (sourceValue != null) sourceValue = sourceValue.toLowerCase();
                if (targetValue != null) targetValue = targetValue.toLowerCase();
            }
            
            if (!Objects.equals(sourceValue, targetValue)) {
                return false;
            }
        }
        
        return true;
    }
    
    private List<org.pk.model.MismatchDetail> createMismatchDetails(String key, Map<String, String> sourceRecord, Map<String, String> targetRecord) {
        List<org.pk.model.MismatchDetail> details = new ArrayList<>();
        
        Set<String> allColumns = new TreeSet<>();
        allColumns.addAll(sourceRecord.keySet());
        allColumns.addAll(targetRecord.keySet());
        
        for (String column : allColumns) {
            String sourceValue = sourceRecord.get(column);
            String targetValue = targetRecord.get(column);
            
            if (!Objects.equals(sourceValue, targetValue)) {
                // Calculate mismatch percentage
                double mismatchPercentage = calculateMismatchPercentage(sourceValue, targetValue);
                details.add(new org.pk.model.MismatchDetail(column, sourceValue, targetValue, mismatchPercentage,
                                                          config.getKeyColumn(), key, sourceRecord, targetRecord));
            }
        }
        
        return details;
    }
    
    private double calculateMismatchPercentage(String sourceValue, String targetValue) {
        if (sourceValue == null && targetValue == null) {
            return 0.0;
        }
        if (sourceValue == null || targetValue == null) {
            return 100.0;
        }
        
        // Simple calculation: if values are completely different, 100% mismatch
        // If they are similar, calculate based on character differences
        if (sourceValue.equals(targetValue)) {
            return 0.0;
        }
        
        // Calculate Levenshtein distance-based similarity
        int maxLength = Math.max(sourceValue.length(), targetValue.length());
        if (maxLength == 0) {
            return 0.0;
        }
        
        int distance = levenshteinDistance(sourceValue, targetValue);
        return (double) distance / maxLength * 100.0;
    }
    
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    private void validateConfig() {
        if (config.getKeyColumn() == null || config.getKeyColumn().trim().isEmpty()) {
            logger.warn("No key column specified. Reconciliation may not work as expected.");
        }
        
        if (config.getBatchSize() <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
    }
}
