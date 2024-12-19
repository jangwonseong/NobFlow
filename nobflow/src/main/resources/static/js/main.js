class FlowViewer {
    constructor() {
        this.flowData = null;
        this.initializeElements();
        this.bindEvents();
    }

    initializeElements() {
        this.fileInput = document.getElementById('flowFile');
        this.loadBtn = document.getElementById('loadBtn');
        this.runBtn = document.getElementById('runBtn');
        this.stopBtn = document.getElementById('stopBtn');
        this.flowDescription = document.getElementById('flowDescription');
        this.analyzeBtn = document.getElementById('analyzeBtn');
        
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
            const response = await fetch('http://localhost:8081/api/logs/analyze');
            const data = await response.json();
            if (data.error) {
                console.error('로그 분석 오류:', data.error);
                return;
            }
            this.displayAnalysisResults(data);
        } catch (error) {
            console.error('로그 분석 실패:', error);
        }
    }
    

    displayAnalysisResults(data) {
        const resultDiv = document.getElementById('analysisResult');
        resultDiv.innerHTML = '';

        // 레벨별 로그 표시
        if (data.logsByLevel) {
            const levelDiv = document.createElement('div');
            levelDiv.innerHTML = '<h3>레벨별 로그</h3>';
            Object.entries(data.logsByLevel).forEach(([level, logs]) => {
                levelDiv.innerHTML += `<p>${level}: ${logs.length}개</p>`;
            });
            resultDiv.appendChild(levelDiv);
        }

        // 에러 로그 표시
        if (data.errorLogs) {
            const errorDiv = document.createElement('div');
            errorDiv.innerHTML = '<h3>에러 로그</h3>';
            data.errorLogs.forEach(log => {
                errorDiv.innerHTML += `<p class="error-log">${log}</p>`;
            });
            resultDiv.appendChild(errorDiv);
        }

        // MQTT 로그 표시
        if (data.mqttLogs) {
            const mqttDiv = document.createElement('div');
            mqttDiv.innerHTML = '<h3>MQTT 로그</h3>';
            data.mqttLogs.forEach(log => {
                mqttDiv.innerHTML += `<p>${log}</p>`;
            });
            resultDiv.appendChild(mqttDiv);
        }

        // Modbus 로그 표시
        if (data.modbusLogs) {
            const modbusDiv = document.createElement('div');
            modbusDiv.innerHTML = '<h3>Modbus 로그</h3>';
            data.modbusLogs.forEach(log => {
                modbusDiv.innerHTML += `<p>${log}</p>`;
            });
            resultDiv.appendChild(modbusDiv);
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new FlowViewer();
});
