package com.example.domentiacare.service.watch

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable

object WatchMessageHelper {
    fun sendMessageToWatch(context: Context, path: String, message: String) {
        val nodeClient = Wearable.getNodeClient(context)
        val messageClient = Wearable.getMessageClient(context)
        Thread {
            try {
                val nodes = Tasks.await(nodeClient.connectedNodes)
                if (nodes.isEmpty()) {
                    Log.e("WatchMessageHelper", "연결된 워치 노드 없음")
                }
                for (node in nodes) {
                    Log.d("WatchMessageHelper", "노드 발견: ${node.displayName} (${node.id})")
                    val task = messageClient.sendMessage(
                        node.id,
                        path,
                        message.toByteArray()
                    )
                    task.addOnSuccessListener {
                        Log.d("WatchMessageHelper", "워치 메시지 전송 **성공** [${node.displayName}] : $message")
                    }
                    task.addOnFailureListener { ex ->
                        Log.e("WatchMessageHelper", "워치 메시지 전송 **실패** [${node.displayName}] : ${ex.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("WatchMessageHelper", "워치 메시지 전송 실패(예외): ${e.message}")
            }
        }.start()
    }
}
