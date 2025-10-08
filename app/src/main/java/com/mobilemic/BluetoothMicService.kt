package com.mobilemic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothMicService : Service() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var audioRecord: AudioRecord? = null
    private var isStreaming = false
    
    private var acceptThread: Thread? = null
    private var streamThread: Thread? = null

    companion object {
        private const val TAG = "BluetoothMicService"
        private const val SERVICE_NAME = "MobileMic"
        private val SERVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID
        
        const val ACTION_STATUS_CHANGED = "com.mobilemic.STATUS_CHANGED"
        const val EXTRA_STATUS = "status"
        
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "MobileMicChannel"
        
        // Audio configuration
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    override fun onCreate() {
        super.onCreate()
        
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Initializing..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startBluetoothServer()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopStreaming()
        closeServerSocket()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MobileMic Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Bluetooth Microphone Service"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(status: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MobileMic")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(status: String) {
        val notification = createNotification(status)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun broadcastStatus(status: String) {
        val intent = Intent(ACTION_STATUS_CHANGED)
        intent.putExtra(EXTRA_STATUS, status)
        sendBroadcast(intent)
        updateNotification(status)
    }

    private fun startBluetoothServer() {
        acceptThread = Thread {
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                    SERVICE_NAME,
                    SERVICE_UUID
                )
                
                Log.d(TAG, "Bluetooth server started, waiting for connection...")
                broadcastStatus("Waiting for connection...")
                
                // Accept incoming connection (blocking call)
                clientSocket = serverSocket?.accept()
                
                Log.d(TAG, "Client connected: ${clientSocket?.remoteDevice?.name}")
                broadcastStatus("Connected to ${clientSocket?.remoteDevice?.name}")
                
                // Start audio streaming
                startAudioStreaming()
                
            } catch (e: IOException) {
                Log.e(TAG, "Error in accept thread", e)
                broadcastStatus("Connection error")
            }
        }
        acceptThread?.start()
    }

    private fun closeServerSocket() {
        try {
            serverSocket?.close()
            clientSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing server socket", e)
        }
    }

    private fun startAudioStreaming() {
        if (isStreaming) return
        
        isStreaming = true
        
        streamThread = Thread {
            try {
                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT
                )
                
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
                
                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord not initialized")
                    broadcastStatus("Error: Cannot access microphone")
                    return@Thread
                }
                
                audioRecord?.startRecording()
                broadcastStatus("Streaming audio...")
                
                val buffer = ByteArray(bufferSize)
                val outputStream: OutputStream = clientSocket?.outputStream ?: return@Thread
                
                Log.d(TAG, "Starting audio stream...")
                
                while (isStreaming && clientSocket?.isConnected == true) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (bytesRead > 0) {
                        try {
                            outputStream.write(buffer, 0, bytesRead)
                            outputStream.flush()
                        } catch (e: IOException) {
                            Log.e(TAG, "Error writing to output stream", e)
                            break
                        }
                    }
                }
                
                Log.d(TAG, "Audio streaming stopped")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in streaming thread", e)
                broadcastStatus("Streaming error")
            } finally {
                stopStreaming()
            }
        }
        streamThread?.start()
    }

    private fun stopStreaming() {
        isStreaming = false
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio record", e)
        }
        
        try {
            clientSocket?.close()
            clientSocket = null
        } catch (e: IOException) {
            Log.e(TAG, "Error closing client socket", e)
        }
        
        broadcastStatus("Disconnected")
    }
}
