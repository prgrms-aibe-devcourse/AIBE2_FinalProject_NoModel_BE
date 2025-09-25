package com.example.nomodel.compose.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageCompositor {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Python 스크립트 경로
    @Value("${python.script.path:/app/compose/application/service/UseGeminiApi.py}")
    private String pythonScriptPath;
    
    // 임시 파일 저장 디렉토리
    private static final String TEMP_DIR = "temp";

    /**
     * Gemini API를 사용하여 두 이미지를 합성합니다.
     * @param productImage 제품 이미지 (바이트 배열)
     * @param modelImage 모델 이미지 (바이트 배열)
     * @param customPrompt 사용자 정의 프롬프트 (null이면 기본 프롬프트 사용)
     * @return 합성된 이미지의 바이트 배열
     * @throws Exception 합성 중 오류 발생 시
     */
    public byte[] composite(byte[] productImage, byte[] modelImage, String customPrompt) throws Exception {
        String sessionId = UUID.randomUUID().toString();
        
        try {
            log.info("Starting image composition with session ID: {}", sessionId);
            
            // 임시 디렉토리 생성
            createTempDirectoryIfNotExists();
            
            // 임시 파일 경로 생성
            String productImagePath = TEMP_DIR + "/product_" + sessionId + ".png";
            String modelImagePath = TEMP_DIR + "/model_" + sessionId + ".png";
            String outputImagePath = TEMP_DIR + "/result_" + sessionId + ".png";
            
            try {
                // 바이트 배열을 임시 파일로 저장
                Files.write(Paths.get(productImagePath), productImage);
                Files.write(Paths.get(modelImagePath), modelImage);
                
                log.info("Temporary files created for session: {}", sessionId);
                
                // Python 스크립트 실행
                byte[] result = executePythonScript(productImagePath, modelImagePath, customPrompt, outputImagePath);
                
                log.info("Image composition completed successfully for session: {}", sessionId);
                return result;
                
            } finally {
                // 임시 파일 정리
                cleanupTempFiles(productImagePath, modelImagePath, outputImagePath);
                log.info("Temporary files cleaned up for session: {}", sessionId);
            }
            
        } catch (Exception e) {
            log.error("Error during image composition for session: {}", sessionId, e);
            throw new Exception("Failed to composite images: " + e.getMessage(), e);
        }
    }

    /**
     * 기본 프롬프트로 이미지 합성
     */
    public byte[] composite(byte[] productImage, byte[] modelImage) throws Exception {
        return composite(productImage, modelImage, null);
    }

    /**
     * Python 스크립트를 실행하여 이미지 합성
     */
    private byte[] executePythonScript(String productImagePath, String modelImagePath, 
                                     String customPrompt, String outputImagePath) throws Exception {
        
        // 커스텀 프롬프트가 null이면 "null" 문자열로 전달
        String promptArg = (customPrompt != null && !customPrompt.trim().isEmpty()) ? customPrompt : "null";
        
        log.info("Executing Python script with arguments: product={}, model={}, prompt={}, output={}", 
                productImagePath, modelImagePath, promptArg, outputImagePath);
        
        // python3 먼저 시도, 실패하면 python 시도
        String[] pythonCommands = {"python3", "python"};
        Exception lastException = null;
        
        for (String pythonCommand : pythonCommands) {
            try {
                log.info("Trying Python command: {}", pythonCommand);
                
                // Python 명령어 구성
                ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonCommand,
                    pythonScriptPath,
                    productImagePath,
                    modelImagePath,
                    promptArg,
                    outputImagePath
                );
                
                // 작업 디렉토리 설정
                processBuilder.directory(new File("."));
                
                // 프로세스 실행
                Process process = processBuilder.start();
                
                // 표준 출력 읽기
                String output = readProcessOutput(process.getInputStream());
                
                // 표준 에러 읽기
                String error = readProcessOutput(process.getErrorStream());
                
                // 프로세스 완료 대기 (최대 5분)
                boolean finished = process.waitFor(5, TimeUnit.MINUTES);
                
                if (!finished) {
                    process.destroyForcibly();
                    throw new Exception("Python script execution timed out");
                }
                
                int exitCode = process.exitValue();
                
                // exit code 9009는 명령어를 찾을 수 없음을 의미 (Windows)
                // 이 경우 다음 Python 명령어를 시도
                if (exitCode == 9009) {
                    log.warn("Python command '{}' not found (exit code 9009), trying next command", pythonCommand);
                    lastException = new Exception("Python command '" + pythonCommand + "' not found");
                    continue;
                }
                
                if (exitCode != 0) {
                    log.error("Python script failed with exit code: {}, error: {}", exitCode, error);
                    throw new Exception("Python script execution failed: " + error);
                }
                
                // Python 스크립트의 JSON 출력 파싱
                JsonNode result = objectMapper.readTree(output);
                
                if (!result.get("success").asBoolean()) {
                    String errorMessage = result.get("error").asText();
                    throw new Exception("Image composition failed: " + errorMessage);
                }
                
                // 결과 이미지 파일 읽기
                Path outputPath = Paths.get(outputImagePath);
                if (!Files.exists(outputPath)) {
                    throw new Exception("Output image file not found: " + outputImagePath);
                }
                
                byte[] resultImage = Files.readAllBytes(outputPath);
                log.info("Successfully read result image using '{}', size: {} bytes", pythonCommand, resultImage.length);
                
                return resultImage;
                
            } catch (Exception e) {
                log.warn("Failed to execute Python script with command '{}': {}", pythonCommand, e.getMessage());
                lastException = e;
                
                // exit code 9009가 아닌 다른 오류인 경우, 다음 명령어를 시도하지 않고 바로 예외 발생
                if (e.getMessage() != null && !e.getMessage().contains("not found") && !e.getMessage().contains("9009")) {
                    break;
                }
            }
        }
        
        // 모든 Python 명령어가 실패한 경우
        log.error("All Python commands failed. Last error: {}", lastException != null ? lastException.getMessage() : "Unknown error");
        throw new Exception("Failed to execute Python script with any available Python command: " + 
                          (lastException != null ? lastException.getMessage() : "Unknown error"), lastException);
    }

    /**
     * 프로세스의 출력 스트림을 읽어서 문자열로 반환
     */
    private String readProcessOutput(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString().trim();
    }

    /**
     * 임시 디렉토리 생성
     */
    private void createTempDirectoryIfNotExists() throws IOException {
        Path tempDir = Paths.get(TEMP_DIR);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
            log.info("Created temporary directory: {}", TEMP_DIR);
        }
    }

    /**
     * 임시 파일들을 정리
     */
    private void cleanupTempFiles(String... filePaths) {
        for (String filePath : filePaths) {
            try {
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    Files.delete(path);
                    log.debug("Deleted temporary file: {}", filePath);
                }
            } catch (IOException e) {
                log.warn("Failed to delete temporary file: {}", filePath, e);
            }
        }
    }
}
