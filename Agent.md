# CSV Reconciliation Service - Agent Documentation

## Project Overview

A Spring Boot-based CSV file reconciliation service that compares two CSV files and identifies differences, mismatches, duplicates, and data quality issues. The service provides a web interface for uploading files and viewing detailed reconciliation reports.

## Tech Stack

- **Backend**: Java 11, Spring Boot 2.7.18
- **Frontend**: HTML, JavaScript, CSS
- **Build Tool**: Maven
- **CSV Processing**: Custom StreamingCsvReader for memory-efficient file processing

## Project Structure

```
reconservice/
├── src/main/java/org/pk/
│   ├── WebApplication.java          # Main Spring Boot application
│   ├── controller/
│   │   └── ReconciliationController.java # REST API endpoints
│   ├── model/
│   │   ├── ReconciliationResult.java  # Result model with reconciliation data
│   │   ├── MismatchDetail.java        # Individual mismatch detail model
│   │   └── ReconciliationConfig.java  # Configuration for reconciliation
│   ├── service/
│   │   ├── ReconciliationService.java # Core reconciliation logic
│   │   └── StreamingCsvReader.java    # CSV file reader with streaming support
│   └── util/
│       └── LevenshteinDistance.java   # String similarity calculation
├── src/main/resources/
│   ├── templates/
│   │   └── index.html                # Main web interface
│   ├── static/
│   │   ├── css/
│   │   │   └── style.css            # Styling
│   │   └── js/
│   │       └── app.js                # Frontend logic
│   └── application.properties        # Spring Boot configuration
└── pom.xml                          # Maven dependencies
```

## Key Components

### Backend Components

#### ReconciliationService
- **Purpose**: Core reconciliation logic
- **Key Methods**:
  - `reconcile()`: Main entry point for reconciliation
  - `compareRecords()`: Compares source and target records
  - `createMismatchDetails()`: Creates detailed mismatch information
- **Recent Changes**: Fixed duplicate detection to only count duplicates within each file separately, not across files

#### StreamingCsvReader
- **Purpose**: Memory-efficient CSV file processing
- **Features**: Streams records one at a time to handle large files without memory issues

#### ReconciliationResult
- **Purpose**: Encapsulates all reconciliation results
- **Fields**: Record counts, mismatch details, duplicate records, processing time, etc.

### Frontend Components

#### app.js
- **Purpose**: Frontend logic for UI interactions
- **Key Functions**:
  - `renderMismatches()`: Renders Data Mismatches tab with 2-row format (source + target)
  - `renderMissingRecords()`: Renders Source Only tab with column filters
  - `renderExtraRecords()`: Renders Target Only tab with column filters
  - `renderDuplicates()`: Renders Duplicate Records tab with column filters
- **Recent Changes**: 
  - Added column-level filters to all tabs
  - Fixed column determination to use full dataset instead of filtered data
  - Implemented highlighting for matched (green) and mismatched (red) attributes

## How to Run the Application

### Prerequisites
- Java 11 or higher
- Maven 3.x

