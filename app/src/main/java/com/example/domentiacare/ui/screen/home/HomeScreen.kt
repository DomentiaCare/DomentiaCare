package com.example.domentiacare.ui.screen.home

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domentiacare.data.local.TokenManager
import com.example.domentiacare.data.remote.RetrofitClient
import com.example.domentiacare.data.remote.dto.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException

@Composable
fun HomeScreen(
    onLogout: () -> Unit
) {
    var userInfo by remember { mutableStateOf<User?>(null) }
    var aiResponse by remember { mutableStateOf("AI 응답을 기다리고 있어요...") }
    var isLoading by remember { mutableStateOf(false) }
    var hasStoragePermissions by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var debugInfo by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val token = TokenManager.getToken()

    // 권한 체크 함수
    fun checkStoragePermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 파일 상태 확인 함수
    fun checkFileState(filePath: String): Map<String, Any> {
        val file = File(filePath)
        val state = mutableMapOf<String, Any>()
        state["path"] = filePath
        state["exists"] = file.exists()
        state["canRead"] = file.canRead()
        state["canWrite"] = file.canWrite()
        if (file.exists()) {
            state["size"] = file.length()
            try {
                val content = if (file.length() < 1024) file.readText() else "파일이 너무 큽니다"
                state["preview"] = content
            } catch (e: Exception) {
                state["preview"] = "읽기 오류: ${e.message}"
            }
        }

        Log.d("TermuxAPI", "파일 상태: $state")
        return state
    }

    // 폴더 상태 확인 함수
    fun checkDirectoryState(dirPath: String) {
        val dir = File(dirPath)
        Log.d("TermuxAPI", "디렉토리 확인: $dirPath")
        Log.d("TermuxAPI", "디렉토리 존재: ${dir.exists()}")
        Log.d("TermuxAPI", "디렉토리 읽기 가능: ${dir.canRead()}")
        Log.d("TermuxAPI", "디렉토리 쓰기 가능: ${dir.canWrite()}")

        if (dir.exists() && dir.isDirectory) {
            val files = dir.listFiles()
            Log.d("TermuxAPI", "디렉토리 내 파일 수: ${files?.size ?: 0}")
            files?.forEach { file ->
                Log.d("TermuxAPI", " - ${file.name} (${file.length()} bytes)")
            }
        }
    }

    // 파일 확인 함수
    fun checkForResponseFile() {
        val handler = Handler(Looper.getMainLooper())
        val maxAttempts = 15
        var attempts = 0

        val fileChecker = object : Runnable {
            override fun run() {
                if (!isLoading) return

                attempts++
                Log.d("TermuxAPI", "파일 확인 중 (시도 $attempts/$maxAttempts)")

                try {
                    // 여러 경로 시도
                    val paths = listOf(
                        "/sdcard/Download/ai_output.txt",
                        "/storage/emulated/0/Download/ai_output.txt",
                        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/ai_output.txt"
                    )

                    for (path in paths) {
                        val file = File(path)
                        Log.d("TermuxAPI", "경로 확인: $path (존재: ${file.exists()})")

                        if (file.exists() && file.length() > 0) {
                            val response = file.readText()
                            isLoading = false
                            aiResponse = response
                            Log.d("TermuxAPI", "응답 발견: $path")
                            return
                        }
                    }

                    // 디버그 로그 확인
                    val debugLog = File("/sdcard/Download/debug_log.txt")
                    if (debugLog.exists()) {
                        val logContent = debugLog.readText()
                        debugInfo = "디버그 로그: $logContent"
                        Log.d("TermuxAPI", "디버그 로그 내용: $logContent")
                    }

                    if (attempts < maxAttempts) {
                        handler.postDelayed(this, 2000)
                    } else {
                        isLoading = false
                        aiResponse = "⚠️ 응답 파일을 찾을 수 없습니다."
                        Log.d("TermuxAPI", "최대 시도 횟수 초과")
                    }
                } catch (e: Exception) {
                    Log.e("TermuxAPI", "파일 확인 오류", e)
                    isLoading = false
                    aiResponse = "⚠️ 오류: ${e.message}"
                }
            }
        }

        handler.post(fileChecker)
    }

    // Android 11+ 권한 요청 런처
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasStoragePermissions = checkStoragePermissions(context)
        Log.d("Permissions", "저장소 권한 상태: $hasStoragePermissions")
    }

    // Android 10 이하 권한 요청 런처
    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        hasStoragePermissions = allGranted
        Log.d("Permissions", "레거시 저장소 권한 상태: $hasStoragePermissions")
    }

    // 권한 요청 함수
    fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                // 직접 설정 앱으로 이동하는 인텐트 사용
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)

                // 인텐트를 처리할 수 있는 앱이 있는지 확인
                if (intent.resolveActivity(context.packageManager) != null) {
                    Log.d("Permissions", "모든 파일 접근 권한 설정 화면으로 이동")
                    storagePermissionLauncher.launch(intent)
                } else {
                    // 대체 방법: 앱 정보 화면으로 이동
                    Log.d("Permissions", "일반 앱 설정 화면으로 이동")
                    val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    appSettingsIntent.data = Uri.parse("package:${context.packageName}")
                    storagePermissionLauncher.launch(appSettingsIntent)

                    // 사용자에게 안내 메시지 표시
                    aiResponse = "⚠️ 설정에서 '저장공간' 권한을 '허용'으로 변경해주세요."
                }
            } catch (e: Exception) {
                Log.e("Permissions", "권한 요청 화면 열기 실패", e)

                // 마지막 방법: 일반 설정 앱으로 이동
                try {
                    val settingsIntent = Intent(Settings.ACTION_SETTINGS)
                    storagePermissionLauncher.launch(settingsIntent)
                    aiResponse = "⚠️ 설정 앱에서 이 앱의 저장공간 권한을 허용해주세요."
                } catch (e2: Exception) {
                    Log.e("Permissions", "설정 앱 열기 실패", e2)
                    aiResponse = "⚠️ 앱 설정에서 저장공간 권한을 직접 허용해주세요."
                }
            }
        } else {
            legacyPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    // Termux 실행 결과를 받기 위한 BroadcastReceiver
    val outputReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("TermuxAPI", "BroadcastReceiver received an intent: ${intent.action}")

                val resultCode = intent.getIntExtra("result-code", -1)
                val stdout = intent.getStringExtra("stdout") ?: ""
                val stderr = intent.getStringExtra("stderr") ?: ""

                Log.d("TermuxAPI", "Result: $resultCode, stdout: $stdout, stderr: $stderr")

                if (resultCode == 0 && stdout.isNotEmpty()) {
                    aiResponse = stdout
                    Log.d("TermuxAPI", "응답 설정 완료: $stdout")
                    isLoading = false
                } else if (stderr.isNotEmpty()) {
                    aiResponse = "⚠️ 오류 발생: $stderr"
                    Log.e("TermuxAPI", "오류 발생: $stderr")
                    errorMessage = stderr
                    isLoading = false
                } else {
                    // BroadcastReceiver에서 응답이 없으면 파일 확인 계속 진행
                    Log.w("TermuxAPI", "브로드캐스트에서 응답 없음, 파일 확인 계속 진행")
                }
            }
        }
    }

    // 컴포저블이 활성화될 때 BroadcastReceiver 등록, 비활성화될 때 해제
    DisposableEffect(Unit) {
        val intentFilter = IntentFilter("com.termux.app.COMMAND_RESULT")
        // Android 12 이상을 위한 플래그 추가
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.registerReceiver(outputReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(outputReceiver, intentFilter)
        }

        onDispose {
            context.unregisterReceiver(outputReceiver)
        }
    }

    // 초기 권한 체크
    LaunchedEffect(Unit) {
        hasStoragePermissions = checkStoragePermissions(context)
        Log.d("Permissions", "초기 저장소 권한 상태: $hasStoragePermissions")

        // 디렉토리 상태 확인
        checkDirectoryState(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath)
    }

    // 사용자 정보 가져오기
    LaunchedEffect(Unit) {
        token?.let {
            RetrofitClient.authApi.getMyInfo("Bearer $it")
                .enqueue(object : Callback<User> {
                    override fun onResponse(call: Call<User>, response: Response<User>) {
                        if (response.isSuccessful) {
                            userInfo = response.body()
                            Log.d("HomeScreen", "유저 정보 수신 성공: ${userInfo?.nickname}")
                        } else {
                            Log.e("HomeScreen", "유저 정보 실패: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<User>, t: Throwable) {
                        Log.e("HomeScreen", "서버 연결 실패", t)
                    }
                })
        } ?: Log.e("HomeScreen", "토큰 없음")
    }

    // Termux에 명령 실행하기
    fun runTermuxCommand(context: Context, prompt: String) {
        try {
            // 먼저 디렉토리 상태 확인
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                throw IOException("다운로드 디렉토리를 생성할 수 없습니다: ${downloadsDir.absolutePath}")
            }

            // 1. 기존 출력 파일 삭제
            val outputFile = File(downloadsDir, "ai_output.txt")
            if (outputFile.exists()) {
                outputFile.delete()
                Log.d("TermuxAPI", "기존 출력 파일 삭제됨: ${outputFile.absolutePath}")
            }

            // 2. 프롬프트 파일 저장 (공용 디렉토리 사용)
            val promptFile = File(downloadsDir, "prompt.txt")
            promptFile.writeText(prompt)
            Log.d("TermuxAPI", "프롬프트 파일 저장 위치: ${promptFile.absolutePath}")

            // 3. 프롬프트 파일 확인
            val promptState = checkFileState(promptFile.absolutePath)
            if (!(promptState["exists"] as Boolean)) {
                throw IOException("프롬프트 파일이 생성되지 않았습니다")
            }

            // 4. Termux API 호출
            val executeIntent = Intent("com.termux.app.RUN_COMMAND")
            executeIntent.putExtra("executable", "/data/data/com.termux/files/usr/bin/bash")

            // 스크립트 경로와 프롬프트 파일 경로 - 명확한 경로 형식 사용
            val scriptPath = "/data/data/com.termux/files/home/run_model.sh"
            val termuxPromptPath = "/sdcard/Download/prompt.txt"
            val termuxOutputPath = "/sdcard/Download/ai_output.txt"

            executeIntent.putExtra("arguments", arrayOf(scriptPath, termuxPromptPath, termuxOutputPath))
            executeIntent.putExtra("workdir", "/data/data/com.termux/files/home")
            executeIntent.putExtra("session_action", "0")
            executeIntent.putExtra("create_new_session", false) // 세션 생성 설정 변경

            // 결과를 받기 위한 브로드캐스트 요청
            executeIntent.putExtra("command_label", "DementiaCare_AI")
            executeIntent.putExtra("background", true) // 백그라운드 실행
            executeIntent.putExtra("wants_result", true) // 결과 요청
            executeIntent.putExtra("return_result", true) // 실행 결과 반환

            // 중지된 앱에도 브로드캐스트 전달
            executeIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)

            context.sendBroadcast(executeIntent)
            Log.d("TermuxAPI", "Termux로 명령 전송됨 (스크립트: $scriptPath, 프롬프트: $termuxPromptPath, 출력: $termuxOutputPath)")

            // 명령 실행 직후 파일 확인 프로세스 시작
            checkForResponseFile()

        } catch (e: Exception) {
            Log.e("TermuxAPI", "Termux 명령 실행 오류", e)
            errorMessage = "Termux 명령 실행 오류: ${e.message}"
            isLoading = false
            aiResponse = "⚠️ Termux 실행 오류: ${e.message}"
        }
    }

    // 기존 응답 파일 읽기 함수
    fun readExistingOutputFile() {
        try {
            isLoading = true
            aiResponse = "기존 파일을 확인하는 중..."

            // 여러 가능한 경로 시도
            val paths = listOf(
                "/sdcard/Download/ai_output.txt",
                "/storage/emulated/0/Download/ai_output.txt",
                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/ai_output.txt"
            )

            var fileFound = false

            for (path in paths) {
                val file = File(path)
                Log.d("TermuxAPI", "기존 파일 확인: $path (존재: ${file.exists()})")

                if (file.exists() && file.length() > 0) {
                    val response = file.readText()
                    isLoading = false
                    aiResponse = response
                    Toast.makeText(context, "기존 응답 파일을 읽었습니다", Toast.LENGTH_SHORT).show()
                    Log.d("TermuxAPI", "기존 응답 발견: $path")
                    fileFound = true
                    break
                }
            }

            if (!fileFound) {
                isLoading = false
                aiResponse = "⚠️ 기존 응답 파일을 찾을 수 없습니다."
                Toast.makeText(context, "응답 파일을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                Log.d("TermuxAPI", "기존 응답 파일 없음")
            }

        } catch (e: Exception) {
            Log.e("TermuxAPI", "파일 읽기 오류", e)
            isLoading = false
            aiResponse = "⚠️ 파일 읽기 오류: ${e.message}"
            Toast.makeText(context, "파일 읽기 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 권한이 제대로 작동하는지 테스트하는 함수
    fun testFilePermissions() {
        try {
            val testFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "test_permission.txt")
            testFile.writeText("권한 테스트")
            val canRead = testFile.exists() && testFile.canRead()
            Toast.makeText(context, "파일 생성/읽기 테스트: ${if (canRead) "성공" else "실패"}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "파일 권한 테스트 오류: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = userInfo?.let { "안녕하세요, ${it.nickname}님!" } ?: "로딩 중...",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 권한 요청 버튼
                if (!hasStoragePermissions) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            "이 앱은 파일 저장을 위해 저장소 권한이 필요합니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Button(
                            onClick = {
                                requestStoragePermissions()
                                Toast.makeText(context, "설정 앱에서 저장소 권한을 허용해주세요", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("저장소 권한 요청하기")
                        }
                    }
                }

                // 버튼 행
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // AI에게 물어보기 버튼
                    Button(
                        onClick = {
                            if (!hasStoragePermissions) {
                                aiResponse = "⚠️ 저장소 권한이 필요합니다."
                                return@Button
                            }

                            val prompt = "Tell me a joke"
                            runTermuxCommand(context, prompt)
                            isLoading = true
                            aiResponse = "AI 응답을 기다리고 있어요..."
                            errorMessage = null
                            debugInfo = null
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && hasStoragePermissions
                    ) {
                        Text(if (isLoading) "처리 중..." else "AI에게 물어보기")
                    }

                    // 기존 파일 읽기 버튼
                    Button(
                        onClick = {
                            if (!hasStoragePermissions) {
                                aiResponse = "⚠️ 저장소 권한이 필요합니다."
                                return@Button
                            }

                            readExistingOutputFile()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && hasStoragePermissions,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("기존 응답 파일 읽기")
                    }

                    // 테스트 버튼
                    Button(
                        onClick = {
                            testFilePermissions()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && hasStoragePermissions,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("기존 응답 파일 읽기")
                    }
                }

                // 로딩 인디케이터
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }

                // AI 응답 표시
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // 남은 공간 차지
                ) {
                    Text(
                        text = "AI 응답:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Text(
                        text = aiResponse,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray)
                            .padding(12.dp)
                            .weight(1f), // 스크롤 가능하도록
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // 오류 메시지 표시
                errorMessage?.let { error ->
                    Text(
                        text = "오류 정보: $error",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // 디버그 정보 표시
                debugInfo?.let { info ->
                    Text(
                        text = info,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // 디버깅 정보
                Button(
                    onClick = {
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val promptFile = File(downloadsDir, "prompt.txt")
                        val outputFile = File(downloadsDir, "ai_output.txt")

                        val debugInfo = """
                            권한 상태: $hasStoragePermissions
                            다운로드 디렉토리: ${downloadsDir.absolutePath} (존재: ${downloadsDir.exists()})
                            프롬프트 파일: ${promptFile.absolutePath} (존재: ${promptFile.exists()})
                            출력 파일: ${outputFile.absolutePath} (존재: ${outputFile.exists()})
                            스크립트 경로: /data/data/com.termux/files/home/run_model.sh
                        """.trimIndent()

                        Log.d("TermuxAPI", "디버그 정보: $debugInfo")
                        aiResponse = "디버그 정보:\n$debugInfo"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray
                    )
                ) {
                    Text("디버그 정보 확인")
                }
            }

            // 로그아웃 버튼
            Button(
                onClick = {
                    TokenManager.clearToken()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("로그아웃")
            }
        }
    }
}