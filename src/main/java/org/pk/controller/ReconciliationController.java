package org.pk.controller;

import org.pk.config.ReconciliationConfig;
import org.pk.model.ReconciliationResult;
import org.pk.service.KeyAnalyzer;
import org.pk.service.ReconciliationService;
import org.pk.service.ReportingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reconcile")
@CrossOrigin(origins = "*")
public class ReconciliationController {
    private static final Logger logger = LoggerFactory.getLogger(ReconciliationController.class);
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads";
    
    @PostMapping
    public ResponseEntity<?> reconcile(
            @RequestParam("sourceFile") MultipartFile sourceFile,
            @RequestParam("targetFile") MultipartFile targetFile,
            @RequestParam(value = "delimiter", defaultValue = ",") char delimiter,
            @RequestParam(value = "hasHeader", defaultValue = "true") boolean hasHeader,
            @RequestParam(value = "keyColumn", required = false) String keyColumn,
            @RequestParam(value = "ignoreCase", defaultValue = "false") boolean ignoreCase,
            @RequestParam(value = "trimWhitespace", defaultValue = "true") boolean trimWhitespace,
            @RequestParam(value = "advancedKeyConfig", defaultValue = "false") boolean advancedKeyConfig,
            @RequestParam(value = "sourceKeyColumns", required = false) String sourceKeyColumns,
            @RequestParam(value = "targetKeyColumns", required = false) String targetKeyColumns,
            @RequestParam(value = "concatenationSeparator", defaultValue = "_") String concatenationSeparator,
            @RequestParam(value = "columnMapping", required = false) String columnMapping) {
        
        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Save uploaded files
            String sourceFileName = UPLOAD_DIR + "/" + System.currentTimeMillis() + "_source_" + sourceFile.getOriginalFilename();
            String targetFileName = UPLOAD_DIR + "/" + System.currentTimeMillis() + "_target_" + targetFile.getOriginalFilename();
            
            sourceFile.transferTo(new File(sourceFileName));
            targetFile.transferTo(new File(targetFileName));
            
            logger.info("Key column parameter received: '{}'", keyColumn);
            
            // Track whether key was auto-detected
            boolean keyAutoDetected = (keyColumn == null || keyColumn.trim().isEmpty());
            
            // Always run KeyAnalyzer to check for duplicates and potentially use composite keys
            logger.info("Running KeyAnalyzer for duplicate detection and composite key analysis");
            ReconciliationConfig analysisConfig = ReconciliationConfig.builder()
                .delimiter(delimiter)
                .hasHeader(hasHeader)
                .ignoreCase(ignoreCase)
                .trimWhitespace(trimWhitespace)
                .batchSize(100)
                .build();
            
            KeyAnalyzer.KeyAnalysisResult analysisResult = KeyAnalyzer.analyzeFiles(sourceFileName, targetFileName, analysisConfig);
            
            // Determine key column based on analysis
            if (keyColumn == null || keyColumn.trim().isEmpty()) {
                logger.info("No key column specified, using auto-detection result");
                if (analysisResult.getRecommendedKey() != null) {
                    if (analysisResult.isCompositeKey()) {
                        keyColumn = String.join(",", analysisResult.getCompositeKeyColumns());
                        logger.info("Auto-detected composite key columns: {}", keyColumn);
                    } else {
                        keyColumn = analysisResult.getRecommendedKey();
                        logger.info("Auto-detected key column: {}", keyColumn);
                    }
                } else {
                    keyColumn = "id";
                    logger.warn("Could not auto-detect key column, using 'id' as default");
                }
            } else {
                // Check if provided key column has duplicates and try composite keys
                logger.info("Key column provided: {}, checking for duplicates", keyColumn);
                if (analysisResult.getColumnAnalyses() != null && 
                    analysisResult.getColumnAnalyses().containsKey(keyColumn)) {
                    KeyAnalyzer.ColumnAnalysis columnAnalysis = analysisResult.getColumnAnalyses().get(keyColumn);
                    if (columnAnalysis.isHasSourceDuplicates() || columnAnalysis.isHasTargetDuplicates()) {
                        logger.warn("Provided key column '{}' has duplicates, attempting composite key detection", keyColumn);
                        if (analysisResult.isCompositeKey()) {
                            keyColumn = String.join(",", analysisResult.getCompositeKeyColumns());
                            logger.info("Switching to composite key columns: {}", keyColumn);
                        } else {
                            logger.warn("No composite key found, will proceed with duplicates in key column");
                        }
                    } else {
                        logger.info("Provided key column '{}' has no duplicates, using as is", keyColumn);
                    }
                } else {
                    logger.warn("Provided key column '{}' not found in analysis results", keyColumn);
                }
            }
            
            // Perform reconciliation
            ReconciliationConfig.Builder configBuilder = ReconciliationConfig.builder()
                .delimiter(delimiter)
                .hasHeader(hasHeader)
                .keyColumn(keyColumn)
                .keyAutoDetected(keyAutoDetected)
                .ignoreCase(ignoreCase)
                .trimWhitespace(trimWhitespace)
                .batchSize(10000);
            
            // Set advanced key configuration if enabled
            if (advancedKeyConfig) {
                logger.info("Advanced key configuration enabled");
                if (sourceKeyColumns != null && !sourceKeyColumns.trim().isEmpty()) {
                    List<String> sourceCols = Arrays.stream(sourceKeyColumns.split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());
                    configBuilder.sourceKeyColumns(sourceCols);
                    logger.info("Source key columns: {}", sourceCols);
                }
                
                if (targetKeyColumns != null && !targetKeyColumns.trim().isEmpty()) {
                    List<String> targetCols = Arrays.stream(targetKeyColumns.split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());
                    configBuilder.targetKeyColumns(targetCols);
                    logger.info("Target key columns: {}", targetCols);
                }
                
                configBuilder.concatenationSeparator(concatenationSeparator);
                logger.info("Concatenation separator: {}", concatenationSeparator);
                
                if (columnMapping != null && !columnMapping.trim().isEmpty()) {
                    Map<String, String> mapping = Arrays.stream(columnMapping.split(","))
                        .map(String::trim)
                        .filter(s -> s.contains("="))
                        .collect(Collectors.toMap(
                            s -> s.split("=")[0].trim(),
                            s -> s.split("=")[1].trim()
                        ));
                    configBuilder.columnMapping(mapping);
                    logger.info("Column mapping: {}", mapping);
                }
            }
            
            ReconciliationConfig config = configBuilder.build();
            
            ReconciliationService reconciliationService = new ReconciliationService(config);
            ReconciliationResult result = reconciliationService.reconcile(sourceFileName, targetFileName);
            
            // Generate report
            String reportFileName = UPLOAD_DIR + "/" + System.currentTimeMillis() + "_report.txt";
            ReportingService reportingService = new ReportingService();
            reportingService.generateReport(result, reportFileName);
            
            // Return result as JSON
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", result);
            response.put("reportFile", reportFileName);
            response.put("keyColumn", keyColumn);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            logger.error("Error during reconciliation: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "CSV Reconciliation Service");
        return ResponseEntity.ok(response);
    }
}
