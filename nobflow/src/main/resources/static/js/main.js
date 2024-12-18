class FlowViewer {
    constructor() {
        this.initializeElements();
        this.bindEvents();
        this.logWebSocket = null;
        this.flowData = null;
    }

    initializeElements() {
        this.fileInput = document.getElementById('flowFile');
        this.loadBtn = document.getElementById('loadBtn');
        this.runBtn = document.getElementById('runBtn');
        this.stopBtn = document.getElementById('stopBtn');
        this.flowDescription = document.getElementById('flowDescription');
        this.logOutput = document.getElementById('logOutput');
        this.searchLog = document.getElementById('searchLog');
        this.logFilters = document.querySelectorAll('.log-filters input[type="checkbox"]');
    }

    bindEvents() {
        this.loadBtn.addEventListener('click', () => this.loadFlow());
        this.runBtn.addEventListener('click', () => this.runFlow());
        this.stopBtn.addEventListener('click', () => this.stopFlow());
        this.searchLog.addEventListener('input', () => this.filterLogs());
        this.logFilters.forEach(filter => {
            filter.addEventListener('change', () => this.filterLogs());
        });
    }

    async loadFlow() {
        const file = this.fileInput.files[0];
        if (!file) return;

        try {
            const text = await file.text();
            this.flowData = JSON.parse(text);
            this.displayFlowInfo();
            this.runBtn.disabled = false;
        } catch (error) {
            this.addLog('ERROR', `Flow 파일 로드 실패: ${error.message}`);
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

    runFlow() {
        if (!this.flowData) return;
    
        fetch('http://localhost:8081/api/flow/run', { // Fix the URL
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(this.flowData)
        });
    
        // Update WebSocket URL to match port
        this.logWebSocket = new WebSocket('ws://localhost:8081/logs');

        this.logWebSocket.onmessage = (event) => {
            const logData = JSON.parse(event.data);
            this.addLog(logData.level, logData.message);
        };

        this.runBtn.disabled = true;
        this.stopBtn.disabled = false;
        this.addLog('INFO', 'Flow 실행 시작');
    }

    stopFlow() {
        if (this.logWebSocket) {
            this.logWebSocket.close();
        }
        
        fetch('http://localhost:8081/api/flow/stop', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        }).then(response => {
            if (response.ok) {
                this.runBtn.disabled = false;
                this.stopBtn.disabled = true;
                this.addLog('INFO', 'Flow 실행 중지');
            } else {
                return response.text().then(text => {
                    throw new Error(text);
                });
            }
        }).catch(error => {
            this.addLog('ERROR', `Flow 중지 실패: ${error.message}`);
        });
    }
    
    

    addLog(level, message) {
        const logEntry = document.createElement('div');
        logEntry.className = `log-${level.toLowerCase()}`;
        logEntry.textContent = `[${new Date().toISOString()}] ${level}: ${message}`;
        this.logOutput.appendChild(logEntry);
        this.logOutput.scrollTop = this.logOutput.scrollHeight;
        this.filterLogs();
    }

    filterLogs() {
        const searchText = this.searchLog.value.toLowerCase();
        const enabledLevels = Array.from(this.logFilters)
            .filter(f => f.checked)
            .map(f => f.value.toLowerCase());

        Array.from(this.logOutput.children).forEach(log => {
            const level = log.className.replace('log-', '');
            const text = log.textContent.toLowerCase();
            const matchesSearch = searchText === '' || text.includes(searchText);
            const matchesLevel = enabledLevels.includes(level);
            log.style.display = matchesSearch && matchesLevel ? '' : 'none';
        });
    }
}

// 페이지 로드 시 FlowViewer 인스턴스 생성
document.addEventListener('DOMContentLoaded', () => {
    new FlowViewer();
});