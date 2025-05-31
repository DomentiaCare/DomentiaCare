package com.quicinc.chatapp;

interface IAnalysisCallback {
    void onResult(String result);
    void onError(String error);
    void onNoResult();
    void onPartialResult(String partialText);  // 🆕 추가
}