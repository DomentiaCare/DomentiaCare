// LlamaAnalysisService.java
// 경로: ChatApp/app/src/main/java/com/quicinc/chatapp/LlamaAnalysisService.java

package com.quicinc.chatapp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.system.Os;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LlamaAnalysisService extends Service {

    private static final String TAG = "LlamaAnalysisService";

    // Binder for clients
    private final LocalBinder binder = new LocalBinder();

    // GenieWrapper instance
    private GenieWrapper genieWrapper;
    private boolean isInitialized = false;
    private ExecutorService executorService;

    // Service initialization state
    private boolean isInitializing = false;

    static {
        System.loadLibrary("chatapp");
    }

    public class LocalBinder extends Binder {
        public LlamaAnalysisService getService() {
            return LlamaAnalysisService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LlamaAnalysisService created");
        executorService = Executors.newSingleThreadExecutor();

        // 백그라운드에서 초기화 시작
        initializeService();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Client bound to LlamaAnalysisService");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Client unbound from LlamaAnalysisService");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "LlamaAnalysisService destroyed");

        if (executorService != null) {
            executorService.shutdown();
        }

        // GenieWrapper는 finalize()에서 자동으로 정리됨
        genieWrapper = null;
    }

    /**
     * 서비스 초기화 - MainActivity 로직 이식
     */
    private void initializeService() {
        if (isInitializing || isInitialized) {
            return;
        }

        isInitializing = true;

        executorService.execute(() -> {
            try {
                Log.d(TAG, "Starting LLaMA service initialization...");

                // 1. SoC 호환성 체크
                String htpConfigPath = checkSocCompatibilityAndGetConfig();
                if (htpConfigPath == null) {
                    Log.e(TAG, "Unsupported device for LLaMA");
                    return;
                }

                // 2. 에셋 복사
                copyAssetsToCache();

                // 3. 환경 설정
                setupEnvironment();

                // 4. 모델 초기화
                initializeModel(htpConfigPath);

                isInitialized = true;
                isInitializing = false;
                Log.i(TAG, "LLaMA service initialization completed successfully");

            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize LLaMA service: " + e.toString());
                isInitializing = false;
            }
        });
    }

    /**
     * SoC 호환성 체크 및 HTP 설정 파일 경로 반환
     */
    private String checkSocCompatibilityAndGetConfig() {
        HashMap<String, String> supportedSocModel = new HashMap<>();
        supportedSocModel.put("SM8750", "qualcomm-snapdragon-8-elite.json");
        supportedSocModel.put("SM8650", "qualcomm-snapdragon-8-gen3.json");
        supportedSocModel.put("QCS8550", "qualcomm-snapdragon-8-gen2.json");

        String socModel = android.os.Build.SOC_MODEL;
        if (!supportedSocModel.containsKey(socModel)) {
            Log.e(TAG, "Unsupported SoC: " + socModel);
            return null;
        }

        String externalDir = getExternalCacheDir().getAbsolutePath();
        Path htpConfigPath = Paths.get(externalDir, "htp_config", supportedSocModel.get(socModel));
        return htpConfigPath.toString();
    }

    /**
     * 에셋을 외부 캐시로 복사 - MainActivity 로직 이식
     */
    private void copyAssetsToCache() throws IOException {
        String externalDir = getExternalCacheDir().getAbsolutePath();

        // models와 htp_config 디렉토리 복사
        copyAssetsDir("models", externalDir);
        copyAssetsDir("htp_config", externalDir);

        Log.d(TAG, "Assets copied to external cache");
    }

    /**
     * 환경 변수 설정 - Conversation 로직 이식
     */
    private void setupEnvironment() {
        try {
            String nativeLibPath = getApplicationContext().getApplicationInfo().nativeLibraryDir;
            Os.setenv("ADSP_LIBRARY_PATH", nativeLibPath, true);
            Os.setenv("LD_LIBRARY_PATH", nativeLibPath, true);
            Log.d(TAG, "Environment variables set successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set environment variables: " + e.toString());
            throw new RuntimeException(e);
        }
    }

    /**
     * GenieWrapper 초기화
     */
    private void initializeModel(String htpConfigPath) {
        try {
            String externalCacheDir = getExternalCacheDir().getAbsolutePath();
            String modelDir = Paths.get(externalCacheDir, "models", "llama3_2_3b").toString();

            Log.d(TAG, "Initializing GenieWrapper with model: " + modelDir);
            Log.d(TAG, "HTP config: " + htpConfigPath);

            genieWrapper = new GenieWrapper(modelDir, htpConfigPath);
            Log.i(TAG, "GenieWrapper initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize GenieWrapper: " + e.toString());
            throw new RuntimeException(e);
        }
    }

    /**
     * 일정 분석 메인 메서드 (실제로는 모든 종류의 쿼리 처리)
     */
    public void analyzeSchedule(String inputText, ScheduleAnalysisCallback callback) {
        if (!isInitialized) {
            if (isInitializing) {
                callback.onError("서비스가 초기화 중입니다. 잠시 후 다시 시도해주세요.");
            } else {
                callback.onError("서비스 초기화에 실패했습니다.");
            }
            return;
        }

        if (inputText == null || inputText.trim().isEmpty()) {
            callback.onError("입력 텍스트가 비어있습니다.");
            return;
        }

        executorService.execute(() -> {
            try {
                // 일정 분석이 아닌 일반 쿼리의 경우 프롬프트를 단순화
                String prompt;
                if (isScheduleQuery(inputText)) {
                    prompt = createSchedulePrompt(inputText.trim());
                } else {
                    prompt = inputText.trim(); // 일반 질문은 그대로 전달
                }

                Log.d(TAG, "Processing query: " + inputText);

                final StringBuilder responseBuilder = new StringBuilder();

                genieWrapper.getResponseForPrompt(prompt, new StringCallback() {
                    @Override
                    public void onNewString(String response) {
                        responseBuilder.append(response);

                        // 응답이 완료된 것으로 보이면 콜백 호출
                        String fullResponse = responseBuilder.toString().trim();
                        if (isResponseComplete(fullResponse, isScheduleQuery(inputText))) {
                            processResponse(fullResponse, inputText, callback);
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error during query processing: " + e.toString());
                callback.onError("쿼리 처리 중 오류가 발생했습니다: " + e.getMessage());
            }
        });
    }

    /**
     * 일정 관련 쿼리인지 확인
     */
    private boolean isScheduleQuery(String query) {
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("schedule") ||
                lowerQuery.contains("appointment") ||
                lowerQuery.contains("meeting") ||
                lowerQuery.contains("일정") ||
                lowerQuery.contains("예약") ||
                lowerQuery.contains("약속");
    }

    /**
     * 일정 분석용 프롬프트 생성
     */
    private String createSchedulePrompt(String userText) {
        return "다음 텍스트에서 일정 정보를 추출하여 정확한 JSON 형식으로 반환해주세요.\n\n" +
                "형식 예시:\n" +
                "일정이 있는 경우: {\"date\":\"2025-05-23\", \"time\":\"14:00\", \"title\":\"병원 예약\", \"location\":\"서울대병원\"}\n" +
                "일정이 없는 경우: {\"no_schedule\": true}\n\n" +
                "규칙:\n" +
                "- 날짜는 YYYY-MM-DD 형식\n" +
                "- 시간은 HH:MM 형식 (24시간)\n" +
                "- 정확한 JSON만 반환\n" +
                "- 추가 설명 없이 JSON만 출력\n\n" +
                "분석할 텍스트: " + userText;
    }

    /**
     * 응답 완료 여부 확인
     */
    private boolean isResponseComplete(String response, boolean isScheduleQuery) {
        if (isScheduleQuery) {
            // 일정 분석의 경우 JSON 형태 확인
            return response.contains("}") && (response.contains("date") || response.contains("no_schedule"));
        } else {
            // 일반 쿼리의 경우 문장이 완료되었는지 확인
            return response.length() > 10 &&
                    (response.endsWith(".") || response.endsWith("!") || response.endsWith("?") ||
                            response.contains("\n") || response.length() > 100);
        }
    }

    /**
     * 응답 처리
     */
    private void processResponse(String response, String originalQuery, ScheduleAnalysisCallback callback) {
        try {
            if (isScheduleQuery(originalQuery)) {
                // 일정 분석 응답 처리
                String jsonResponse = extractJson(response);

                if (jsonResponse.contains("no_schedule")) {
                    callback.onNoScheduleFound();
                } else if (jsonResponse.contains("date")) {
                    callback.onScheduleFound(jsonResponse);
                } else {
                    callback.onNoScheduleFound();
                }
            } else {
                // 일반 쿼리 응답 처리
                callback.onScheduleFound(response.trim());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing response: " + e.toString());
            callback.onError("응답 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 응답에서 JSON 부분 추출
     */
    private String extractJson(String response) {
        // 첫 번째 { 부터 마지막 } 까지 추출
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1);
        }

        return response.trim();
    }

    /**
     * 서비스 초기화 상태 확인
     */
    public boolean isServiceReady() {
        return isInitialized;
    }

    /**
     * 서비스 초기화 진행 중 여부 확인
     */
    public boolean isServiceInitializing() {
        return isInitializing;
    }

    // MainActivity에서 가져온 유틸리티 메서드들

    /**
     * 에셋 디렉토리 복사
     */
    private void copyAssetsDir(String inputAssetRelPath, String outputPath) throws IOException {
        File outputAssetPath = new File(Paths.get(outputPath, inputAssetRelPath).toString());

        String[] subAssetList = getAssets().list(inputAssetRelPath);
        if (subAssetList.length == 0) {
            if (!outputAssetPath.exists()) {
                copyFile(inputAssetRelPath, outputAssetPath);
            }
            return;
        }

        if (!outputAssetPath.exists()) {
            outputAssetPath.mkdirs();
        }
        for (String subAssetName : subAssetList) {
            String inputSubAssetPath = Paths.get(inputAssetRelPath, subAssetName).toString();
            copyAssetsDir(inputSubAssetPath, outputPath);
        }
    }

    /**
     * 파일 복사
     */
    private void copyFile(String inputFilePath, File outputAssetFile) throws IOException {
        InputStream in = getAssets().open(inputFilePath);
        OutputStream out = new FileOutputStream(outputAssetFile);

        byte[] buffer = new byte[1024 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }

        in.close();
        out.close();
    }

    /**
     * 일정 분석 결과 콜백 인터페이스
     */
    public interface ScheduleAnalysisCallback {
        void onScheduleFound(String jsonResult);
        void onNoScheduleFound();
        void onError(String errorMessage);
    }
}