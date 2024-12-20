class FlowViewer {
    constructor() {
        this.initializeElements();
        this.bindEvents();
        this.setupWebSocket();
        this.logFilters = {
            INFO: true,
            DEBUG: true,
            WARN: true,
            ERROR: true
        };
    }

    initializeElements() {
        this.fileInput = document.getElementById('flowFile');
        this.loadBtn = document.getElementById('loadBtn');
        this.runBtn = document.getElementById('runBtn');
        this.stopBtn = document.getElementById('stopBtn');
        this.flowDescription = document.getElementById('flowDescription');
        this.analyzeBtn = document.getElementById('analyzeBtn');
        this.logContainer = document.getElementById('logContainer');
        this.searchInput = document.getElementById('searchLog');
        this.logFile = document.getElementById('logFile');
        this.loadLogBtn = document.getElementById('loadLogBtn');
        
        // 로그 필터 체크박스들
        this.filterCheckboxes = document.querySelectorAll('.log-filters input[type="checkbox"]');
        
        // 초기 버튼 상태 설정
        this.runBtn.disabled = true;
        this.stopBtn.disabled = true;
    }

    bindEvents() {
        this.loadBtn.addEventListener('click', () => this.loadFlow());
        this.runBtn.addEventListener('click', () => this.runFlow());
        this.stopBtn.addEventListener('click', () => this.stopFlow());
        
        // 로그 분석 버튼 이벤트 추가
        if (this.analyzeBtn) {
            this.analyzeBtn.addEventListener('click', () => this.analyzeLog());
        }
        this.loadLogBtn.addEventListener('click', () => this.loadLogFile());
        this.searchInput.addEventListener('input', () => this.filterLogs());
        this.filterCheckboxes.forEach(checkbox => {
            checkbox.addEventListener('change', () => {
                this.logFilters[checkbox.value] = checkbox.checked;
                this.filterLogs();
            });
        });
    }

    setupWebSocket() {
        this.ws = new WebSocket('ws://localhost:8081/logs/stream');
        
        this.ws.onmessage = (event) => {
            const logEntry = this.createLogEntry(event.data);
            if (this.shouldShowLog(logEntry)) {
                this.logContainer.appendChild(logEntry);
                this.scrollToBottom();
            }
        };

        this.ws.onclose = () => console.log('WebSocket 연결이 닫혔습니다');
        this.ws.onerror = (error) => console.error('WebSocket 오류:', error);
    }

    createLogEntry(logText) {
        const logEntry = document.createElement('div');
        logEntry.className = 'log-entry';
        logEntry.textContent = logText;

        // 로그 레벨 스타일 적용
        const logLevel = this.getLogLevel(logText);
        if (logLevel) {
            logEntry.classList.add(`log-${logLevel}`);
            logEntry.dataset.level = logLevel;
        }

        return logEntry;
    }

    getLogLevel(logText) {
        if (logText.includes('[INFO]')) return 'INFO';
        if (logText.includes('[DEBUG]')) return 'DEBUG';
        if (logText.includes('[WARN]')) return 'WARN';
        if (logText.includes('[ERROR]')) return 'ERROR';
        return 'INFO'; // 기본값
    }

    shouldShowLog(logEntry) {
        const level = logEntry.dataset.level;
        const searchText = this.searchInput.value.toLowerCase();
        const matchesFilter = !level || this.logFilters[level];
        const matchesSearch = !searchText || logEntry.textContent.toLowerCase().includes(searchText);
        return matchesFilter && matchesSearch;
    }

    filterLogs() {
        const searchText = this.searchInput.value.toLowerCase();
        const logEntries = this.logContainer.getElementsByClassName('log-entry');
        
        Array.from(logEntries).forEach(entry => {
            const text = entry.textContent.toLowerCase();
            const logLevel = this.getLogLevel(entry.textContent);
            const matchesSearch = !searchText || text.includes(searchText);
            const matchesFilter = this.logFilters[logLevel];
            
            entry.style.display = (matchesSearch && matchesFilter) ? '' : 'none';
        });
    }

    scrollToBottom() {
        this.logContainer.scrollTop = this.logContainer.scrollHeight;
    }

    async loadFlow() {
        const file = this.fileInput.files[0];
        if (!file) {
            console.error('파일을 선택해주세요.');
            return;
        }

        try {
            const text = await file.text();
            this.flowData = JSON.parse(text);
            this.displayFlowInfo();
            this.runBtn.disabled = false;
            console.log('Flow 파일이 성공적으로 로드되었습니다.');
        } catch (error) {
            console.error(`Flow 파일 로드 실패: ${error.message}`);
        }
    }

    displayFlowInfo() {
        if (!this.flowData) return;
        const nodesCount = this.flowData.nodes?.length || 0;
        const connectionsCount = this.flowData.connections?.length || 0;
        this.flowDescription.innerHTML = `
            <p>노드 수: ${nodesCount}</p>
            <p>연결 수: ${connectionsCount}</p>
            <pre>${JSON.stringify(this.flowData, null, 2)}</pre>
        `;
    }

    async runFlow() {
        if (!this.flowData) {
            console.error('Flow 데이터가 없습니다.');
            return;
        }

        try {
            const response = await fetch('http://localhost:8081/api/flow/run', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(this.flowData)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            this.runBtn.disabled = true;
            this.stopBtn.disabled = false;
            console.log('Flow 실행이 시작되었습니다.');
        } catch (error) {
            console.error(`Flow 실행 실패: ${error.message}`);
        }
    }

    async stopFlow() {
        try {
            const response = await fetch('http://localhost:8081/api/flow/stop', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(await response.text());
            }

            this.runBtn.disabled = false;
            this.stopBtn.disabled = true;
            console.log('Flow 실행이 중지되었습니다.');
        } catch (error) {
            console.error(`Flow 중지 실패: ${error.message}`);
        }
    }

    async analyzeLog() {
        try {
            const response = await fetch('http://localhost:8081/api/logs/analysis');
            const data = await response.json();
            this.displayAnalysisResults(data);
        } catch (error) {
            console.error('로그 분석 오류:', error);
        }
    }

    displayAnalysisResults(data) {
        const analysisHtml = `
            <div class="analysis-results">
                <h3>로그 분석 결과</h3>
                <div class="stat-box">
                    <h4>로그 레벨 분포</h4>
                    <p>INFO: ${data.levelCounts?.INFO || 0}</p>
                    <p>DEBUG: ${data.levelCounts?.DEBUG || 0}</p>
                    <p>WARN: ${data.levelCounts?.WARN || 0}</p>
                    <p>ERROR: ${data.levelCounts?.ERROR || 0}</p>
                </div>
                <div class="stat-box">
                    <h4>에러율</h4>
                    <p>${data.errorRate?.toFixed(2)}%</p>
                </div>
                <div class="stat-box">
                    <h4>MQTT 메시지</h4>
                    <p>총 개수: ${data.mqttMessageCount || 0}</p>
                </div>
            </div>
        `;

        const analysisContainer = document.createElement('div');
        analysisContainer.innerHTML = analysisHtml;
        
        // 기��� 분석 결과 제거
        const oldAnalysis = document.querySelector('.analysis-results');
        if (oldAnalysis) oldAnalysis.remove();
        
        // 새 분석 결과 추가
        this.logContainer.parentElement.insertBefore(
            analysisContainer, 
            this.logContainer
        );
    }

    async loadLogFile() {
        const file = this.logFile.files[0];
        if (!file) {
            alert('로그 파일을 선택해주세요.');
            return;
        }

        try {
            const formData = new FormData();
            formData.append('logFile', file);

            const response = await fetch('http://localhost:8081/api/logs/upload', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) throw new Error('로그 파일 업로드 실패');

            // 로그 분석 요청
            this.analyzeLog();
        } catch (error) {
            console.error('로그 파일 처리 오류:', error);
            alert('로그 파일 처리 중 오류가 발생했습니다.');
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new FlowViewer();
});
