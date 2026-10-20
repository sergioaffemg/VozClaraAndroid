package br.com.vozclara

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var engine: AudioEngine
    private lateinit var status: TextView
    private lateinit var button: Button
    private lateinit var gainLabel: TextView
    private var running = false
    private var gainPercent = 70

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = AudioEngine()
        volumeControlStream = AudioManager.STREAM_MUSIC
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 32, 28, 24) }
        layout.addView(TextView(this).apply { text = "Voz Clara"; textSize = 28f })
        layout.addView(TextView(this).apply { text = "Amplificação de voz e redução moderada de ruído"; textSize = 16f; setPadding(0, 8, 0, 24) })
        status = TextView(this).apply { text = "Parado. Conecte um fone com fio ou USB-C."; textSize = 15f }
        layout.addView(status)
        gainLabel = TextView(this).apply { text = "Intensidade da voz: $gainPercent%"; setPadding(0, 24, 0, 4) }
        layout.addView(gainLabel)
        layout.addView(SeekBar(this).apply {
            max = 100; progress = gainPercent
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    gainPercent = progress.coerceAtLeast(10)
                    gainLabel.text = "Intensidade da voz: $gainPercent%"
                    engine.setGainPercent(gainPercent)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        })
        button = Button(this).apply { text = "Iniciar amplificação"; setOnClickListener { toggle() } }
        layout.addView(button)
        layout.addView(TextView(this).apply { text = "\nMantenha o celular perto de quem fala. Comece com volume baixo."; textSize = 14f })
        setContentView(layout)
    }

    private fun toggle() {
        if (running) {
            engine.stop(); running = false; button.text = "Iniciar amplificação"; status.text = "Parado."; return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 42); return
        }
        try {
            engine.setGainPercent(gainPercent); engine.start(); running = true
            button.text = "Parar"; status.text = "Áudio ativo. Saída selecionada pelo Android."
        } catch (e: Exception) {
            status.text = "Falha ao iniciar: ${e.message ?: "erro de áudio"}"
            Toast.makeText(this, status.text, Toast.LENGTH_LONG).show()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 42 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) toggle()
        else if (requestCode == 42) Toast.makeText(this, "Permita o uso do microfone.", Toast.LENGTH_LONG).show()
    }
    override fun onDestroy() { engine.stop(); super.onDestroy() }
}
