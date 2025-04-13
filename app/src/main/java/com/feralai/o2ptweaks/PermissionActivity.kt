package com.feralai.o2ptweaks

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PermissionActivity : AppCompatActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permesso concesso, avvia MainActivity
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // Permesso negato, mostra un messaggio o gestisci la situazione
                // Per esempio, puoi chiudere l'app o mostrare una spiegazione
                finish() // Chiude l'activity se il permesso è negato
            }
            finish() // Chiude PermissionActivity dopo aver gestito il permesso
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permesso già concesso, avvia MainActivity direttamente
                    startActivity(Intent(this, MainActivity::class.java))
                    finish() // Chiude PermissionActivity
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Spiega all'utente perché serve il permesso (opzionale, ma consigliato)
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Richiedi il permesso
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Su versioni precedenti a Android 13, avvia direttamente MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}