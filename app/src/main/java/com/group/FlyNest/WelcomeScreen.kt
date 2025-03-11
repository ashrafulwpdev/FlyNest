package com.group.FlyNest

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.group.FlyNest.databinding.ActivityWelcomeScreenBinding

class WelcomeScreen : AppCompatActivity() {
    private val binding : ActivityWelcomeScreenBinding by lazy {
        ActivityWelcomeScreenBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

Handler(Looper.getMainLooper()).postDelayed({

    startActivity(Intent(this,LoginActivity::class.java))
    finish()
},3000)

    }
}