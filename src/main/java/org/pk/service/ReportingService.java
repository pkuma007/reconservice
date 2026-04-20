package org.pk.service;

import org.pk.model.MismatchDetail;
import org.pk.model.ReconciliationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReportingService {
    private static final Logger logger = LoggerFactory.getLogger(ReportingService.class);
    
    public void generateReport(ReconciliationResult result, String reportFilePath) throws IOException {
        logger.info("Generating reconciliation report to: {}", reportFilePath);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFilePath))) {
            writeHeader(writer);
            writeSummary(writer, result);
            writeDetailedMismatches(writer, result);
            writeFooter(writer);
        }
        
        logger.info("Report generated successfully: {}", reportFilePath);
    }
    
    public void printConsoleReport(ReconciliationResult result) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CSV RECONCILIATION REPORT");
        System.out.println("=".repeat(80));
        
        System.out.println("\nSUMMARY:");
        System.out.println("--------");
        System.out.printf("Source Records:     %,d%n", result.getSourceRecordCount());
        System.out.printf("Target Records:     %,d%n", result.getTargetRecordCount());
        System.out.printf("Records Compared:   %,d%n", result.getRecordsCompared());
        System.out.printf("Matching Records:   %,d (%.2f%%)%n", 
                         result.getMatchingRecords(), result.getMatchPercentage());
        System.out.printf("Missing in Target:  %,d%n", result.getMissingInTarget());
        System.out.printf("Extra in Target:    %,d%n", result.getExtraInTarget());
        System.out.printf("Mismatched Records: %,d%n", result.getMismatchedRecords());
        System.out.printf("Processing Time:     %,d ms%n", result.getProcessingTimeMs());
        System.out.println("\nBREAKDOWN:");
        System.out.println("----------");
        System.out.println("Matching = Exact match on all fields");
        System.out.println("Mismatched = Same key exists but values differ");
        System.out.println("Missing = Key exists in source but not target");
        System.out.println("Extra = Key exists in target but not source");
        
        System.out.println("\nSTATUS:");
        System.out.println("------");
        if (result.isPerfectMatch()) {
            System.out.println("✅ PERFECT MATCH - All records match perfectly!");
        } else {
            System.out.println("❌ MISMATCHES FOUND - Review details below");
        }
        
        if (!result.getMismatchDetails().isEmpty()) {
            System.out.println("\nDETAILED MISMATCHES:");
            System.out.println("--------------------");
            System.out.println("Column Name    | Source Value         | Target Value         | Mismatch %");
            System.out.println("---------------+---------------------+---------------------+------------");
            
            int maxDetails = Math.min(10, result.getMismatchDetails().size());
            for (int i = 0; i < maxDetails; i++) {
                MismatchDetail detail = result.getMismatchDetails().get(i);
                System.out.printf("%-14s | %-19s | %-19s | %.2f%%%n",
                    detail.getColumnName() != null ? detail.getColumnName() : "N/A",
                    detail.getSourceValue() != null ? detail.getSourceValue() : "N/A",
                    detail.getTargetValue() != null ? detail.getTargetValue() : "N/A",
                    detail.getMismatchPercentage());
            }
            
            if (result.getMismatchDetails().size() > maxDetails) {
                System.out.printf("... and %d more mismatches (see full report for details)%n",
                                 result.getMismatchDetails().size() - maxDetails);
            }
        }
        
        System.out.println("\n" + "=".repeat(80));
    }
    
    private void writeHeader(PrintWriter writer) {
        writer.println("CSV RECONCILIATION REPORT");
        writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        writer.println("=".repeat(80));
        writer.println();
    }
    
    private void writeSummary(PrintWriter writer, ReconciliationResult result) {
        writer.println("EXECUTIVE SUMMARY");
        writer.println("----------------");
        writer.printf("Source File Record Count:     %,d%n", result.getSourceRecordCount());
        writer.printf("Target File Record Count:     %,d%n", result.getTargetRecordCount());
        writer.printf("Records Compared:             %,d%n", result.getRecordsCompared());
        writer.printf("Total Matching Records:      %,d%n", result.getMatchingRecords());
        writer.printf("Match Percentage:             %.2f%%%n", result.getMatchPercentage());
        writer.printf("Records Missing in Target:   %,d%n", result.getMissingInTarget());
        writer.printf("Extra Records in Target:     %,d%n", result.getExtraInTarget());
        writer.printf("Records with Data Mismatches: %,d%n", result.getMismatchedRecords());
        writer.printf("Total Processing Time:        %,d ms%n", result.getProcessingTimeMs());
        writer.println();
        
        writer.println("RECONCILIATION BREAKDOWN");
        writer.println("-------------------------");
        writer.println("Matching = Exact match on all fields");
        writer.println("Mismatched = Same key exists but values differ");
        writer.println("Missing = Key exists in source but not target");
        writer.println("Extra = Key exists in target but not source");
        writer.println();
        
        writer.println("RECONCILIATION CONFIGURATION");
        writer.println("--------------------------");
        writer.printf("Key Column Used:             %s%n", result.getKeyColumn() != null ? result.getKeyColumn() : "Not specified");
        writer.printf("Delimiter:                  %s%n", result.getDelimiter() != null ? result.getDelimiter() : "Default (,)");
        writer.printf("Headers Present:              %s%n", result.isHasHeaders() ? "Yes" : "No");
        writer.println();
        writer.println("RECONCILIATION STATUS");
        writer.println("--------------------");
        if (result.isPerfectMatch()) {
            writer.println("✅ PERFECT MATCH - All records match perfectly!");
        } else {
            writer.println("❌ MISMATCHES DETECTED - Requires attention");
            
            if (result.getMissingInTarget() > 0) {
                writer.printf("⚠️  %,d records are missing in target file%n", result.getMissingInTarget());
            }
            if (result.getExtraInTarget() > 0) {
                writer.printf("⚠️  %,d extra records found in target file%n", result.getExtraInTarget());
            }
            if (result.getMismatchedRecords() > 0) {
                writer.printf("⚠️  %,d records have data mismatches%n", result.getMismatchedRecords());
            }
        }
        writer.println();
    }
    
    private void writeDetailedMismatches(PrintWriter writer, ReconciliationResult result) {
        if (result.getMismatchDetails().isEmpty()) {
            writer.println("DETAILED MISMATCHES");
            writer.println("------------------");
            writer.println("No mismatches found.");
            writer.println();
            return;
        }
        
        writer.println("DETAILED MISMATCHES");
        writer.println("------------------");
        writer.printf("Total Mismatches: %,d%n", result.getMismatchDetails().size());
        writer.println();
        writer.println("Column Name    | Source Value         | Target Value         | Mismatch %");
        writer.println("---------------+---------------------+---------------------+------------");
        
        for (MismatchDetail detail : result.getMismatchDetails()) {
            String colName = (detail.getColumnName() != null ? detail.getColumnName() : "N/A").substring(0, Math.min(14, detail.getColumnName() != null ? detail.getColumnName().length() : 2));
            String srcValue = (detail.getSourceValue() != null ? detail.getSourceValue() : "N/A").substring(0, Math.min(20, detail.getSourceValue() != null ? detail.getSourceValue().length() : 2));
            String tgtValue = (detail.getTargetValue() != null ? detail.getTargetValue() : "N/A").substring(0, Math.min(20, detail.getTargetValue() != null ? detail.getTargetValue().length() : 2));
            String mismatchPct = String.format("%.2f%%", detail.getMismatchPercentage());
            
            writer.printf("%-14s | %-19s | %-19s | %s%n", 
                detail.getColumnName() != null ? detail.getColumnName() : "N/A",
                detail.getSourceValue() != null ? detail.getSourceValue() : "N/A",
                detail.getTargetValue() != null ? detail.getTargetValue() : "N/A",
                mismatchPct);
        }
        writer.println();
    }
    
    private void writeFooter(PrintWriter writer) {
        writer.println("=".repeat(80));
        writer.println("END OF REPORT");
        writer.println("Generated by CSV Reconciliation Service");
        writer.println("=".repeat(80));
    }
    
    public String generateJsonReport(ReconciliationResult result) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"timestamp\": \"").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        json.append("  \"summary\": {\n");
        json.append("    \"sourceRecordCount\": ").append(result.getSourceRecordCount()).append(",\n");
        json.append("    \"targetRecordCount\": ").append(result.getTargetRecordCount()).append(",\n");
        json.append("    \"recordsCompared\": ").append(result.getRecordsCompared()).append(",\n");
        json.append("    \"matchingRecords\": ").append(result.getMatchingRecords()).append(",\n");
        json.append("    \"matchPercentage\": ").append(result.getMatchPercentage()).append(",\n");
        json.append("    \"missingInTarget\": ").append(result.getMissingInTarget()).append(",\n");
        json.append("    \"extraInTarget\": ").append(result.getExtraInTarget()).append(",\n");
        json.append("    \"mismatchedRecords\": ").append(result.getMismatchedRecords()).append(",\n");
        json.append("    \"processingTimeMs\": ").append(result.getProcessingTimeMs()).append(",\n");
        json.append("    \"perfectMatch\": ").append(result.isPerfectMatch()).append("\n");
        json.append("  },\n");
        json.append("  \"mismatches\": [\n");
        
        for (int i = 0; i < result.getMismatchDetails().size(); i++) {
            MismatchDetail detail = result.getMismatchDetails().get(i);
            json.append("    {\n");
            json.append("      \"columnName\": \"").append(escapeJson(detail.getColumnName())).append("\",\n");
            json.append("      \"sourceValue\": \"").append(escapeJson(detail.getSourceValue())).append("\",\n");
            json.append("      \"targetValue\": \"").append(escapeJson(detail.getTargetValue())).append("\",\n");
            json.append("      \"mismatchPercentage\": ").append(detail.getMismatchPercentage()).append("\n");
            json.append("    }");
            if (i < result.getMismatchDetails().size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("  ]\n");
        json.append("}");
        
        return json.toString();
    }
    
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
