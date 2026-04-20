# CSV Data Reconciliation Service

A high-performance Java application for reconciling large CSV files with millions of records. This service provides detailed comparison reports between source and target CSV files, identifying missing records, extra records, and data mismatches.

## Features

- **High Performance**: Stream-based processing for handling large files (millions of records) with minimal memory footprint
- **Flexible Configuration**: Support for custom delimiters, headers, case sensitivity, and whitespace handling
- **Detailed Reporting**: Comprehensive mismatch reports with console output and file generation
- **Batch Processing**: Configurable batch sizes for optimal memory usage
- **Multiple Output Formats**: Support for text and JSON report formats
- **Robust Error Handling**: Graceful handling of malformed data and file issues

## Architecture

The service is built with a modular architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                        Main Application                      │
├─────────────────────────────────────────────────────────────┤
│  ReconciliationConfig  │  ReconciliationService  │  ReportingService  │
├─────────────────────────────────────────────────────────────┤
│              StreamingCsvReader (Apache Commons CSV)        │
├─────────────────────────────────────────────────────────────┤
│                    File System I/O                          │
└─────────────────────────────────────────────────────────────┘
```

## Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher

### Building the Project

```bash
git clone <repository-url>
cd reconservice
mvn clean compile
```

### Running the Application

```bash
# Basic usage
java -cp target/classes org.pk.Main source.csv target.csv

# With custom report file
java -cp target/classes org.pk.Main source.csv target.csv custom_report.txt

# Or build a JAR and run
mvn clean package
java -jar target/reconservice-1.0-SNAPSHOT.jar source.csv target.csv
```

### Running Tests

```bash
mvn test
```

## Configuration

The service uses a builder pattern for configuration:

```java
ReconciliationConfig config = ReconciliationConfig.builder()
    .delimiter(',')           // CSV delimiter character
    .hasHeader(true)          // Whether files have headers
    .keyColumn("id")          // Column to use as unique key
    .ignoreCase(false)        // Case-sensitive comparison
    .trimWhitespace(true)     // Trim whitespace from values
    .batchSize(10000)         // Records per batch for processing
    .build();
```

### Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `delimiter` | char | ',' | CSV delimiter character |
| `hasHeader` | boolean | true | Whether CSV files contain headers |
| `keyColumn` | String | null | Column name to use as unique identifier |
| `ignoreCase` | boolean | false | Perform case-insensitive comparisons |
| `trimWhitespace` | boolean | true | Trim whitespace from field values |
| `batchSize` | int | 10000 | Number of records per processing batch |

## Usage Examples

### Basic CSV Reconciliation

**Source CSV (source.csv):**
```csv
id,name,age,city
1,John Doe,25,New York
2,Jane Smith,30,London
3,Bob Johnson,35,Paris
```

**Target CSV (target.csv):**
```csv
id,name,age,city
1,John Doe,25,New York
2,Jane Smith,31,London
3,Bob Johnson,35,Paris
4,Alice Brown,28,Tokyo
```

**Command:**
```bash
java -jar reconservice.jar source.csv target.csv report.txt
```

**Output Report:**
```
CSV RECONCILIATION REPORT
================================================================================

SUMMARY:
--------
Source Records:     3
Target Records:     4
Matching Records:   2 (66.67%)
Missing in Target:  0
Extra in Target:    1
Mismatched Records: 1
Processing Time:     45 ms

STATUS:
------
❌ MISMATCHES FOUND - Review details below

DETAILED MISMATCHES:
--------------------
1. Key '2' has mismatches: age=[source='30', target='31']
2. Key '4' found in target but missing in source
```

### Custom Delimiter Example

For pipe-delimited files:

```java
ReconciliationConfig config = ReconciliationConfig.builder()
    .delimiter('|')
    .hasHeader(true)
    .keyColumn("employee_id")
    .build();
```

### No Header Files

For files without headers:

```java
ReconciliationConfig config = ReconciliationConfig.builder()
    .delimiter(',')
    .hasHeader(false)
    .keyColumn(null)  // Will use first column as key
    .build();
```

## Performance Considerations

### Memory Usage

The service uses streaming processing to minimize memory footprint:

- **Memory Usage**: O(batchSize) rather than O(totalRecords)
- **Recommended Batch Size**: 5,000 - 50,000 records depending on available memory
- **Large File Support**: Tested with files containing 10+ million records

### Performance Tips

1. **Batch Size**: Adjust batch size based on available memory
2. **SSD Storage**: Use SSD for better I/O performance
3. **Key Column**: Use indexed or unique columns for faster lookups
4. **Case Sensitivity**: Disable case sensitivity for faster comparisons

## API Reference

### ReconciliationService

Main service for performing CSV reconciliation.

```java
ReconciliationService service = new ReconciliationService(config);
ReconciliationResult result = service.reconcile(sourceFile, targetFile);
```

### ReconciliationResult

Contains the results of the reconciliation process.

```java
long sourceCount = result.getSourceRecordCount();
long targetCount = result.getTargetRecordCount();
long matches = result.getMatchingRecords();
double matchPercentage = result.getMatchPercentage();
boolean isPerfect = result.isPerfectMatch();
List<String> details = result.getMismatchDetails();
```

### ReportingService

Generates reports in various formats.

```java
ReportingService reporter = new ReportingService();
reporter.printConsoleReport(result);
reporter.generateReport(result, "report.txt");
String jsonReport = reporter.generateJsonReport(result);
```

## Error Handling

The service handles various error conditions gracefully:

- **File Not Found**: Clear error messages with file paths
- **Malformed CSV**: Skips invalid records and logs warnings
- **Missing Key Column**: Falls back to first column
- **Memory Constraints**: Batch processing prevents OOM errors

## Testing

The project includes comprehensive unit tests:

- **ReconciliationService Tests**: Various reconciliation scenarios
- **StreamingCsvReader Tests**: File reading and parsing
- **Configuration Tests**: Builder pattern and validation
- **Integration Tests**: End-to-end reconciliation workflows

Run tests with:
```bash
mvn test
```

For coverage reports:
```bash
mvn jacoco:report
```

## Logging

The service uses SLF4J with Logback for logging:

- **INFO Level**: High-level progress information
- **DEBUG Level**: Detailed batch processing information
- **WARN Level**: Non-fatal issues and data problems
- **ERROR Level**: Fatal errors and exceptions

Configure logging in `src/main/resources/logback.xml`.

## Building and Deployment

### Maven Build

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package JAR
mvn package

# Install to local repository
mvn install
```

### Docker Support

Create a Dockerfile for containerized deployment:

```dockerfile
FROM openjdk:11-jre-slim
COPY target/reconservice-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Troubleshooting

### Common Issues

1. **OutOfMemoryError**: Reduce batch size or increase heap size
2. **File Not Found**: Check file paths and permissions
3. **Malformed CSV**: Validate file format and delimiter settings
4. **No Key Column**: Ensure key column exists in both files

### Debug Mode

Enable debug logging:
```bash
java -Dlogging.level.org.pk=DEBUG -jar reconservice.jar source.csv target.csv
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:

- Create an issue in the project repository
- Check existing issues for solutions
- Review the test cases for usage examples
