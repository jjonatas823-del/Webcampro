package com.missaolive.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Gravity
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var status: TextView
    private lateinit var urlText: TextView
    private lateinit var streamButton: Button
    private lateinit var cameraButton: Button
    private lateinit var qualitySpinner: Spinner
    private var cameraProvider: ProcessCameraProvider? = null
    private var analyzer: ImageAnalysis? = null
    private var server: MjpegServer? = null
    private var isStreaming = false
    private var lens = CameraSelector.DEFAULT_BACK_CAMERA
    private var selectedSize = Size(1920, 1080)
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else status.text = "Permissão de câmera negada."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 20, 24, 24); setBackgroundColor(0xFF101827.toInt()) }
        val title = TextView(this).apply { text = "Missãolive"; textSize = 28f; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER; setPadding(0, 8, 0, 2) }
        val sub = TextView(this).apply { text = "Câmera sem fio para OBS Studio"; textSize = 14f; setTextColor(0xFFB8C2D0.toInt()); gravity = Gravity.CENTER; setPadding(0, 0, 0, 18) }
        previewView = PreviewView(this).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE; scaleType = PreviewView.ScaleType.FILL_CENTER }
        root.addView(title); root.addView(sub)
        root.addView(previewView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        val qualityLabel = TextView(this).apply { text = "Qualidade de captura"; setTextColor(0xFFFFFFFF.toInt()); setPadding(0, 16, 0, 6) }
        qualitySpinner = Spinner(this)
        val options = listOf("720p (1280×720)", "1080p (1920×1080)", "4K (3840×2160)")
        qualitySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        qualitySpinner.setSelection(1)
        qualitySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedSize = when(position) { 0 -> Size(1280,720); 2 -> Size(3840,2160); else -> Size(1920,1080) }
                if (!isStreaming) startCamera()
            }
        }
        root.addView(qualityLabel); root.addView(qualitySpinner)
        cameraButton = Button(this).apply { text = "Trocar câmera"; setOnClickListener { lens = if (lens == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA; startCamera() } }
        streamButton = Button(this).apply { text = "INICIAR TRANSMISSÃO"; setOnClickListener { toggleStream() } }
        status = TextView(this).apply { text = "Pronto para conectar"; textSize = 14f; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER; setPadding(0, 10, 0, 6) }
        urlText = TextView(this).apply { text = ""; textSize = 14f; setTextColor(0xFF62D98B.toInt()); gravity = Gravity.CENTER; setPadding(0, 4, 0, 12) }
        root.addView(cameraButton); root.addView(streamButton); root.addView(status); root.addView(urlText)
        setContentView(root)
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get(); cameraProvider = provider; provider.unbindAll()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder().setTargetResolution(selectedSize).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analyzer = analysis
                analysis.setAnalyzer(executor) { image ->
                    if (isStreaming) {
                        try {
                            val bitmap = image.toBitmap()
                            val out = ByteArrayOutputStream(); bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                            server?.publish(out.toByteArray())
                            bitmap.recycle()
                        } catch (_: Exception) { }
                    }
                    image.close()
                }
                provider.bindToLifecycle(this, lens, preview, analysis)
            } catch (e: Exception) { status.text = "Não foi possível abrir a câmera: ${e.message}" }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleStream() {
        if (!isStreaming) {
            try {
                server = MjpegServer(8080); server!!.start()
                isStreaming = true; streamButton.text = "PARAR TRANSMISSÃO"
                val ip = localIp() ?: "IP não encontrado"
                urlText.text = "http://$ip:8080/video.mjpg"
                status.text = "Transmitindo • conecte o OBS pela rede local"
            } catch (e: Exception) { status.text = "Falha ao iniciar: ${e.message}" }
        } else {
            isStreaming = false; server?.stop(); server = null
            streamButton.text = "INICIAR TRANSMISSÃO"; status.text = "Transmissão parada"; urlText.text = ""
        }
    }

    private fun localIp(): String? {
        try { val interfaces = NetworkInterface.getNetworkInterfaces(); while (interfaces.hasMoreElements()) { val intf = interfaces.nextElement(); val addrs = intf.inetAddresses; while(addrs.hasMoreElements()) { val addr = addrs.nextElement(); if (!addr.isLoopbackAddress && addr is Inet4Address) return addr.hostAddress } } } catch (_: Exception) {}
        return null
    }
    override fun onDestroy() { isStreaming = false; server?.stop(); executor.shutdown(); super.onDestroy() }
}