### Build and Run
```bash
# Build the project
mvn clean package -DskipTests

# Run the application
java -jar target/reconservice-1.0-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

### Development Mode
```bash
# Run with Spring Boot DevTools for hot reload
mvn spring-boot:run
```

## Development Guidelines

### Code Style
- Follow Java naming conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Keep methods focused and small

### Frontend Changes
- Update cache-busting parameter in `index.html` after changing `app.js`
- Current version: v33
- Location: `<script src="/js/app.js?v=33"></script>`

### Backend Changes
- Rebuild the application after Java changes: `mvn clean package -DskipTests`
- Restart the server after rebuilding

## Recent Features and Changes

### Recent Enhancements (April 2026)
1. **Tabbed Interface**
   - Implemented main tab navigation with "Upload Files" and "Results" tabs
   - Upload section now resides in the Upload tab
   - Results and Report sections moved to Results tab
   - Added glass-morphism styling for tabs with hover effects
   - Automatic tab switching when "Reconcile Files" button is clicked

2. **Asynchronous File Processing with Progress Feedback**
   - Added loading spinner with "Processing Files..." message
   - Automatic switch to Results tab when processing starts
   - User-friendly message: "This may take a while for large files. Please wait."
   - Improved UX for handling large file reconciliations

3. **Empty Value Filtering in Column Filters**
   - Added special keyword "empty" to filter for null/blank values
   - Users can type "empty" in any column filter input
   - Filters records where column value is null, undefined, empty string (''), or whitespace-only
   - Updated all four render functions (Missing, Extra, Duplicates, Mismatches)
   - Updated placeholder text: "Filter... (type 'empty' for null/blank)"

4. **Pagination Container Styling Improvements**
   - Added padding and background color to pagination containers
   - Added border with rounded corners for better visual separation
   - Improved button hover state to only apply when not disabled
   - Added disabled button styling (gray background, no hover effect)

5. **Duplicate Records Logic Fix**
   - Fixed to only detect duplicates within each file separately
   - Previously incorrectly treated records existing in both files as duplicates
   - Now correctly identifies true duplicates (same key appearing multiple times in same file)

6. **Column-Level Filters**
   - Added column-specific filter inputs to all tabs
   - Removed global search-container
   - Filters apply to Source Only, Target Only, Duplicate Records, and Data Mismatches tabs
   - Fixed column determination to use full dataset instead of filtered data

7. **Data Mismatches Tab Enhancements**
   - Implemented 2-row format per mismatch (source row + target row)
   - Highlighted mismatched attributes in light red (#ffcdd2)
   - Highlighted matched attributes in light green (#c8e6c9)
   - Always shows ReconKeyColumn, KeyValue, and SOURCE/TARGET columns

8. **UI Improvements**
   - Made Executive Summary more compact (smaller fonts, reduced padding)
   - Removed duplicate summary sections from Detailed Report
   - Fixed Source/Target column filter to always match (shows both rows)

## Important Notes

### Duplicate Detection
- Duplicates are now correctly detected as keys appearing multiple times within the SAME file
- Records that exist in both source and target files are NOT duplicates (they are matching records)
- The Duplicate Records tab shows only true duplicates with a `DuplicateCount` field

### Mismatch Detection
- Mismatch percentage is calculated as: (mismatchCount / totalRecords) * 100
- Mismatch details include full record data for detailed display
- Each mismatch is shown with both source and target values

### Column Filters
- Column filters work in real-time
- Filters reset pagination to page 1 when applied
- Columns are determined from the full dataset, not filtered data

### Cache Busting
- Always update the version parameter in `index.html` after changing `app.js`
- Format: `/js/app.js?v=N` where N is an incrementing number

## Troubleshooting

### Common Issues

1. **Frontend changes not appearing**
   - Check cache-busting parameter in `index.html`
   - Hard refresh browser (Ctrl+Shift+R or Cmd+Shift+R)

2. **Column filters not working**
   - Ensure columns are determined from full dataset before filtering
   - Check that event listeners are properly attached to filter inputs

3. **Duplicate records showing incorrectly**
   - Verify that duplicate detection only checks within each file separately
   - Check that sourceKeyCounts and targetKeyCounts are being populated correctly

## Testing

### Manual Testing Checklist
- [ ] Upload two CSV files with same structure
- [ ] Verify reconciliation summary shows correct counts
- [ ] Check Data Mismatches tab shows 2-row format with highlighting
- [ ] Test column filters on all tabs
- [ ] Verify Source Only and Target Only tabs show correct data
- [ ] Check Duplicate Records tab shows only true duplicates
- [ ] Test with files that have duplicate keys within same file
- [ ] Test with files that have duplicate keys across files (should not show as duplicates)

## API Endpoints

### POST /api/reconcile
- **Purpose**: Reconcile two CSV files
- **Request**: Multipart form data with sourceFile, targetFile, and configuration options
- **Response**: ReconciliationResult JSON with all reconciliation data

## Configuration Options

### ReconciliationConfig
- `keyColumn`: Column to use as key (optional, auto-detected if not provided)
- `delimiter`: CSV delimiter (default: comma)
- `hasHeader`: Whether files have headers (default: true)
- `ignoreCase`: Whether to ignore case when comparing values
- `trimWhitespace`: Whether to trim whitespace from values

## Performance Considerations

- Uses streaming CSV reader to handle large files without memory issues
- Processes files sequentially to minimize memory footprint
- Pagination implemented for large result sets (default: 10 records per page)

## Future Enhancements

Potential areas for improvement:
- Support for composite keys (multiple columns as key)
- Export reconciliation results to Excel/CSV
- Scheduled reconciliation jobs
- Historical reconciliation results storage
- Advanced filtering and sorting options
