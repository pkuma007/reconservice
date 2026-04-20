package org.pk.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pk.config.ReconciliationConfig;
import org.pk.model.ReconciliationResult;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationServiceTest {
    
    @TempDir
    Path tempDir;
    
    private ReconciliationService reconciliationService;
    private ReconciliationConfig config;
    
    @BeforeEach
    void setUp() {
        config = ReconciliationConfig.builder()
            .delimiter(',')
            .hasHeader(true)
            .keyColumn("id")
            .ignoreCase(false)
            .trimWhitespace(true)
            .batchSize(100)
            .build();
        
        reconciliationService = new ReconciliationService(config);
    }
    
    @Test
    void testPerfectMatch() throws IOException {
        String sourceFile = createTestFile("source.csv", 
            "id,name,age\n" +
            "1,John,25\n" +
            "2,Jane,30\n" +
            "3,Bob,35");
        
        String targetFile = createTestFile("target.csv",
            "id,name,age\n" +
            "1,John,25\n" +
            "2,Jane,30\n" +
            "3,Bob,35");
        
        ReconciliationResult result = reconciliationService.reconcile(sourceFile, targetFile);
        
        assertThat(result.isPerfectMatch()).isTrue();
        assertThat(result.getSourceRecordCount()).isEqualTo(3);
        assertThat(result.getTargetRecordCount()).isEqualTo(3);
        assertThat(result.getMatchingRecords()).isEqualTo(3);
        assertThat(result.getMissingInTarget()).isEqualTo(0);
        assertThat(result.getExtraInTarget()).isEqualTo(0);
        assertThat(result.getMismatchedRecords()).isEqualTo(0);
        assertThat(result.getMatchPercentage()).isEqualTo(100.0);
    }
    
    @Test
    void testMissingInTarget() throws IOException {
        String sourceFile = createTestFile("source.csv",
            "id,name,age\n" +
            "1,John,25\n" +
            "2,Jane,30\n" +
            "3,Bob,35");
        
        String targetFile = createTestFile("target.csv",
            "id,name,age\n" +
            "1,John,25\n" +
            "3,Bob,35");
        
        ReconciliationResult result = reconciliationService.reconcile(sourceFile, targetFile);
        
        assertThat(result.isPerfectMatch()).isFalse();
        assertThat(result.getSourceRecordCount()).isEqualTo(3);
        assertThat(result.getTargetRecordCount()).isEqualTo(2);
        assertThat(result.getMatchingRecords()).isEqualTo(2);
        assertThat(result.getMissingInTarget()).isEqualTo(1);
        assertThat(result.getExtraInTarget()).isEqualTo(0);
        assertThat(result.getMismatchedRecords()).isEqualTo(0);
        assertThat(result.getMismatchDetails()).hasSize(1);
        assertThat(result.getMismatchDetails().get(0).toString()).contains("2");
    }
    
    @Test
    void testExtraInTarget() throws IOException {
        String sourceFile = createTestFile("source.csv",
            "id,name,age\n" +
            "1,John,25\n" +
            "2,Jane,30");
        
        String targetFile = createTestFile("target.csv",
            "id,name,age\n" +
            "1,John,25\n" +
            "2,Jane,30\n" +
            "3,Bob,35");
        
        ReconciliationResult result = reconciliationService.reconcile(sourceFile, targetFile);
        
        assertThat(result.isPerfectMatch()).isFalse();
        assertThat(result.getSourceRecordCount()).isEqualTo(2);
        assertThat(result.getTargetRecordCount()).isEqualTo(3);
        assertThat(result.getMatchingRecords()).isEqualTo(2);
        assertThat(result.getMissingInTarget()).isEqualTo(0);
        assertThat(result.getExtraInTarget()).isEqualTo(1);
        assertThat(result.getMismatchedRecords()).isEqualTo(0);
        assertThat(result.getMismatchDetails()).hasSize(1);
        assertThat(result.getMismatchDetails().get(0).toString()).contains("3");
    }
    
    @Test
    void testDataMismatches() throws IOException {
        String sourceFile = createTestFile("source.csv",
            "id,name,age\n" +
            "1,John,25\n" +
            "2,Jane,30\n" +
            "3,Bob,35");
        
        String targetFile = createTestFile("target.csv",
            "id,name,age\n" +
            "1,John,25\n" +
            "2,Jane,31\n" +
            "3,Robert,35");
        
        ReconciliationResult result = reconciliationService.reconcile(sourceFile, targetFile);
        
        assertThat(result.isPerfectMatch()).isFalse();
        assertThat(result.getSourceRecordCount()).isEqualTo(3);
        assertThat(result.getTargetRecordCount()).isEqualTo(3);
        assertThat(result.getMatchingRecords()).isEqualTo(1);
        assertThat(result.getMissingInTarget()).isEqualTo(0);
        assertThat(result.getExtraInTarget()).isEqualTo(0);
        assertThat(result.getMismatchedRecords()).isEqualTo(2);
        assertThat(result.getMismatchDetails()).hasSize(2);
    }
    
    @Test
    void testSemicolonDelimiter() throws IOException {
        ReconciliationConfig semicolonConfig = ReconciliationConfig.builder()
            .delimiter(';')
            .hasHeader(true)
            .keyColumn("id")
            .build();
        
        ReconciliationService service = new ReconciliationService(semicolonConfig);
        
        String sourceFile = createTestFile("source.csv",
            "id;name;age\n" +
            "1;John;25\n" +
            "2;Jane;30");
        
        String targetFile = createTestFile("target.csv",
            "id;name;age\n" +
            "1;John;25\n" +
            "2;Jane;30");
        
        ReconciliationResult result = service.reconcile(sourceFile, targetFile);
        
        assertThat(result.isPerfectMatch()).isTrue();
        assertThat(result.getMatchingRecords()).isEqualTo(2);
    }
    
    @Test
    void testNoHeader() throws IOException {
        ReconciliationConfig noHeaderConfig = ReconciliationConfig.builder()
            .delimiter(',')
            .hasHeader(false)
            .keyColumn(null)
            .build();
        
        ReconciliationService service = new ReconciliationService(noHeaderConfig);
        
        String sourceFile = createTestFile("source.csv",
            "1,John,25\n" +
            "2,Jane,30");
        
        String targetFile = createTestFile("target.csv",
            "1,John,25\n" +
            "2,Jane,30");
        
        ReconciliationResult result = service.reconcile(sourceFile, targetFile);
        
        assertThat(result.getMatchingRecords()).isEqualTo(2);
    }
    
    @Test
    void testIgnoreCase() throws IOException {
        ReconciliationConfig caseConfig = ReconciliationConfig.builder()
            .delimiter(',')
            .hasHeader(true)
            .keyColumn("id")
            .ignoreCase(true)
            .build();
        
        ReconciliationService service = new ReconciliationService(caseConfig);
        
        String sourceFile = createTestFile("source.csv",
            "id,name,age\n" +
            "1,John,25\n" +
            "2,JANE,30");
        
        String targetFile = createTestFile("target.csv",
            "id,name,age\n" +
            "1,john,25\n" +
            "2,jane,30");
        
        ReconciliationResult result = service.reconcile(sourceFile, targetFile);
        
        assertThat(result.isPerfectMatch()).isTrue();
        assertThat(result.getMatchingRecords()).isEqualTo(2);
    }
    
    private String createTestFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        try (FileWriter writer = new FileWriter(file.toFile())) {
            writer.write(content);
        }
        return file.toString();
    }
}
