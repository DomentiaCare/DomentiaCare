// TestLlamaActivity.kt
// 경로: app/src/main/java/com/example/domentiacare/ui/test/TestLlamaActivity.kt

package com.example.domentiacare.ui.test

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.domentiacare.service.llama.LlamaServiceManager
import kotlinx.coroutines.launch

class TestLlamaActivity : ComponentActivity() {

    private lateinit var llamaServiceManager: LlamaServiceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        llamaServiceManager = LlamaServiceManager()

        setContent {
            MaterialTheme {
                TestLlamaScreen(
                    llamaServiceManager = llamaServiceManager,
                    onBackPressed = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        llamaServiceManager.disconnect(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestLlamaScreen(
    llamaServiceManager: LlamaServiceManager,
    onBackPressed: () -> Unit
) {
    var connectionStatus by remember { mutableStateOf("Not Connected") }
    var queryText by remember { mutableStateOf("Hello, how are you?") }
    var response by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🤖 LLaMA ChatApp Test") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 연결 상태 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (connectionStatus) {
                        "Connected & Ready" -> MaterialTheme.colorScheme.primaryContainer
                        "Connected (Initializing...)" -> MaterialTheme.colorScheme.secondaryContainer
                        "Connecting..." -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📡 Connection Status",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = connectionStatus,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                connectionStatus = "Connecting..."
                                Log.d("TestLlamaActivity", "Attempting to connect to ChatApp...")

                                val connected = llamaServiceManager.connectToService(context)
                                Log.d("TestLlamaActivity", "Connection result: $connected")

                                connectionStatus = if (connected) {
                                    Log.d("TestLlamaActivity", "Checking if service is ready...")
                                    if (llamaServiceManager.isServiceReady()) {
                                        "Connected & Ready"
                                    } else {
                                        "Connected (Initializing...)"
                                    }
                                } else {
                                    "Connection Failed"
                                }

                                Log.d("TestLlamaActivity", "Final status: $connectionStatus")
                            }
                        },
                        enabled = !llamaServiceManager.isConnecting(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (llamaServiceManager.isConnecting()) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Connect to ChatApp")
                    }
                }
            }

            // 쿼리 입력 섹션
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💬 Send Query",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        label = { Text("Enter your question") },
                        placeholder = { Text("Ask anything in English...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                response = ""

                                try {
                                    Log.d("TestLlamaActivity", "Sending query: $queryText")
                                    val result = llamaServiceManager.sendQuery(queryText)
                                    response = result
                                    Log.d("TestLlamaActivity", "Received response: $result")
                                } catch (e: Exception) {
                                    response = "Error: ${e.message}"
                                    Log.e("TestLlamaActivity", "Error sending query", e)
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading && llamaServiceManager.isConnected() && queryText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Thinking...")
                        } else {
                            Text("Send Query")
                        }
                    }
                }
            }

            // 응답 표시 섹션
            if (response.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🤖 LLaMA Response",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SelectionContainer {
                            Text(
                                text = response,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // 샘플 쿼리 섹션
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📝 Sample Queries",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val sampleQueries = listOf(
                        "Hello, how are you today?",
                        "What is the capital of France?",
                        "Tell me a short joke",
                        "What is 25 + 17?",
                        "Explain AI in simple terms",
                        "What day is today?",
                        "Count from 1 to 10"
                    )

                    sampleQueries.forEach { query ->
                        OutlinedButton(
                            onClick = { queryText = query },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = query,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // 디버그 정보 섹션
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔧 Debug Info",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Service Connected: ${llamaServiceManager.isConnected()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Service Connecting: ${llamaServiceManager.isConnecting()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Service Ready: ${llamaServiceManager.isServiceReady()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // 연결 상태 실시간 업데이트
            LaunchedEffect(llamaServiceManager) {
                while (true) {
                    if (llamaServiceManager.isConnected()) {
                        connectionStatus = if (llamaServiceManager.isServiceReady()) {
                            "Connected & Ready"
                        } else {
                            "Connected (Initializing...)"
                        }
                    } else if (llamaServiceManager.isConnecting()) {
                        connectionStatus = "Connecting..."
                    } else {
                        connectionStatus = "Not Connected"
                    }
                    kotlinx.coroutines.delay(2000) // 2초마다 업데이트
                }
            }
        }
    }
}