package org.pk.config;

import java.util.List;
import java.util.Map;

public class ReconciliationConfig {
    private char delimiter = ',';
    private boolean hasHeader = true;
    private String keyColumn;
    private List<String> sourceKeyColumns;
    private List<String> targetKeyColumns;
    private String concatenationSeparator = "_";
    private Map<String, String> columnMapping;
    private boolean keyAutoDetected = false;
    private boolean ignoreCase = false;
    private boolean trimWhitespace = true;
    private int batchSize = 10000;
    
    public ReconciliationConfig() {
    }
    
    public ReconciliationConfig(char delimiter, boolean hasHeader, String keyColumn) {
        this.delimiter = delimiter;
        this.hasHeader = hasHeader;
        this.keyColumn = keyColumn;
    }
    
    public char getDelimiter() {
        return delimiter;
    }
    
    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }
    
    public boolean isHasHeader() {
        return hasHeader;
    }
    
    public void setHasHeader(boolean hasHeader) {
        this.hasHeader = hasHeader;
    }
    
    public String getKeyColumn() {
        return keyColumn;
    }
    
    public void setKeyColumn(String keyColumn) {
        this.keyColumn = keyColumn;
    }

    public List<String> getSourceKeyColumns() {
        return sourceKeyColumns;
    }

    public void setSourceKeyColumns(List<String> sourceKeyColumns) {
        this.sourceKeyColumns = sourceKeyColumns;
    }

    public List<String> getTargetKeyColumns() {
        return targetKeyColumns;
    }

    public void setTargetKeyColumns(List<String> targetKeyColumns) {
        this.targetKeyColumns = targetKeyColumns;
    }

    public String getConcatenationSeparator() {
        return concatenationSeparator;
    }

    public void setConcatenationSeparator(String concatenationSeparator) {
        this.concatenationSeparator = concatenationSeparator;
    }

    public Map<String, String> getColumnMapping() {
        return columnMapping;
    }

    public void setColumnMapping(Map<String, String> columnMapping) {
        this.columnMapping = columnMapping;
    }

    public boolean isKeyAutoDetected() {
        return keyAutoDetected;
    }

    public void setKeyAutoDetected(boolean keyAutoDetected) {
        this.keyAutoDetected = keyAutoDetected;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }
    
    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }
    
    public boolean isTrimWhitespace() {
        return trimWhitespace;
    }
    
    public void setTrimWhitespace(boolean trimWhitespace) {
        this.trimWhitespace = trimWhitespace;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public static ReconciliationConfig.Builder builder() {
        return new ReconciliationConfig.Builder();
    }
    
    public static class Builder {
        private char delimiter = ',';
        private boolean hasHeader = true;
        private String keyColumn;
        private List<String> sourceKeyColumns;
        private List<String> targetKeyColumns;
        private String concatenationSeparator = "_";
        private Map<String, String> columnMapping;
        private boolean keyAutoDetected = false;
        private boolean ignoreCase = false;
        private boolean trimWhitespace = true;
        private int batchSize = 10000;
        
        public Builder delimiter(char delimiter) {
            this.delimiter = delimiter;
            return this;
        }
        
        public Builder hasHeader(boolean hasHeader) {
            this.hasHeader = hasHeader;
            return this;
        }
        
        public Builder keyColumn(String keyColumn) {
            this.keyColumn = keyColumn;
            return this;
        }
        
        public Builder sourceKeyColumns(List<String> sourceKeyColumns) {
            this.sourceKeyColumns = sourceKeyColumns;
            return this;
        }
        
        public Builder targetKeyColumns(List<String> targetKeyColumns) {
            this.targetKeyColumns = targetKeyColumns;
            return this;
        }
        
        public Builder concatenationSeparator(String concatenationSeparator) {
            this.concatenationSeparator = concatenationSeparator;
            return this;
        }
        
        public Builder columnMapping(Map<String, String> columnMapping) {
            this.columnMapping = columnMapping;
            return this;
        }

        public Builder keyAutoDetected(boolean keyAutoDetected) {
            this.keyAutoDetected = keyAutoDetected;
            return this;
        }

        public Builder ignoreCase(boolean ignoreCase) {
            this.ignoreCase = ignoreCase;
            return this;
        }
        
        public Builder trimWhitespace(boolean trimWhitespace) {
            this.trimWhitespace = trimWhitespace;
            return this;
        }
        
        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }
        
        public ReconciliationConfig build() {
            ReconciliationConfig config = new ReconciliationConfig();
            config.delimiter = this.delimiter;
            config.hasHeader = this.hasHeader;
            config.keyColumn = this.keyColumn;
            config.sourceKeyColumns = this.sourceKeyColumns;
            config.targetKeyColumns = this.targetKeyColumns;
            config.concatenationSeparator = this.concatenationSeparator;
            config.columnMapping = this.columnMapping;
            config.keyAutoDetected = this.keyAutoDetected;
            config.ignoreCase = this.ignoreCase;
            config.trimWhitespace = this.trimWhitespace;
            config.batchSize = this.batchSize;
            return config;
        }
    }
}
