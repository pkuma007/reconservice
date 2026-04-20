package org.pk.reader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.pk.config.ReconciliationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class StreamingCsvReader {
    private static final Logger logger = LoggerFactory.getLogger(StreamingCsvReader.class);
    
    private final ReconciliationConfig config;
    
    public StreamingCsvReader(ReconciliationConfig config) {
        this.config = config;
    }
    
    public void processFile(String filePath, Consumer<Map<String, String>> recordProcessor) throws IOException {
        logger.info("Processing file: {}", filePath);
        
        CSVFormat.Builder builder = CSVFormat.DEFAULT
            .builder()
            .setDelimiter(config.getDelimiter())
            .setIgnoreHeaderCase(config.isIgnoreCase())
            .setTrim(config.isTrimWhitespace());
            
        if (config.isHasHeader()) {
            builder.setHeader();
        }
        
        CSVFormat format = builder.build();
        
        try (Reader reader = new BufferedReader(new FileReader(filePath));
             CSVParser csvParser = format.parse(reader)) {
            
            AtomicLong recordCount = new AtomicLong(0);
            
            for (CSVRecord csvRecord : csvParser) {
                Map<String, String> recordMap = new HashMap<>();
                
                if (config.isHasHeader()) {
                    // With headers - use column names as keys
                    for (Map.Entry<String, String> entry : csvRecord.toMap().entrySet()) {
                        String value = entry.getValue();
                        if (config.isTrimWhitespace() && value != null) {
                            value = value.trim();
                        }
                        recordMap.put(entry.getKey(), value);
                    }
                } else {
                    // Without headers - use numeric indices as keys
                    for (int i = 0; i < csvRecord.size(); i++) {
                        String value = csvRecord.get(i);
                        if (config.isTrimWhitespace() && value != null) {
                            value = value.trim();
                        }
                        recordMap.put(String.valueOf(i), value);
                    }
                }
                
                recordProcessor.accept(recordMap);
                recordCount.incrementAndGet();
                
                if (recordCount.get() % config.getBatchSize() == 0) {
                    logger.debug("Processed {} records from {}", recordCount.get(), filePath);
                }
            }
            
            logger.info("Completed processing {} records from {}", recordCount.get(), filePath);
        }
    }
    
    public Map<String, String> getHeaders(String filePath) throws IOException {
        CSVFormat.Builder builder = CSVFormat.DEFAULT
            .builder()
            .setDelimiter(config.getDelimiter())
            .setIgnoreHeaderCase(config.isIgnoreCase())
            .setTrim(config.isTrimWhitespace());
            
        if (config.isHasHeader()) {
            builder.setHeader();
        }
        
        CSVFormat format = builder.build();
        
        try (Reader reader = new BufferedReader(new FileReader(filePath));
             CSVParser csvParser = format.parse(reader)) {
            
            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            Map<String, String> result = new HashMap<>();
            if (headerMap != null) {
                for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                    result.put(entry.getKey(), entry.getValue().toString());
                }
            }
            return result;
        }
    }
    
    public long countRecords(String filePath) throws IOException {
        final AtomicLong count = new AtomicLong(0);
        
        processFile(filePath, record -> count.incrementAndGet());
        
        return count.get();
    }
    
    public List<Map<String, String>> readSampleRecords(String filePath, int sampleSize) throws IOException {
        List<Map<String, String>> sampleRecords = new ArrayList<>();
        
        processFile(filePath, record -> {
            if (sampleRecords.size() < sampleSize) {
                sampleRecords.add(new HashMap<>(record));
            }
        });
        
        return sampleRecords;
    }
}
