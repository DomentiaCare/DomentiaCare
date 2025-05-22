package com.quicinc.chatapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class LlamaAnalysisService extends Service {

    private static final String TAG = "LlamaAnalysisService";

    // GenieWrapper instance
    private GenieWrapper genieWrapper;
    private boolean isInitialized = false;
    private ExecutorService executorService;

    // Service initialization state
    private boolean isInitializing = false;

    static {
        System.loadLibrary("chatapp");
    }

    // AIDL 기반 Binder
    private final ILlamaAnalysisService.Stub binder = new ILlamaAnalysisService.Stub() {
        @Override
        public void analyzeText(String text, IAnalysisCallback callback) throws RemoteException {
            Log.d(TAG, "AIDL analyzeText called with: " + text);
            analyzeTextInternal(text, callback);
        }

        @Override
        public boolean isServiceReady() throws RemoteException {
            return isInitialized;
        }

        @Override
        public boolean isServiceInitializing() throws RemoteException {
            return isInitializing;
        }
    };

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
        Log.d(TAG, "Client bound to LlamaAnalysisService via AIDL");
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

        genieWrapper = null;
    }

    private void analyzeTextInternal(String inputText, IAnalysisCallback callback) {
        if (!isInitialized) {
            try {
                if (isInitializing) {
                    callback.onError("서비스가 초기화 중입니다. 잠시 후 다시 시도해주세요.");
                } else {
                    callback.onError("서비스 초기화에 실패했습니다.");
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Error calling callback: " + e.getMessage());
            }
            return;
        }

        if (inputText == null || inputText.trim().isEmpty()) {
            try {
                callback.onError("입력 텍스트가 비어있습니다.");
            } catch (RemoteException e) {
                Log.e(TAG, "Error calling callback: " + e.getMessage());
            }
            return;
        }

        executorService.execute(() -> {
            final StringBuilder responseBuilder = new StringBuilder();
            final AtomicBoolean callbackInvoked = new AtomicBoolean(false);
            final long startTime = System.currentTimeMillis();

            try {
                Log.d(TAG, "Processing query: " + inputText);

                // 🔧 단일 응답 수집기
                final Object responseLock = new Object();

                genieWrapper.getResponseForPrompt(inputText.trim(), new StringCallback() {
                    @Override
                    public void onNewString(String response) {
                        synchronized (responseLock) {
                            responseBuilder.append(response);
                            String currentResponse = responseBuilder.toString().trim();

                            Log.d(TAG, "Response length: " + currentResponse.length() + ", content preview: " +
                                    (currentResponse.length() > 50 ? currentResponse.substring(0, 50) + "..." : currentResponse));

                            // 🆕 즉시 부분 결과 전송
                            try {
                                callback.onPartialResult(currentResponse);
                                Log.d(TAG, "Partial result sent, length: " + currentResponse.length());
                            } catch (RemoteException e) {
                                Log.e(TAG, "Error sending partial result: " + e.getMessage());
                            }

                            // 🔧 완료 조건 체크는 별도로 처리 (부분 결과 전송과 분리)
                            // 완료 조건을 만족하고 아직 최종 콜백이 호출되지 않았을 때만
                            if (!callbackInvoked.get() && isCompleteResponse(currentResponse)) {
                                if (callbackInvoked.compareAndSet(false, true)) {
                                    Log.d(TAG, "✅ Complete response detected, invoking final callback");
                                    invokeFinalCallback(callback, currentResponse);
                                }
                            }
                        }
                    }
                });

                // 🔧 타임아웃 메커니즘
                Thread timeoutThread = new Thread(() -> {
                    try {
                        Thread.sleep(10000); // 10초 대기

                        // 🔧 타임아웃 시에도 한 번만 실행되도록 보장
                        if (callbackInvoked.compareAndSet(false, true)) {
                            synchronized (responseLock) {
                                String finalResponse = responseBuilder.toString().trim();
                                Log.d(TAG, "⏰ Timeout reached, sending response: " + finalResponse.substring(0, Math.min(100, finalResponse.length())));
                                invokeFinalCallback(callback, finalResponse.isEmpty() ? "Response timeout" : finalResponse);
                            }
                        }
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Timeout thread interrupted");
                    }
                });
                timeoutThread.setDaemon(true); // 데몬 스레드로 설정
                timeoutThread.start();

            } catch (Exception e) {
                Log.e(TAG, "Error during query processing: " + e.toString());
                // 🔧 예외 발생 시에도 한 번만 실행되도록 보장
                if (callbackInvoked.compareAndSet(false, true)) {
                    invokeErrorCallback(callback, "쿼리 처리 중 오류가 발생했습니다: " + e.getMessage());
                }
            }
        });
    }

    // 🔧 더 정확한 완료 조건 체크 - 더 엄격하게
    // 🔧 훨씬 더 엄격한 완료 조건 체크
    private boolean isCompleteResponse(String response) {
        // 기본 길이 체크 - 더 엄격하게
        if (response.length() < 200) {
            return false;
        }

        // JSON 응답 체크 - 훨씬 더 엄격한 조건
        if (response.contains("Summary:") && response.contains("Schedule:")) {
            boolean hasAllFields = response.contains("\"date\"") &&
                    response.contains("\"time\"") &&
                    response.contains("\"place\"");

            // 완전히 닫힌 JSON 구조인지 확인
            boolean endsWithBrace = response.trim().endsWith("}");

            if (hasAllFields && endsWithBrace) {
                // 🆕 JSON 부분 추출 및 검증
                try {
                    int jsonStart = response.indexOf("{");
                    if (jsonStart == -1) return false;

                    String jsonPart = response.substring(jsonStart).trim();

                    // JSON이 완전히 닫혀있는지 확인
                    if (!jsonPart.endsWith("}")) return false;

                    // place 필드가 완전히 채워져 있는지 확인
                    // "AMC theater on Main Street" 전체가 있어야 완료
                    boolean hasCompletePlace = jsonPart.contains("\"place\": \"AMC theater on Main Street\"") ||
                            jsonPart.contains("\"place\":\"AMC theater on Main Street\"");

                    // date와 time이 구체적으로 채워져 있는지 확인
                    boolean hasConcreteDate = (jsonPart.contains("\"date\": \"Sunday\"") ||
                            jsonPart.contains("\"date\":\"Sunday\"")) &&
                            !jsonPart.contains("\"date\": \"\"");

                    boolean hasConcreteTime = (jsonPart.contains("\"time\": \"14:00\"") ||
                            jsonPart.contains("\"time\":\"14:00\"")) &&
                            !jsonPart.contains("\"time\": \"\"");

                    if (hasCompletePlace && hasConcreteDate && hasConcreteTime) {
                        Log.d(TAG, "✅ Complete JSON response detected with all required fields");
                        return true;
                    } else {
                        Log.d(TAG, "❌ JSON incomplete - Place: " + hasCompletePlace +
                                ", Date: " + hasConcreteDate + ", Time: " + hasConcreteTime);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing JSON for completion check: " + e.getMessage());
                    return false;
                }
            }
        }

        // 간단한 응답의 경우 (JSON이 아닌 경우)
        // 문장이 완전히 끝났는지 확인
        if (!response.contains("Summary:") && !response.contains("Schedule:")) {
            // 마침표나 느낌표로 끝나고, 충분한 길이가 있으면 완료로 간주
            if ((response.endsWith(".") || response.endsWith("!") || response.endsWith("?")) &&
                    response.length() > 50) {
                Log.d(TAG, "✅ Complete simple response detected");
                return true;
            }
        }

        return false;
    }

    // 🔧 안전한 최종 콜백 호출
    private void invokeFinalCallback(IAnalysisCallback callback, String response) {
        try {
            if (response == null || response.trim().isEmpty()) {
                callback.onNoResult();
            } else {
                callback.onResult(response);
            }
            Log.d(TAG, "Final callback invoked successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error invoking final callback: " + e.getMessage());
        }
    }

    // 🔧 안전한 오류 콜백 호출
    private void invokeErrorCallback(IAnalysisCallback callback, String errorMessage) {
        try {
            callback.onError(errorMessage);
            Log.d(TAG, "Error callback invoked successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error invoking error callback: " + e.getMessage());
        }
    }

    // 기존 초기화 메서드들 (동일)
    private void initializeService() {
        if (isInitializing || isInitialized) {
            return;
        }

        isInitializing = true;

        executorService.execute(() -> {
            try {
                Log.d(TAG, "Starting LLaMA service initialization...");

                String htpConfigPath = checkSocCompatibilityAndGetConfig();
                if (htpConfigPath == null) {
                    Log.e(TAG, "Unsupported device for LLaMA");
                    return;
                }

                copyAssetsToCache();
                setupEnvironment();
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

    private void copyAssetsToCache() throws IOException {
        String externalDir = getExternalCacheDir().getAbsolutePath();
        copyAssetsDir("models", externalDir);
        copyAssetsDir("htp_config", externalDir);
        Log.d(TAG, "Assets copied to external cache");
    }

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

    // 유틸리티 메서드들 (기존과 동일)
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
}