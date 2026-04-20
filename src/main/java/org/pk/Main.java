package org.pk;

import org.pk.config.ReconciliationConfig;
import org.pk.model.ReconciliationResult;
import org.pk.service.KeyAnalyzer;
import org.pk.service.ReconciliationService;
import org.pk.service.ReportingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }
        
        String sourceFile = args[0];
        String targetFile = args[1];
        String reportFile = "reconciliation_report.txt";
        char delimiter = ',';
        
        // Parse optional arguments
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-d=") || arg.startsWith("--delimiter=")) {
                String delimValue = arg.substring(arg.indexOf('=') + 1);
                if (delimValue.length() == 1) {
                    delimiter = delimValue.charAt(0);
                } else {
                    System.err.println("Error: Delimiter must be a single character");
                    System.exit(1);
                }
            } else if (!arg.startsWith("-")) {
                // Assume it's the report file
                reportFile = arg;
            }
        }
        
        try {
            // First analyze files to determine best key column
            ReconciliationConfig analysisConfig = ReconciliationConfig.builder()
                .delimiter(delimiter)
                .hasHeader(true)
                .ignoreCase(false)
                .trimWhitespace(true)
                .batchSize(100)
                .build();
            
            KeyAnalyzer.KeyAnalysisResult analysisResult = KeyAnalyzer.analyzeFiles(sourceFile, targetFile, analysisConfig);
            
            // Determine key column
            String keyColumn = analysisResult.getRecommendedKey();
            if (keyColumn == null) {
                System.out.println("Warning: Could not determine a suitable key column. Using 'id' as default.");
                keyColumn = "id";
            } else {
                if (analysisResult.isCompositeKey()) {
                    System.out.println("Recommended composite key columns: " + String.join(", ", analysisResult.getCompositeKeyColumns()));
                    keyColumn = String.join(",", analysisResult.getCompositeKeyColumns());
                } else {
                    System.out.println("Recommended key column: " + keyColumn);
                }
                System.out.println("Column analysis:");
                analysisResult.getColumnAnalyses().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        KeyAnalyzer.ColumnAnalysis analysis = entry.getValue();
                        System.out.printf("  %s: %.1f%% unique (%d/%d), duplicates: %s/%s%n",
                            analysis.getColumnName(), analysis.getUniquenessPercentage(),
                            analysis.getSourceUniqueCount(), analysis.getTargetUniqueCount(),
                            analysis.isHasSourceDuplicates() ? "Yes" : "No",
                            analysis.isHasTargetDuplicates() ? "Yes" : "No");
                    });
            }
            
            ReconciliationConfig config = ReconciliationConfig.builder()
                .delimiter(delimiter)
                .hasHeader(true)
                .keyColumn(keyColumn)
                .ignoreCase(false)
                .trimWhitespace(true)
                .batchSize(10000)
                .build();
            
            ReconciliationService reconciliationService = new ReconciliationService(config);
            ReportingService reportingService = new ReportingService();
            
            logger.info("Starting CSV reconciliation...");
            ReconciliationResult result = reconciliationService.reconcile(sourceFile, targetFile);
            
            reportingService.printConsoleReport(result);
            reportingService.generateReport(result, reportFile);
            
            logger.info("Reconciliation completed successfully!");
            
        } catch (IOException e) {
            logger.error("Error during reconciliation: {}", e.getMessage(), e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: java -jar reconservice.jar <source_file> <target_file> [report_file] [-d=<delimiter>]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  source_file    Path to the source CSV file");
        System.out.println("  target_file    Path to the target CSV file");
        System.out.println("  report_file    Path to the output report file (optional, default: reconciliation_report.txt)");
        System.out.println("  -d=<delimiter> or --delimiter=<delimiter>  Delimiter character (optional, default: comma ',')");
        System.out.println();
        System.out.println("Supported delimiters: comma (,), pipe (|), tilde (~), semicolon (;), tab (\\t), or any single character");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar reconservice.jar source.csv target.csv report.txt");
        System.out.println("  java -jar reconservice.jar source.csv target.csv -d=|");
        System.out.println("  java -jar reconservice.jar source.csv target.csv report.txt -d=~");
        System.out.println("  java -jar reconservice.jar source.csv target.csv --delimiter=;");
    }
}