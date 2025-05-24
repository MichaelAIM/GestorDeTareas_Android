package com.example.taskmaster

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen") // Requerido si Target API es 31+ para Splash Screen personalizado
class SplashActivity : AppCompatActivity() {

    private val SPLASH_TIME_OUT: Long = 3000 // 3 segundos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            // Este método se ejecutará una vez que termine el temporizador
            // Inicia tu actividad principal
            startActivity(Intent(this, MainActivity::class.java))

            // Cierra esta actividad
            finish()
        }, SPLASH_TIME_OUT)
    }
}