package com.quicinc.chatapp;

import com.quicinc.chatapp.IAnalysisCallback;

interface ILlamaAnalysisService {
    void analyzeText(String text, IAnalysisCallback callback);
    boolean isServiceReady();
    boolean isServiceInitializing();
}