package org.pk.service;

import org.pk.reader.StreamingCsvReader;
import org.pk.config.ReconciliationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class KeyAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(KeyAnalyzer.class);
    
    public static KeyAnalysisResult analyzeFiles(String sourceFile, String targetFile, ReconciliationConfig config) throws IOException {
        logger.info("Analyzing files to determine optimal reconciliation key");
        
        StreamingCsvReader reader = new StreamingCsvReader(config);
        
        // Sample records from both files (increased sample size for better duplicate detection)
        List<Map<String, String>> sourceSample = reader.readSampleRecords(sourceFile, 500);
        List<Map<String, String>> targetSample = reader.readSampleRecords(targetFile, 500);
        
        if (sourceSample.isEmpty() || targetSample.isEmpty()) {
            return new KeyAnalysisResult("No records found in files", null, null);
        }
        
        // Get all column names from both files
        Set<String> sourceColumns = getAllColumns(sourceSample);
        Set<String> targetColumns = getAllColumns(targetSample);
        Set<String> commonColumns = new HashSet<>(sourceColumns);
        commonColumns.retainAll(targetColumns);
        
        // Analyze each common column for uniqueness
        Map<String, ColumnAnalysis> columnAnalyses = new HashMap<>();
        
        for (String column : commonColumns) {
            ColumnAnalysis analysis = analyzeColumn(column, sourceSample, targetSample);
            columnAnalyses.put(column, analysis);
        }
        
        // Find best column based on uniqueness score
        String recommendedKey = findBestKeyColumn(columnAnalyses);
        
        // Check if the best column has duplicates and try composite keys
        if (recommendedKey != null && columnAnalyses.containsKey(recommendedKey)) {
            ColumnAnalysis bestAnalysis = columnAnalyses.get(recommendedKey);
            if (bestAnalysis.isHasSourceDuplicates() || bestAnalysis.isHasTargetDuplicates()) {
                logger.info("Best key column '{}' has duplicates, attempting composite key detection", recommendedKey);
                List<String> compositeKey = findCompositeKey(columnAnalyses, sourceSample, targetSample);
                if (compositeKey != null && !compositeKey.isEmpty()) {
                    logger.info("Found composite key: {}", compositeKey);
                    return new KeyAnalysisResult("Analysis completed - Using composite key", 
                        String.join(",", compositeKey), columnAnalyses, compositeKey);
                }
            }
        }
        
        return new KeyAnalysisResult("Analysis completed", recommendedKey, columnAnalyses);
    }
    
    private static Set<String> getAllColumns(List<Map<String, String>> records) {
        Set<String> columns = new HashSet<>();
        for (Map<String, String> record : records) {
            columns.addAll(record.keySet());
        }
        return columns;
    }
    
    private static ColumnAnalysis analyzeColumn(String columnName, List<Map<String, String>> sourceRecords, List<Map<String, String>> targetRecords) {
        // Extract values from source
        Set<String> sourceValues = new HashSet<>();
        for (Map<String, String> record : sourceRecords) {
            String value = record.get(columnName);
            if (value != null && !value.trim().isEmpty()) {
                sourceValues.add(value.trim());
            }
        }
        
        // Extract values from target
        Set<String> targetValues = new HashSet<>();
        for (Map<String, String> record : targetRecords) {
            String value = record.get(columnName);
            if (value != null && !value.trim().isEmpty()) {
                targetValues.add(value.trim());
            }
        }
        
        // Calculate uniqueness metrics
        int sourceUnique = sourceValues.size();
        int targetUnique = targetValues.size();
        int totalRecords = Math.max(sourceRecords.size(), targetRecords.size());
        
        // Check for duplicates
        boolean hasSourceDuplicates = sourceValues.size() < sourceRecords.size();
        boolean hasTargetDuplicates = targetValues.size() < targetRecords.size();
        
        // Calculate uniqueness percentage
        double uniquenessPercentage = (double) (sourceUnique + targetUnique) / (2 * totalRecords) * 100;
        
        return new ColumnAnalysis(columnName, sourceUnique, targetUnique, 
                               hasSourceDuplicates, hasTargetDuplicates, uniquenessPercentage);
    }
    
    private static String findBestKeyColumn(Map<String, ColumnAnalysis> columnAnalyses) {
        if (columnAnalyses.isEmpty()) {
            return null;
        }
        
        // Define column priority: ID-like columns first, then others
        List<String> priorityColumns = Arrays.asList("id", "ID", "Id", "user_id", "user_id", "customer_id", 
                                                   "col_A", "col_0", "key", "Key", "KEY");
        
        // Sort columns by multiple criteria:
        // 1. Priority (ID-like columns first)
        // 2. Uniqueness percentage (higher is better)
        // 3. No duplicates
        List<Map.Entry<String, ColumnAnalysis>> sortedEntries = columnAnalyses.entrySet().stream()
            .sorted((e1, e2) -> {
                String col1 = e1.getKey();
                String col2 = e2.getKey();
                ColumnAnalysis analysis1 = e1.getValue();
                ColumnAnalysis analysis2 = e2.getValue();
                
                // Check if columns are priority columns
                boolean isPriority1 = priorityColumns.stream().anyMatch(col1::contains);
                boolean isPriority2 = priorityColumns.stream().anyMatch(col2::contains);
                
                // Priority columns first
                if (isPriority1 && !isPriority2) return -1;
                if (!isPriority1 && isPriority2) return 1;
                
                // Then by uniqueness percentage
                int uniqCompare = Double.compare(analysis2.getUniquenessPercentage(), analysis1.getUniquenessPercentage());
                if (uniqCompare != 0) return uniqCompare;
                
                // Then by no duplicates
                boolean hasDuplicates1 = analysis1.isHasSourceDuplicates() || analysis1.isHasTargetDuplicates();
                boolean hasDuplicates2 = analysis2.isHasSourceDuplicates() || analysis2.isHasTargetDuplicates();
                if (!hasDuplicates1 && hasDuplicates2) return -1;
                if (hasDuplicates1 && !hasDuplicates2) return 1;
                
                // Finally by column name order
                return col1.compareTo(col2);
            })
            .collect(Collectors.toList());
        
        // Prefer columns with high uniqueness and no duplicates
        for (Map.Entry<String, ColumnAnalysis> entry : sortedEntries) {
            ColumnAnalysis analysis = entry.getValue();
            if (!analysis.isHasSourceDuplicates() && !analysis.isHasTargetDuplicates() 
                && analysis.getUniquenessPercentage() >= 80.0) {
                return entry.getKey();
            }
        }
        
        // If no perfect column found, return the first priority column or highest uniqueness
        for (Map.Entry<String, ColumnAnalysis> entry : sortedEntries) {
            String columnName = entry.getKey();
            if (priorityColumns.stream().anyMatch(columnName::contains)) {
                return columnName;
            }
        }
        
        return sortedEntries.get(0).getKey();
    }
    
    private static List<String> findCompositeKey(Map<String, ColumnAnalysis> columnAnalyses, 
                                                  List<Map<String, String>> sourceSample, 
                                                  List<Map<String, String>> targetSample) {
        logger.info("Attempting to find composite key from {} columns", columnAnalyses.size());
        
        // Sort columns by uniqueness percentage (highest first)
        List<String> sortedColumns = columnAnalyses.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue().getUniquenessPercentage(), 
                                                 e1.getValue().getUniquenessPercentage()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // Try combinations of 2-3 columns
        for (int comboSize = 2; comboSize <= Math.min(3, sortedColumns.size()); comboSize++) {
            List<List<String>> combinations = generateCombinations(sortedColumns, comboSize);
            
            for (List<String> combination : combinations) {
                if (isUniqueCompositeKey(combination, sourceSample, targetSample)) {
                    logger.info("Found unique composite key: {}", combination);
                    return combination;
                }
            }
        }
        
        logger.warn("No suitable composite key found");
        return null;
    }
    
    private static List<List<String>> generateCombinations(List<String> columns, int size) {
        List<List<String>> combinations = new ArrayList<>();
        generateCombinationsHelper(columns, size, 0, new ArrayList<>(), combinations);
        return combinations;
    }
    
    private static void generateCombinationsHelper(List<String> columns, int size, int start, 
                                                   List<String> current, List<List<String>> combinations) {
        if (current.size() == size) {
            combinations.add(new ArrayList<>(current));
            return;
        }
        
        for (int i = start; i < columns.size(); i++) {
            current.add(columns.get(i));
            generateCombinationsHelper(columns, size, i + 1, current, combinations);
            current.remove(current.size() - 1);
        }
    }
    
    private static boolean isUniqueCompositeKey(List<String> columns, 
                                               List<Map<String, String>> sourceSample,
                                               List<Map<String, String>> targetSample) {
        // Check source uniqueness
        Set<String> sourceCompositeKeys = new HashSet<>();
        for (Map<String, String> record : sourceSample) {
            String compositeKey = buildCompositeKey(columns, record);
            if (compositeKey != null && !compositeKey.trim().isEmpty()) {
                if (sourceCompositeKeys.contains(compositeKey)) {
                    return false; // Duplicate found
                }
                sourceCompositeKeys.add(compositeKey);
            }
        }
        
        // Check target uniqueness
        Set<String> targetCompositeKeys = new HashSet<>();
        for (Map<String, String> record : targetSample) {
            String compositeKey = buildCompositeKey(columns, record);
            if (compositeKey != null && !compositeKey.trim().isEmpty()) {
                if (targetCompositeKeys.contains(compositeKey)) {
                    return false; // Duplicate found
                }
                targetCompositeKeys.add(compositeKey);
            }
        }
        
        // Check if all records have valid composite keys
        return sourceCompositeKeys.size() == sourceSample.size() && 
               targetCompositeKeys.size() == targetSample.size();
    }
    
    private static String buildCompositeKey(List<String> columns, Map<String, String> record) {
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            String value = record.get(column);
            if (value == null) return null;
            
            if (i > 0) key.append("|");
            key.append(value.trim());
        }
        return key.toString();
    }
    
    public static class KeyAnalysisResult {
        private final String message;
        private final String recommendedKey;
        private final Map<String, ColumnAnalysis> columnAnalyses;
        private final List<String> compositeKeyColumns;
        
        public KeyAnalysisResult(String message, String recommendedKey, Map<String, ColumnAnalysis> columnAnalyses) {
            this.message = message;
            this.recommendedKey = recommendedKey;
            this.columnAnalyses = columnAnalyses;
            this.compositeKeyColumns = null;
        }
        
        public KeyAnalysisResult(String message, String recommendedKey, Map<String, ColumnAnalysis> columnAnalyses, List<String> compositeKeyColumns) {
            this.message = message;
            this.recommendedKey = recommendedKey;
            this.columnAnalyses = columnAnalyses;
            this.compositeKeyColumns = compositeKeyColumns;
        }
        
        public String getMessage() { return message; }
        public String getRecommendedKey() { return recommendedKey; }
        public Map<String, ColumnAnalysis> getColumnAnalyses() { return columnAnalyses; }
        public List<String> getCompositeKeyColumns() { return compositeKeyColumns; }
        public boolean isCompositeKey() { return compositeKeyColumns != null && !compositeKeyColumns.isEmpty(); }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Key Analysis Result:\n");
            sb.append("Message: ").append(message).append("\n");
            
            if (recommendedKey != null) {
                sb.append("Recommended Key Column: ").append(recommendedKey).append("\n");
            }
            
            if (columnAnalyses != null && !columnAnalyses.isEmpty()) {
                sb.append("Column Analysis:\n");
                columnAnalyses.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        ColumnAnalysis analysis = entry.getValue();
                        sb.append("  ").append(entry.getKey()).append(": ")
                          .append("Uniqueness: ").append(String.format("%.1f%%", analysis.getUniquenessPercentage()))
                          .append(", Source Duplicates: ").append(analysis.isHasSourceDuplicates() ? "Yes" : "No")
                          .append(", Target Duplicates: ").append(analysis.isHasTargetDuplicates() ? "Yes" : "No")
                          .append("\n");
                    });
            }
            
            return sb.toString();
        }
    }
    
    public static class ColumnAnalysis {
        private final String columnName;
        private final int sourceUniqueCount;
        private final int targetUniqueCount;
        private final boolean hasSourceDuplicates;
        private final boolean hasTargetDuplicates;
        private final double uniquenessPercentage;
        
        public ColumnAnalysis(String columnName, int sourceUniqueCount, int targetUniqueCount,
                         boolean hasSourceDuplicates, boolean hasTargetDuplicates, double uniquenessPercentage) {
            this.columnName = columnName;
            this.sourceUniqueCount = sourceUniqueCount;
            this.targetUniqueCount = targetUniqueCount;
            this.hasSourceDuplicates = hasSourceDuplicates;
            this.hasTargetDuplicates = hasTargetDuplicates;
            this.uniquenessPercentage = uniquenessPercentage;
        }
        
        public String getColumnName() { return columnName; }
        public int getSourceUniqueCount() { return sourceUniqueCount; }
        public int getTargetUniqueCount() { return targetUniqueCount; }
        public boolean isHasSourceDuplicates() { return hasSourceDuplicates; }
        public boolean isHasTargetDuplicates() { return hasTargetDuplicates; }
        public double getUniquenessPercentage() { return uniquenessPercentage; }
    }
}
