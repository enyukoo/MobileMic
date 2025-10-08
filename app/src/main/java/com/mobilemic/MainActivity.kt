package com.mobilemic

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusText: TextView
    private lateinit var deviceNameText: TextView
    
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isServiceRunning = false

    companion object {
        private const val REQUEST_PERMISSIONS = 1
        private const val REQUEST_ENABLE_BT = 2
        private const val REQUEST_DISCOVERABLE = 3
    }

    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothMicService.ACTION_STATUS_CHANGED -> {
                    val status = intent.getStringExtra(BluetoothMicService.EXTRA_STATUS) ?: ""
                    updateStatus(status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        statusText = findViewById(R.id.statusText)
        deviceNameText = findViewById(R.id.deviceNameText)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        updateDeviceName()
        
        startButton.setOnClickListener {
            if (checkPermissions()) {
                startBluetoothMicService()
            } else {
                requestPermissions()
            }
        }

        stopButton.setOnClickListener {
            stopBluetoothMicService()
        }

        val filter = IntentFilter(BluetoothMicService.ACTION_STATUS_CHANGED)
        registerReceiver(serviceReceiver, filter, RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(serviceReceiver)
    }

    private fun updateDeviceName() {
        val deviceName = if (checkBluetoothPermission()) {
            bluetoothAdapter?.name ?: "Unknown"
        } else {
            "Unknown"
        }
        deviceNameText.text = getString(R.string.device_info, deviceName)
    }

    private fun checkBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        
        return permissions.isEmpty()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        permissions.add(Manifest.permission.RECORD_AUDIO)
        
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_PERMISSIONS) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                updateDeviceName()
                startBluetoothMicService()
            } else {
                Toast.makeText(this, "Permissions are required to use this app", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startBluetoothMicService() {
        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        // Enable Bluetooth if not enabled
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            return
        }

        // Make device discoverable
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode == RESULT_OK) {
                    startBluetoothMicService()
                } else {
                    Toast.makeText(this, "Bluetooth must be enabled", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_DISCOVERABLE -> {
                if (resultCode > 0) {
                    // Device is now discoverable, start the service
                    val intent = Intent(this, BluetoothMicService::class.java)
                    startService(intent)
                    isServiceRunning = true
                    startButton.isEnabled = false
                    stopButton.isEnabled = true
                    updateStatus("Waiting for connection...")
                } else {
                    Toast.makeText(this, "Device must be discoverable", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun stopBluetoothMicService() {
        val intent = Intent(this, BluetoothMicService::class.java)
        stopService(intent)
        isServiceRunning = false
        startButton.isEnabled = true
        stopButton.isEnabled = false
        updateStatus("Idle")
    }

    private fun updateStatus(status: String) {
        runOnUiThread {
            statusText.text = "Status: $status"
        }
    }
}
