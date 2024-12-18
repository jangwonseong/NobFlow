package com.samsa;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.samsa.core.Message;
import com.samsa.node.inout.ReadFileNode;
import com.samsa.node.inout.WriteFileNode;

import lombok.extern.slf4j.Slf4j;

/**
 * ReadWrite 클래스는 파일 읽기/쓰기 기능을 테스트하는 실행 클래스입니다.
 * ReadFileNode와 WriteFileNode를 사용하여 파일 입출력 작업을 수행합니다.
 * 
 * 이 클래스는 파일 읽기/쓰기 노드의 기본적인 사용법을 보여주는
 * 예제로 활용될 수 있습니다.
 *
 * @author samsa
 * @version 1.0
 */
@Slf4j
public class ReadWrite {
    /**
     * 파일 읽기/쓰기 테스트를 실행하는 메인 메소드입니다.
     *
     * @param args 명령행 인자 (사용하지 않음)
     */
    public static void main(String[] args) {
        Path filePath = Paths.get("./log/log2.log").toAbsolutePath();

        WriteFileNode writer = new WriteFileNode(filePath, true);
        ReadFileNode reader = new ReadFileNode(filePath, true);

        // 파일에 내용 쓰기
        Message writeMessage = new Message("This is a test message");
        writer.onMessage(writeMessage);

        // 파일 내용 읽기
        Message readMessage = new Message("");
        reader.onMessage(readMessage);

        // 읽은 내용 출력
        Object readContent = readMessage.getPayload();
        if (readContent instanceof List) {
            List<String> lines = (List<String>) readContent;
            log.info("Read file contents:");
            for (String line : lines) {
                log.info(line);
            }
        } else {
            log.info("Read content: {}", readContent);
        }
    }
}