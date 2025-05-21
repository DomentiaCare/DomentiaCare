// ---------------------------------------------------------------------
// Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
// SPDX-License-Identifier: BSD-3-Clause
// ---------------------------------------------------------------------
package com.example.domentiacare.service.llama;

/**
 * StringCallBack - Callback to tunnel JNI output into Java
 */
public interface StringCallback {
    void onNewString(String str);
}
