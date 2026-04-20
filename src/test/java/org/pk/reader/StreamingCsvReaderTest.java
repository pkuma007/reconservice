package org.pk.reader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pk.config.ReconciliationConfig;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StreamingCsvReaderTest {
    
    @TempDir
    Path tempDir;
    
    private StreamingCsvReader reader;
    private ReconciliationConfig config;
    
    @BeforeEach
    void setUp() {
        config = ReconciliationConfig.builder()
            .delimiter(',')
            .hasHeader(true)
            .keyColumn("id")
            .build();
        
        reader = new StreamingCsvReader(config);
    }
    
    @Test
    void testProcessFileWithHeaders() throws IOException {
        String testFile = createTestFile("test.csv",
            "id,name,age\n" +
            "1,John,25\n" +
            "2,Jane,30\n" +
            "3,Bob,35");
        
        List<Map<String, String>> records = new ArrayList<>();
        reader.processFile(testFile, records::add);
        
        assertThat(records).hasSize(3);
        
        Map<String, String> firstRecord = records.get(0);
        assertThat(firstRecord.get("id")).isEqualTo("1");
        assertThat(firstRecord.get("name")).isEqualTo("John");
        assertThat(firstRecord.get("age")).isEqualTo("25");
        
        Map<String, String> secondRecord = records.get(1);
        assertThat(secondRecord.get("id")).isEqualTo("2");
        assertThat(secondRecord.get("name")).isEqualTo("Jane");
        assertThat(secondRecord.get("age")).isEqualTo("30");
    }
    
    @Test
    void testProcessFileWithoutHeaders() throws IOException {
        ReconciliationConfig noHeaderConfig = ReconciliationConfig.builder()
            .delimiter(',')
            .hasHeader(false)
            .build();
        
        StreamingCsvReader noHeaderReader = new StreamingCsvReader(noHeaderConfig);
        
        String testFile = createTestFile("test.csv",
            "1,John,25\n" +
            "2,Jane,30");
        
        List<Map<String, String>> records = new ArrayList<>();
        noHeaderReader.processFile(testFile, records::add);
        
        assertThat(records).hasSize(2);
        
        Map<String, String> firstRecord = records.get(0);
        assertThat(firstRecord.get("0")).isEqualTo("1");
        assertThat(firstRecord.get("1")).isEqualTo("John");
        assertThat(firstRecord.get("2")).isEqualTo("25");
    }
    
    @Test
    void testSemicolonDelimiter() throws IOException {
        ReconciliationConfig semicolonConfig = ReconciliationConfig.builder()
            .delimiter(';')
            .hasHeader(true)
            .build();
        
        StreamingCsvReader semicolonReader = new StreamingCsvReader(semicolonConfig);
        
        String testFile = createTestFile("test.csv",
            "id;name;age\n" +
            "1;John;25\n" +
            "2;Jane;30");
        
        List<Map<String, String>> records = new ArrayList<>();
        semicolonReader.processFile(testFile, records::add);
        
        assertThat(records).hasSize(2);
        
        Map<String, String> firstRecord = records.get(0);
        assertThat(firstRecord.get("id")).isEqualTo("1");
        assertThat(firstRecord.get("name")).isEqualTo("John");
        assertThat(firstRecord.get("age")).isEqualTo("25");
    }
    
    @Test
    void testTrimWhitespace() throws IOException {
        ReconciliationConfig trimConfig = ReconciliationConfig.builder()
            .delimiter(',')
            .hasHeader(true)
            .trimWhitespace(true)
            .build();
        
        StreamingCsvReader trimReader = new StreamingCsvReader(trimConfig);
        
        String testFile = createTestFile("test.csv",
            "id,name,age\n" +
            " 1 , John , 25 \n" +
            "2,Jane,30");
        
        List<Map<String, String>> records = new ArrayList<>();
        trimReader.processFile(testFile, records::add);
        
        assertThat(records).hasSize(2);
        
        Map<String, String> firstRecord = records.get(0);
        assertThat(firstRecord.get("id")).isEqualTo("1");
        assertThat(firstRecord.get("name")).isEqualTo("John");
        assertThat(firstRecord.get("age")).isEqualTo("25");
    }
    
    @Test
    void testCountRecords() throws IOException {
        String testFile = createTestFile("test.csv",
            "id,name,age\n" +
            "1,John,25\n" +
            "2,Jane,30\n" +
            "3,Bob,35");
        
        long count = reader.countRecords(testFile);
        assertThat(count).isEqualTo(3);
    }
    
    @Test
    void testReadSampleRecords() throws IOException {
        String testFile = createTestFile("test.csv",
            "id,name,age\n" +
            "1,John,25\n" +
            "2,Jane,30\n" +
            "3,Bob,35\n" +
            "4,Alice,28");
        
        List<Map<String, String>> sample = reader.readSampleRecords(testFile, 2);
        assertThat(sample).hasSize(2);
        
        Map<String, String> firstRecord = sample.get(0);
        assertThat(firstRecord.get("id")).isEqualTo("1");
        assertThat(firstRecord.get("name")).isEqualTo("John");
    }
    
    @Test
    void testGetHeaders() throws IOException {
        String testFile = createTestFile("test.csv",
            "id,name,age\n" +
            "1,John,25\n" +
            "2,Jane,30");
        
        Map<String, String> headers = reader.getHeaders(testFile);
        assertThat(headers).containsKeys("id", "name", "age");
    }
    
    private String createTestFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        try (FileWriter writer = new FileWriter(file.toFile())) {
            writer.write(content);
        }
        return file.toString();
    }
}
