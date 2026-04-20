document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('reconciliationForm');
    const sourceFileInput = document.getElementById('sourceFile');
    const targetFileInput = document.getElementById('targetFile');
    const sourceFileName = document.getElementById('sourceFileName');
    const targetFileName = document.getElementById('targetFileName');
    const submitBtn = document.getElementById('submitBtn');
    const btnText = submitBtn.querySelector('.btn-text');
    const btnLoader = submitBtn.querySelector('.btn-loader');
    const resultsSection = document.getElementById('resultsSection');
    const resultsContent = document.getElementById('resultsContent');
    const reportSection = document.getElementById('reportSection');
    const reportContent = document.getElementById('reportContent');
    const downloadReportBtn = document.getElementById('downloadReportBtn');

    // Main tab switching
    const mainTabs = document.querySelectorAll('.main-tab');
    const mainTabContents = document.querySelectorAll('.tab-content');

    mainTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const tabName = tab.dataset.tab;
            
            mainTabs.forEach(t => t.classList.remove('active'));
            mainTabContents.forEach(c => c.classList.remove('active'));
            
            tab.classList.add('active');
            document.getElementById(`${tabName}-tab`).classList.add('active');
        });
    });

    // File input change handlers
    sourceFileInput.addEventListener('change', function() {
        sourceFileName.textContent = this.files[0] ? this.files[0].name : '';
    });

    targetFileInput.addEventListener('change', function() {
        targetFileName.textContent = this.files[0] ? this.files[0].name : '';
    });

    // Advanced key configuration toggle
    const advancedKeyConfigCheckbox = document.getElementById('advancedKeyConfig');
    const advancedKeyConfigSection = document.getElementById('advancedKeyConfigSection');
    
    advancedKeyConfigCheckbox.addEventListener('change', function() {
        advancedKeyConfigSection.style.display = this.checked ? 'block' : 'none';
        if (this.checked) {
            document.getElementById('keyColumn').value = '';
        }
    });

    // Form submission handler
    form.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        // Validate files
        if (!sourceFileInput.files[0] || !targetFileInput.files[0]) {
            alert('Please select both source and target files');
            return;
        }

        // Show loading state
        submitBtn.disabled = true;
        btnText.style.display = 'none';
        btnLoader.style.display = 'inline-block';
        
        // Switch to results tab and show loading message
        mainTabs.forEach(t => t.classList.remove('active'));
        mainTabContents.forEach(c => c.classList.remove('active'));
        document.querySelector('.main-tab[data-tab="results"]').classList.add('active');
        document.getElementById('results-tab').classList.add('active');
        
        // Show loading state in results section
        resultsSection.style.display = 'block';
        resultsContent.innerHTML = `
            <div class="loading-container" style="text-align: center; padding: 40px;">
                <div class="spinner" style="border: 4px solid #f3f3f3; border-top: 4px solid #667eea; border-radius: 50%; width: 50px; height: 50px; animation: spin 1s linear infinite; margin: 0 auto 20px;"></div>
                <h3 style="color: #667eea; margin-bottom: 10px;">Processing Files...</h3>
                <p style="color: #666;">This may take a while for large files. Please wait.</p>
            </div>
            <style>
                @keyframes spin {
                    0% { transform: rotate(0deg); }
                    100% { transform: rotate(360deg); }
                }
            </style>
        `;
        reportSection.style.display = 'none';

        // Create form data
        const formData = new FormData();
        formData.append('sourceFile', sourceFileInput.files[0]);
        formData.append('targetFile', targetFileInput.files[0]);
        formData.append('delimiter', document.getElementById('delimiter').value);
        formData.append('hasHeader', document.getElementById('hasHeader').value);
        formData.append('keyColumn', document.getElementById('keyColumn').value);
        formData.append('ignoreCase', document.getElementById('ignoreCase').checked);
        formData.append('trimWhitespace', document.getElementById('trimWhitespace').checked);
        
        // Add advanced key configuration fields
        const advancedKeyConfig = document.getElementById('advancedKeyConfig').checked;
        formData.append('advancedKeyConfig', advancedKeyConfig);
        
        if (advancedKeyConfig) {
            formData.append('sourceKeyColumns', document.getElementById('sourceKeyColumns').value);
            formData.append('targetKeyColumns', document.getElementById('targetKeyColumns').value);
            formData.append('concatenationSeparator', document.getElementById('concatenationSeparator').value);
            formData.append('columnMapping', document.getElementById('columnMapping').value);
        }

        try {
            const response = await fetch('/api/reconcile', {
                method: 'POST',
                body: formData
            });

            const data = await response.json();

            if (data.success) {
                displayResults(data.result);
                displayReport(data.result);
                downloadReportBtn.onclick = () => downloadReport(data.result);
            } else {
                displayError(data.error);
            }
        } catch (error) {
            displayError('Error: ' + error.message);
        } finally {
            // Reset button state
            submitBtn.disabled = false;
            btnText.style.display = 'inline-block';
            btnLoader.style.display = 'none';
        }
    });

    function displayResults(result) {
        resultsSection.style.display = 'block';
        
        const matchPercentage = result.matchPercentage.toFixed(2);
        const isPerfectMatch = result.matchingRecords === result.sourceRecordCount && 
                               result.matchingRecords === result.targetRecordCount && 
                               result.mismatchedRecords === 0;

        let html = `
            <div class="status-badge ${isPerfectMatch ? 'status-success' : 'status-error'}" style="font-size: 14px; padding: 8px 16px;">
                ${isPerfectMatch ? '✅ PERFECT MATCH' : '❌ MISMATCHES FOUND'}
            </div>
            
            <div class="summary-grid" style="gap: 10px;">
                <div class="summary-card" style="padding: 12px;">
                    <h3 style="font-size: 12px; margin-bottom: 4px;">Source Records</h3>
                    <div class="value" style="font-size: 18px;">${result.sourceRecordCount.toLocaleString()}</div>
                </div>
                <div class="summary-card" style="padding: 12px;">
                    <h3 style="font-size: 12px; margin-bottom: 4px;">Target Records</h3>
                    <div class="value" style="font-size: 18px;">${result.targetRecordCount.toLocaleString()}</div>
                </div>
                <div class="summary-card" style="padding: 12px;">
                    <h3 style="font-size: 12px; margin-bottom: 4px;">Matching Records</h3>
                    <div class="value" style="font-size: 18px;">${result.matchingRecords.toLocaleString()}</div>
                    <div class="percentage" style="font-size: 14px;">${matchPercentage}%</div>
                </div>
                <div class="summary-card" style="padding: 12px;">
                    <h3 style="font-size: 12px; margin-bottom: 4px;">Mismatched Records</h3>
                    <div class="value" style="font-size: 18px;">${result.mismatchedRecords.toLocaleString()}</div>
                </div>
                <div class="summary-card" style="padding: 12px;">
                    <h3 style="font-size: 12px; margin-bottom: 4px;">Source Only</h3>
                    <div class="value" style="font-size: 18px;">${result.missingInTarget.toLocaleString()}</div>
                </div>
                <div class="summary-card" style="padding: 12px;">
                    <h3 style="font-size: 12px; margin-bottom: 4px;">Target Only</h3>
                    <div class="value" style="font-size: 18px;">${result.extraInTarget.toLocaleString()}</div>
                </div>
                <div class="summary-card" style="padding: 12px;">
                    <h3 style="font-size: 12px; margin-bottom: 4px;">Processing Time</h3>
                    <div class="value" style="font-size: 18px;">${result.processingTimeMs}ms</div>
                </div>
                ${result.sourceDuplicateKeys > 0 ? `
                <div class="summary-card" style="padding: 12px; background: #fff3cd;">
                    <h3 style="font-size: 12px; margin-bottom: 4px; color: #856404;">Duplicate in Source</h3>
                    <div class="value" style="font-size: 18px; color: #dc3545;">${result.sourceDuplicateKeys}</div>
                </div>
                ` : ''}
                ${result.targetDuplicateKeys > 0 ? `
                <div class="summary-card" style="padding: 12px; background: #fff3cd;">
                    <h3 style="font-size: 12px; margin-bottom: 4px; color: #856404;">Duplicate in Target</h3>
                    <div class="value" style="font-size: 18px; color: #dc3545;">${result.targetDuplicateKeys}</div>
                </div>
                ` : ''}
            </div>
        `;

        if (result.mismatchDetails && result.mismatchDetails.length > 0) {
            html += `
                <h3>Mismatch Summary</h3>
                <div class="mismatches-table-container">
                <table class="mismatches-table">
                    <thead>
                        <tr>
                            <th>Column Name</th>
                            <th>Source Value</th>
                            <th>Target Value</th>
                            <th>Mismatch %</th>
                            <th>Mismatch Count</th>
                            <th>Total Records</th>
                        </tr>
                    </thead>
                    <tbody>
            `;
            
            const totalRecords = result.matchingRecords;
            
            // Group mismatch details by column name
            const groupedByColumn = new Map();
            result.mismatchDetails.forEach((detail) => {
                if (!groupedByColumn.has(detail.columnName)) {
                    groupedByColumn.set(detail.columnName, []);
                }
                groupedByColumn.get(detail.columnName).push(detail);
            });
            
            // Display one example per column with count
            groupedByColumn.forEach((details, columnName) => {
                const example = details[0]; // Show first example
                const mismatchCount = details.length; // Count of mismatches for this column
                const mismatchPercentage = totalRecords > 0 ? (mismatchCount / totalRecords * 100).toFixed(2) : '0.00';
                html += `
                    <tr>
                        <td><strong>${columnName || 'N/A'}</strong></td>
                        <td class="source-value">${example.sourceValue || 'N/A'}</td>
                        <td class="target-value">${example.targetValue || 'N/A'}</td>
                        <td class="mismatch-percentage">${mismatchPercentage}%</td>
                        <td>${mismatchCount}</td>
                        <td>${totalRecords}</td>
                    </tr>
                `;
            });
            
            html += `
                    </tbody>
                </table>
                </div>
            `;
        }

        resultsContent.innerHTML = html;
    }

    function displayReport(result) {
        reportSection.style.display = 'block';
        
        const isPerfectMatch = result.matchingRecords === result.sourceRecordCount && 
                              result.matchingRecords === result.targetRecordCount && 
                              result.mismatchedRecords === 0;
        
        let html = `
            <div class="status-indicator ${isPerfectMatch ? 'success' : 'error'}" style="font-size: 14px;">
                ${isPerfectMatch ? '✅ PERFECT MATCH - All records match perfectly!' : '❌ MISMATCHES DETECTED - Review details below'}
            </div>

            <div class="report-section" style="padding: 10px;">
                <h3 style="font-size: 0.9rem; margin: 0 0 8px 0; color: #495057;">Reconciliation Configuration</h3>
                <div style="display: flex; gap: 20px; font-size: 0.85rem; color: #495057;">
                    <span><strong>Headers:</strong> ${result.hasHeaders ? 'Yes' : 'No'}</span>
                    <span><strong>Delimiter:</strong> ${result.delimiter || ','}</span>
                    <span><strong>Key:</strong> ${result.keyAutoDetected ? result.keyColumn + ' 🤖' : result.keyColumn}</span>
                </div>
            </div>

            ${result.sourceDuplicateKeys > 0 || result.targetDuplicateKeys > 0 ? `
                <div class="report-section" style="background: #fff3cd; border-left-color: #ffc107; padding: 12px; margin-bottom: 10px;">
                    <h3 style="color: #856404; font-size: 0.95rem; margin: 0 0 8px 0;">⚠️ Data Quality Issues</h3>
                    <div style="display: flex; gap: 20px; font-size: 0.85rem;">
                        <div>
                            <span style="color: #856404;">Source Duplicates:</span>
                            <span style="color: #dc3545; font-weight: bold; margin-left: 5px;">${result.sourceDuplicateKeys}</span>
                        </div>
                        <div>
                            <span style="color: #856404;">Target Duplicates:</span>
                            <span style="color: #dc3545; font-weight: bold; margin-left: 5px;">${result.targetDuplicateKeys}</span>
                        </div>
                        <div>
                            <span style="color: #856404;">Total Duplicate Records:</span>
                            <span style="color: #dc3545; font-weight: bold; margin-left: 5px;">${result.duplicateRecords ? result.duplicateRecords.length : 0}</span>
                        </div>
                    </div>
                </div>
            ` : ''}

            <div class="tabs-container">
                <div class="tabs">
                    <button class="tab active" data-tab="mismatches">🔍 Data Mismatches <span class="record-count-badge ${result.mismatchedRecords > 0 ? 'error' : 'success'}">${result.mismatchedRecords.toLocaleString()}</span></button>
                    <button class="tab" data-tab="missing">🔴 Source Only <span class="record-count-badge ${result.missingInTarget > 0 ? 'error' : 'success'}">${result.missingInTarget.toLocaleString()}</span></button>
                    <button class="tab" data-tab="extra">🟢 Target Only <span class="record-count-badge ${result.extraInTarget > 0 ? 'warning' : 'success'}">${result.extraInTarget.toLocaleString()}</span></button>
                    <button class="tab" data-tab="duplicates">⚠️ Duplicate Records <span class="record-count-badge ${result.duplicateRecords && result.duplicateRecords.length > 0 ? 'warning' : 'success'}">${result.duplicateRecords ? result.duplicateRecords.length : 0}</span></button>
                </div>

                <div class="tab-content active" id="mismatches-tab">
                    <div id="mismatches-list"></div>
                    <div class="pagination-container" id="mismatches-pagination"></div>
                </div>

                <div class="tab-content" id="missing-tab">
                    <div id="missing-records-list"></div>
                    <div class="pagination-container" id="missing-pagination"></div>
                </div>

                <div class="tab-content" id="extra-tab">
                    <div id="extra-records-list"></div>
                    <div class="pagination-container" id="extra-pagination"></div>
                </div>

                <div class="tab-content" id="duplicates-tab">
                    <div id="duplicates-records-list"></div>
                    <div class="pagination-container" id="duplicates-pagination"></div>
                </div>
            </div>

            <div class="report-section" style="padding: 10px;">
                <h3 style="font-size: 0.9rem; margin: 0 0 8px 0; color: #495057;">📝 Report Information</h3>
                <div style="display: flex; gap: 20px; font-size: 0.85rem; color: #495057;">
                    <span><strong>Generated:</strong> ${new Date().toLocaleString()}</span>
                    <span><strong>Report ID:</strong> ${Date.now()}</span>
                </div>
            </div>
        `;

        reportContent.innerHTML = html;
        
        // Initialize pagination and search functionality
        initializePagination(result);
        initializeTabs();
    }

    // Store current pagination state
    let paginationState = {
        missing: { page: 1, pageSize: 50, data: [] },
        extra: { page: 1, pageSize: 50, data: [] },
        mismatches: { page: 1, pageSize: 50, data: [] },
        duplicates: { page: 1, pageSize: 50, data: [] }
    };

    function initializePagination(result) {
        paginationState.missing.data = result.sourceOnlyRecords || [];
        paginationState.extra.data = result.targetOnlyRecords || [];
        paginationState.mismatches.data = result.mismatchDetails || [];
        paginationState.duplicates.data = result.duplicateRecords || [];

        renderMissingRecords();
        renderExtraRecords();
        renderMismatches();
        renderDuplicates();
    }

    function initializeTabs() {
        const tabs = document.querySelectorAll('.tab');
        const tabContents = document.querySelectorAll('.tab-content');

        tabs.forEach(tab => {
            tab.addEventListener('click', () => {
                const tabName = tab.dataset.tab;
                
                tabs.forEach(t => t.classList.remove('active'));
                tabContents.forEach(c => c.classList.remove('active'));
                
                tab.classList.add('active');
                document.getElementById(`${tabName}-tab`).classList.add('active');
                
                // Render the appropriate tab content
                if (tabName === 'missing') renderMissingRecords();
                else if (tabName === 'extra') renderExtraRecords();
                else if (tabName === 'duplicates') renderDuplicates();
                else if (tabName === 'mismatches') renderMismatches();
            });
        });
    }

    function filterRecords(type, searchTerm) {
        const state = paginationState[type];
        const lowerSearchTerm = searchTerm.toLowerCase();
        
        if (!searchTerm) {
            state.data = type === 'mismatches' ? 
                paginationState.mismatches.data : 
                (type === 'missing' ? paginationState.missing.data : 
                (type === 'extra' ? paginationState.extra.data : paginationState.duplicates.data));
        } else {
            const originalData = type === 'mismatches' ? 
                paginationState.mismatches.data : 
                (type === 'missing' ? paginationState.missing.data : 
                (type === 'extra' ? paginationState.extra.data : paginationState.duplicates.data));
            
            state.data = originalData.filter(record => {
                if (type === 'mismatches') {
                    // Search in structured mismatch details
                    return (record.columnName && record.columnName.toLowerCase().includes(lowerSearchTerm)) ||
                           (record.sourceValue && String(record.sourceValue).toLowerCase().includes(lowerSearchTerm)) ||
                           (record.targetValue && String(record.targetValue).toLowerCase().includes(lowerSearchTerm));
                } else {
                    return Object.values(record).some(value => 
                        String(value).toLowerCase().includes(lowerSearchTerm)
                    );
                }
            });
        }
        
        state.page = 1;
        if (type === 'missing') renderMissingRecords();
        else if (type === 'extra') renderExtraRecords();
        else if (type === 'duplicates') renderDuplicates();
        else renderMismatches();
    }

    function renderMissingRecords() {
        const state = paginationState.missing;
        const container = document.getElementById('missing-records-list');
        const paginationContainer = document.getElementById('missing-pagination');
        
        if (!container) return;
        
        // Get all unique column names from the full dataset
        const allColumns = new Set();
        state.data.forEach(record => {
            if (typeof record === 'object' && record !== null) {
                Object.keys(record).forEach(key => allColumns.add(key));
            }
        });
        
        // Ensure ReconKeyColumn and keyValue are first, then sort remaining columns alphabetically
        const columns = Array.from(allColumns);
        const priorityColumns = ['ReconKeyColumn', 'keyValue'];
        const orderedColumns = [
            ...priorityColumns.filter(col => columns.includes(col)),
            ...columns.filter(col => !priorityColumns.includes(col)).sort()
        ];
        
        // Apply column filters if they exist
        let filteredData = state.data;
        if (state.columnFilters) {
            filteredData = state.data.filter(record => {
                for (const [column, filterValue] of Object.entries(state.columnFilters)) {
                    if (!filterValue) continue;
                    const lowerFilter = filterValue.toLowerCase();
                    
                    // Special keyword 'empty' to filter for empty/null/undefined values
                    if (lowerFilter === 'empty') {
                        const value = record[column];
                        if (value !== null && value !== undefined && value !== '' && String(value).trim() !== '') {
                            return false;
                        }
                    } else {
                        const value = String(record[column] || '').toLowerCase();
                        if (!value.includes(lowerFilter)) {
                            return false;
                        }
                    }
                }
                return true;
            });
        }
        
        const start = (state.page - 1) * state.pageSize;
        const end = start + state.pageSize;
        const recordsToShow = filteredData.slice(start, end);
        
        if (recordsToShow.length === 0) {
            container.innerHTML = '<p style="text-align: center; color: #666; padding: 20px;">No missing records found</p>';
            paginationContainer.innerHTML = '';
            return;
        }
        
        let html = `
            <div class="records-table-container">
                <table class="records-table">
                    <thead>
                        <tr>
                            <th>SL No</th>
        `;
        
        orderedColumns.forEach(column => {
            const filterValue = state.columnFilters && state.columnFilters[column] ? state.columnFilters[column] : '';
            const hasFilter = filterValue && filterValue.trim() !== '';
            html += `
                <th>
                    <div>${column}</div>
                    <input type="text" 
                           class="column-filter-input ${hasFilter ? 'column-filter-active' : ''}" 
                           data-column="${column}" 
                           placeholder="Filter... (type 'empty' for null/blank)" 
                           value="${filterValue}"
                           style="margin-top: 5px; padding: 3px; font-size: 12px; width: 90%;">
                </th>
            `;
        });
        
        html += `
                        </tr>
                    </thead>
                    <tbody>
        `;
        
        let slNo = (state.page - 1) * state.pageSize + 1;
        recordsToShow.forEach(record => {
            html += '<tr>';
            html += `<td>${slNo++}</td>`;
            orderedColumns.forEach(column => {
                html += `<td>${record[column] !== undefined ? record[column] : ''}</td>`;
            });
            html += '</tr>';
        });
        
        html += `
                    </tbody>
                </table>
            </div>
        `;
        
        container.innerHTML = html;
        
        // Add event listeners for column filter inputs
        container.querySelectorAll('.column-filter-input').forEach(input => {
            input.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    const column = e.target.dataset.column;
                    const value = e.target.value;
                    
                    if (!state.columnFilters) {
                        state.columnFilters = {};
                    }
                    
                    state.columnFilters[column] = value;
                    state.page = 1;
                    renderMissingRecords();
                }
            });
            
            input.addEventListener('input', (e) => {
                if (e.target.value.trim() !== '') {
                    e.target.classList.add('column-filter-active');
                } else {
                    e.target.classList.remove('column-filter-active');
                }
            });
        });
        
        renderPagination('missing', paginationContainer);
    }

    function renderExtraRecords() {
        const state = paginationState.extra;
        const container = document.getElementById('extra-records-list');
        const paginationContainer = document.getElementById('extra-pagination');
        
        if (!container) return;
        
        // Get all unique column names from the full dataset
        const allColumns = new Set();
        state.data.forEach(record => {
            if (typeof record === 'object' && record !== null) {
                Object.keys(record).forEach(key => allColumns.add(key));
            }
        });
        
        // Ensure ReconKeyColumn and keyValue are first, then sort remaining columns alphabetically
        const columns = Array.from(allColumns);
        const priorityColumns = ['ReconKeyColumn', 'keyValue'];
        const orderedColumns = [
            ...priorityColumns.filter(col => columns.includes(col)),
            ...columns.filter(col => !priorityColumns.includes(col)).sort()
        ];
        
        // Apply column filters if they exist
        let filteredData = state.data;
        if (state.columnFilters) {
            filteredData = state.data.filter(record => {
                for (const [column, filterValue] of Object.entries(state.columnFilters)) {
                    if (!filterValue) continue;
                    const lowerFilter = filterValue.toLowerCase();
                    
                    // Special keyword 'empty' to filter for empty/null/undefined values
                    if (lowerFilter === 'empty') {
                        const value = record[column];
                        if (value !== null && value !== undefined && value !== '' && String(value).trim() !== '') {
                            return false;
                        }
                    } else {
                        const value = String(record[column] || '').toLowerCase();
                        if (!value.includes(lowerFilter)) {
                            return false;
                        }
                    }
                }
                return true;
            });
        }
        
        const start = (state.page - 1) * state.pageSize;
        const end = start + state.pageSize;
        const recordsToShow = filteredData.slice(start, end);
        
        if (recordsToShow.length === 0) {
            container.innerHTML = '<p style="text-align: center; color: #666; padding: 20px;">No extra records found</p>';
            paginationContainer.innerHTML = '';
            return;
        }
        
        let html = `
            <div class="records-table-container">
                <table class="records-table">
                    <thead>
                        <tr>
                            <th>SL No</th>
        `;
        
        orderedColumns.forEach(column => {
            const filterValue = state.columnFilters && state.columnFilters[column] ? state.columnFilters[column] : '';
            const hasFilter = filterValue && filterValue.trim() !== '';
            html += `
                <th>
                    <div>${column}</div>
                    <input type="text" 
                           class="column-filter-input ${hasFilter ? 'column-filter-active' : ''}" 
                           data-column="${column}" 
                           placeholder="Filter... (type 'empty' for null/blank)" 
                           value="${filterValue}"
                           style="margin-top: 5px; padding: 3px; font-size: 12px; width: 90%;">
                </th>
            `;
        });
        
        html += `
                        </tr>
                    </thead>
                    <tbody>
        `;
        
        let slNo = (state.page - 1) * state.pageSize + 1;
        recordsToShow.forEach(record => {
            html += '<tr>';
            html += `<td>${slNo++}</td>`;
            orderedColumns.forEach(column => {
                html += `<td>${record[column] !== undefined ? record[column] : ''}</td>`;
            });
            html += '</tr>';
        });
        
        html += `
                    </tbody>
                </table>
            </div>
        `;
        
        container.innerHTML = html;
        
        // Add event listeners for column filter inputs
        container.querySelectorAll('.column-filter-input').forEach(input => {
            input.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    const column = e.target.dataset.column;
                    const value = e.target.value;
                    
                    if (!state.columnFilters) {
                        state.columnFilters = {};
                    }
                    
                    state.columnFilters[column] = value;
                    state.page = 1;
                    renderExtraRecords();
                }
            });
            
            input.addEventListener('input', (e) => {
                if (e.target.value.trim() !== '') {
                    e.target.classList.add('column-filter-active');
                } else {
                    e.target.classList.remove('column-filter-active');
                }
            });
        });
        
        renderPagination('extra', paginationContainer);
    }

    function renderDuplicates() {
        const state = paginationState.duplicates;
        const container = document.getElementById('duplicates-records-list');
        const paginationContainer = document.getElementById('duplicates-pagination');
        
        if (!container) return;
        
        // Get all unique column names from the full dataset
        const allColumns = new Set();
        state.data.forEach(record => {
            if (typeof record === 'object' && record !== null) {
                Object.keys(record).forEach(key => allColumns.add(key));
            }
        });
        
        // Ensure ReconKeyColumn, keyValue, Source, and DuplicateCount are first, then sort remaining columns alphabetically
        const columns = Array.from(allColumns);
        const priorityColumns = ['ReconKeyColumn', 'keyValue', 'Source', 'DuplicateCount'];
        const orderedColumns = [
            ...priorityColumns.filter(col => columns.includes(col)),
            ...columns.filter(col => !priorityColumns.includes(col)).sort()
        ];
        
        // Apply column filters if they exist
        let filteredData = state.data;
        if (state.columnFilters) {
            filteredData = state.data.filter(record => {
                for (const [column, filterValue] of Object.entries(state.columnFilters)) {
                    if (!filterValue) continue;
                    const lowerFilter = filterValue.toLowerCase();
                    
                    // Special keyword 'empty' to filter for empty/null/undefined values
                    if (lowerFilter === 'empty') {
                        const value = record[column];
                        if (value !== null && value !== undefined && value !== '' && String(value).trim() !== '') {
                            return false;
                        }
                    } else {
                        const value = String(record[column] || '').toLowerCase();
                        if (!value.includes(lowerFilter)) {
                            return false;
                        }
                    }
                }
                return true;
            });
        }
        
        const start = (state.page - 1) * state.pageSize;
        const end = start + state.pageSize;
        const recordsToShow = filteredData.slice(start, end);
        
        if (recordsToShow.length === 0) {
            container.innerHTML = '<p style="text-align: center; color: #666; padding: 20px;">No duplicate records found</p>';
            paginationContainer.innerHTML = '';
            return;
        }
        
        let html = `
            <div class="records-table-container">
                <table class="records-table">
                    <thead>
                        <tr>
                            <th>SL No</th>
        `;
        
        orderedColumns.forEach(column => {
            const filterValue = state.columnFilters && state.columnFilters[column] ? state.columnFilters[column] : '';
            const hasFilter = filterValue && filterValue.trim() !== '';
            html += `
                <th>
                    <div>${column}</div>
                    <input type="text" 
                           class="column-filter-input ${hasFilter ? 'column-filter-active' : ''}" 
                           data-column="${column}" 
                           placeholder="Filter... (type 'empty' for null/blank)" 
                           value="${filterValue}"
                           style="margin-top: 5px; padding: 3px; font-size: 12px; width: 90%;">
                </th>
            `;
        });
        
        html += `
                        </tr>
                    </thead>
                    <tbody>
        `;
        
        let slNo = (state.page - 1) * state.pageSize + 1;
        recordsToShow.forEach(record => {
            html += '<tr>';
            html += `<td>${slNo++}</td>`;
            orderedColumns.forEach(column => {
                const value = record[column] !== undefined ? record[column] : '';
                // Highlight Source column
                if (column === 'Source') {
                    const color = value === 'Source' ? '#dc3545' : '#28a745';
                    html += `<td style="color: ${color}; font-weight: bold;">${value}</td>`;
                } else {
                    html += `<td>${value}</td>`;
                }
            });
            html += '</tr>';
        });
        
        html += `
                    </tbody>
                </table>
            </div>
        `;
        
        container.innerHTML = html;
        
        // Add event listeners for column filter inputs
        container.querySelectorAll('.column-filter-input').forEach(input => {
            input.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    const column = e.target.dataset.column;
                    const value = e.target.value;
                    
                    if (!state.columnFilters) {
                        state.columnFilters = {};
                    }
                    
                    state.columnFilters[column] = value;
                    state.page = 1;
                    renderDuplicates();
                }
            });
            
            input.addEventListener('input', (e) => {
                if (e.target.value.trim() !== '') {
                    e.target.classList.add('column-filter-active');
                } else {
                    e.target.classList.remove('column-filter-active');
                }
            });
        });
        
        renderPagination('duplicates', paginationContainer);
    }

    function renderMismatches() {
        const state = paginationState.mismatches;
        const container = document.getElementById('mismatches-list');
        const paginationContainer = document.getElementById('mismatches-pagination');
        
        if (!container) return;
        
        // Apply column filters if they exist
        let filteredData = state.data;
        if (state.columnFilters) {
            filteredData = state.data.filter(detail => {
                // Check each column filter
                for (const [column, filterValue] of Object.entries(state.columnFilters)) {
                    if (!filterValue) continue; // Skip empty filters
                    
                    const lowerFilter = filterValue.toLowerCase();
                    
                    // Special keyword 'empty' to filter for empty/null/undefined values
                    if (lowerFilter === 'empty') {
                        let matches = false;
                        
                        // Check in source record for empty value
                        if (detail.sourceRecord) {
                            const sourceValue = detail.sourceRecord[column];
                            if (sourceValue === null || sourceValue === undefined || sourceValue === '' || String(sourceValue).trim() === '') {
                                matches = true;
                            }
                        }
                        
                        // Check in target record for empty value
                        if (detail.targetRecord && !matches) {
                            const targetValue = detail.targetRecord[column];
                            if (targetValue === null || targetValue === undefined || targetValue === '' || String(targetValue).trim() === '') {
                                matches = true;
                            }
                        }
                        
                        // Check in special fields for empty value
                        if (column === 'ReconKeyColumn' && (detail.reconKeyColumn === null || detail.reconKeyColumn === undefined || detail.reconKeyColumn === '' || String(detail.reconKeyColumn).trim() === '')) {
                            matches = true;
                        }
                        if (column === 'keyValue' && (detail.keyValue === null || detail.keyValue === undefined || detail.keyValue === '' || String(detail.keyValue).trim() === '')) {
                            matches = true;
                        }
                        // SOURCE/TARGET filter always matches since we show both rows
                        if (column === 'SOURCE/TARGET') {
                            matches = true;
                        }
                        
                        if (!matches) {
                            return false;
                        }
                    } else {
                        let matches = false;
                        
                        // Check in source record
                        if (detail.sourceRecord) {
                            const sourceValue = String(detail.sourceRecord[column] || '').toLowerCase();
                            if (sourceValue.includes(lowerFilter)) {
                                matches = true;
                            }
                        }
                        
                        // Check in target record
                        if (detail.targetRecord) {
                            const targetValue = String(detail.targetRecord[column] || '').toLowerCase();
                            if (targetValue.includes(lowerFilter)) {
                                matches = true;
                            }
                        }
                        
                        // Check in special fields
                        if (column === 'ReconKeyColumn' && detail.reconKeyColumn && String(detail.reconKeyColumn).toLowerCase().includes(lowerFilter)) {
                            matches = true;
                        }
                        if (column === 'keyValue' && detail.keyValue && String(detail.keyValue).toLowerCase().includes(lowerFilter)) {
                            matches = true;
                        }
                        // SOURCE/TARGET filter always matches since we show both rows
                        if (column === 'SOURCE/TARGET') {
                            matches = true;
                        }
                        
                        if (!matches) {
                            return false;
                        }
                    }
                }
                return true;
            });
        }
        
        const start = (state.page - 1) * state.pageSize;
        const end = start + state.pageSize;
        const recordsToShow = filteredData.slice(start, end);
        
        if (recordsToShow.length === 0) {
            container.innerHTML = '<p style="text-align: center; color: #666; padding: 20px;">No data mismatches found</p>';
            paginationContainer.innerHTML = '';
            return;
        }
        
        // Group mismatch details by key to show source and target rows together
        const groupedByKey = new Map();
        recordsToShow.forEach((detail) => {
            const key = detail.keyValue || 'N/A';
            if (!groupedByKey.has(key)) {
                groupedByKey.set(key, []);
            }
            groupedByKey.get(key).push(detail);
        });
        
        // Get all unique columns from source and target records
        const allColumns = new Set();
        recordsToShow.forEach((detail) => {
            if (detail.sourceRecord) {
                Object.keys(detail.sourceRecord).forEach(key => allColumns.add(key));
            }
            if (detail.targetRecord) {
                Object.keys(detail.targetRecord).forEach(key => allColumns.add(key));
            }
        });
        
        // Ensure ReconKeyColumn, keyValue, and SOURCE/TARGET are first, then sort remaining columns alphabetically
        const columns = Array.from(allColumns);
        const priorityColumns = ['ReconKeyColumn', 'keyValue', 'SOURCE/TARGET'];
        const orderedColumns = [
            ...priorityColumns,
            ...columns.filter(col => !priorityColumns.includes(col)).sort()
        ];
        
        let html = `
            <div class="mismatches-table-container">
                <table class="mismatches-table">
                    <thead>
                        <tr>
                            <th>SL No</th>
        `;
        
        orderedColumns.forEach(column => {
            const filterValue = state.columnFilters && state.columnFilters[column] ? state.columnFilters[column] : '';
            const hasFilter = filterValue && filterValue.trim() !== '';
            html += `
                <th>
                    <div>${column}</div>
                    <input type="text" 
                           class="column-filter-input ${hasFilter ? 'column-filter-active' : ''}" 
                           data-column="${column}" 
                           placeholder="Filter... (type 'empty' for null/blank)" 
                           value="${filterValue}"
                           style="margin-top: 5px; padding: 3px; font-size: 12px; width: 90%;">
                </th>
            `;
        });
        
        html += `
                        </tr>
                    </thead>
                    <tbody>
        `;
        
        let slNo = (state.page - 1) * state.pageSize + 1;
        groupedByKey.forEach((details, keyValue) => {
            const firstDetail = details[0];
            const mismatchedColumns = new Set(details.map(d => d.columnName));
            
            // Source row
            html += '<tr>';
            html += `<td>${slNo}</td>`;
            orderedColumns.forEach(column => {
                if (column === 'ReconKeyColumn') {
                    html += `<td>${firstDetail.reconKeyColumn || 'N/A'}</td>`;
                } else if (column === 'keyValue') {
                    html += `<td>${keyValue}</td>`;
                } else if (column === 'SOURCE/TARGET') {
                    html += `<td style="color: #dc3545; font-weight: bold;">SOURCE</td>`;
                } else {
                    const value = firstDetail.sourceRecord && firstDetail.sourceRecord[column] !== undefined ? 
                                  firstDetail.sourceRecord[column] : '';
                    const isMismatched = mismatchedColumns.has(column);
                    if (isMismatched) {
                        html += `<td style="background-color: #ffcdd2; font-weight: bold;">${value}</td>`;
                    } else {
                        html += `<td style="background-color: #c8e6c9;">${value}</td>`;
                    }
                }
            });
            html += '</tr>';
            slNo++;
            
            // Target row
            html += '<tr>';
            html += `<td>${slNo}</td>`;
            orderedColumns.forEach(column => {
                if (column === 'ReconKeyColumn') {
                    html += `<td>${firstDetail.reconKeyColumn || 'N/A'}</td>`;
                } else if (column === 'keyValue') {
                    html += `<td>${keyValue}</td>`;
                } else if (column === 'SOURCE/TARGET') {
                    html += `<td style="color: #28a745; font-weight: bold;">TARGET</td>`;
                } else {
                    const value = firstDetail.targetRecord && firstDetail.targetRecord[column] !== undefined ? 
                                  firstDetail.targetRecord[column] : '';
                    const isMismatched = mismatchedColumns.has(column);
                    if (isMismatched) {
                        html += `<td style="background-color: #ffcdd2; font-weight: bold;">${value}</td>`;
                    } else {
                        html += `<td style="background-color: #c8e6c9;">${value}</td>`;
                    }
                }
            });
            html += '</tr>';
            slNo++;
            
            // Add a separator row
            html += '<tr><td colspan="' + (orderedColumns.length + 1) + '" style="background-color: #f8f9fa; height: 10px;"></td></tr>';
        });
        
        html += `
                    </tbody>
                </table>
            </div>
        `;
        
        container.innerHTML = html;
        
        // Add event listeners for column filter inputs
        container.querySelectorAll('.column-filter-input').forEach(input => {
            input.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    const column = e.target.dataset.column;
                    const value = e.target.value;
                    
                    if (!state.columnFilters) {
                        state.columnFilters = {};
                    }
                    
                    state.columnFilters[column] = value;
                    state.page = 1; // Reset to first page when filtering
                    renderMismatches();
                }
            });
            
            input.addEventListener('input', (e) => {
                if (e.target.value.trim() !== '') {
                    e.target.classList.add('column-filter-active');
                } else {
                    e.target.classList.remove('column-filter-active');
                }
            });
        });
        
        renderPagination('mismatches', paginationContainer);
    }

    function renderPagination(type, container) {
        if (!container) return;
        
        const state = paginationState[type];
        const totalPages = Math.ceil(state.data.length / state.pageSize);
        const startRecord = (state.page - 1) * state.pageSize + 1;
        const endRecord = Math.min(state.page * state.pageSize, state.data.length);
        
        let html = `
            <span class="pagination-info">Showing ${startRecord}-${endRecord} of ${state.data.length} records</span>
        `;
        
        if (totalPages > 1) {
            html += `
                <button class="btn-load-more" onclick="changePage('${type}', ${state.page - 1})" ${state.page === 1 ? 'disabled' : ''}>Previous</button>
                <button class="btn-load-more" onclick="changePage('${type}', ${state.page + 1})" ${state.page === totalPages ? 'disabled' : ''}>Next</button>
            `;
        }
        
        container.innerHTML = html;
    }

    window.changePage = function(type, newPage) {
        const state = paginationState[type];
        const totalPages = Math.ceil(state.data.length / state.pageSize);
        
        if (newPage >= 1 && newPage <= totalPages) {
            state.page = newPage;
            if (type === 'missing') renderMissingRecords();
            else if (type === 'extra') renderExtraRecords();
            else if (type === 'duplicates') renderDuplicates();
            else renderMismatches();
        }
    }

    function parseMismatchDetail(detail) {
        // Parse mismatch detail string into structured format
        const keyMatch = detail.match(/Key '([^']+)'|found in (source|target)/);
        const key = keyMatch ? keyMatch[1] || 'Unknown' : 'Unknown';
        
        const fields = [];
        const fieldMatches = detail.matchAll(/(\w+)=\[source='([^']*)', target='([^']*)'\]/g);
        
        for (const match of fieldMatches) {
            fields.push({
                name: match[1],
                source: match[2],
                target: match[3]
            });
        }
        
        return { key, fields };
    }

    function displayError(message) {
        resultsSection.style.display = 'block';
        resultsContent.innerHTML = `<div class="error-message">${message}</div>`;
        reportSection.style.display = 'none';
    }

    function downloadReport(result) {
        let report = `DATA RECONCILIATION REPORT
Generated: ${new Date().toISOString()}
================================================================================

EXECUTIVE SUMMARY
----------------
Source File Record Count:     ${result.sourceRecordCount.toLocaleString()}
Target File Record Count:     ${result.targetRecordCount.toLocaleString()}
Total Matching Records:      ${result.matchingRecords.toLocaleString()}
Match Percentage:             ${result.matchPercentage.toFixed(2)}%
Records Missing in Target:   ${result.missingInTarget.toLocaleString()}
Extra Records in Target:     ${result.extraInTarget.toLocaleString()}
Records with Data Mismatches: ${result.mismatchedRecords.toLocaleString()}
Total Processing Time:        ${result.processingTimeMs} ms

RECONCILIATION CONFIGURATION
--------------------------
Key Column Used:             ${result.keyColumn || 'Auto-detected'}
Delimiter:                  ${result.delimiter || 'Default (,)'}
Headers Present:              ${result.hasHeaders ? 'Yes' : 'No'}

RECONCILIATION STATUS
--------------------
`;

        if (result.matchingRecords === result.sourceRecordCount && 
            result.matchingRecords === result.targetRecordCount && 
            result.mismatchedRecords === 0) {
            report += '✅ PERFECT MATCH - All records match perfectly!';
        } else {
            report += '❌ MISMATCHES DETECTED - Requires attention';
            
            if (result.missingInTarget > 0) {
                report += `\n⚠️  ${result.missingInTarget.toLocaleString()} records are missing in target file`;
            }
            if (result.extraInTarget > 0) {
                report += `\n⚠️  ${result.extraInTarget.toLocaleString()} extra records found in target file`;
            }
            if (result.mismatchedRecords > 0) {
                report += `\n⚠️  ${result.mismatchedRecords.toLocaleString()} records have data mismatches`;
            }
        }

        report += '\n\nDETAILED MISMATCHES\n------------------\n';
        
        if (result.sourceOnlyRecords && result.sourceOnlyRecords.length > 0) {
            report += `Source Only Records: ${result.sourceOnlyRecords.length}\n\n`;
            result.sourceOnlyRecords.forEach((record, index) => {
                report += `Source Only Record #${index + 1}:\n`;
                Object.entries(record).forEach(([key, value]) => {
                    report += `  ${key}: ${value}\n`;
                });
                report += '\n';
            });
        }

        if (result.targetOnlyRecords && result.targetOnlyRecords.length > 0) {
            report += `Target Only Records: ${result.targetOnlyRecords.length}\n\n`;
            result.targetOnlyRecords.forEach((record, index) => {
                report += `Target Only Record #${index + 1}:\n`;
                Object.entries(record).forEach(([key, value]) => {
                    report += `  ${key}: ${value}\n`;
                });
                report += '\n';
            });
        }
        
        if (result.duplicateRecords && result.duplicateRecords.length > 0) {
            report += `Duplicate Records: ${result.duplicateRecords.length}\n\n`;
            result.duplicateRecords.forEach((record, index) => {
                report += `Duplicate Record #${index + 1}:\n`;
                Object.entries(record).forEach(([key, value]) => {
                    report += `  ${key}: ${value}\n`;
                });
                report += '\n';
            });
        }
        
        if (result.mismatchDetails && result.mismatchDetails.length > 0) {
            report += `Data Mismatches: ${result.mismatchDetails.length}\n\n`;
            report += 'Column Name    | Source Value         | Target Value         | Mismatch %\n';
            report += '---------------+---------------------+---------------------+------------\n';
            result.mismatchDetails.forEach((detail) => {
                const colName = (detail.columnName || 'N/A').padEnd(14);
                const srcValue = (detail.sourceValue || 'N/A').padEnd(20);
                const tgtValue = (detail.targetValue || 'N/A').padEnd(20);
                const mismatchPct = (detail.mismatchPercentage ? detail.mismatchPercentage.toFixed(2) + '%' : 'N/A').padEnd(11);
                report += `${colName} | ${srcValue} | ${tgtValue} | ${mismatchPct}\n`;
            });
        } else if (result.sourceOnlyRecords.length === 0 && result.targetOnlyRecords.length === 0 && (!result.duplicateRecords || result.duplicateRecords.length === 0)) {
            report += 'No mismatches found.\n';
        }

        report += '\n================================================================================\nEND OF REPORT\nGenerated by Data Reconciliation Service\n================================================================================';

        const blob = new Blob([report], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `reconciliation_report_${Date.now()}.txt`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }
});
