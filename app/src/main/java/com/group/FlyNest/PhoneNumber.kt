package com.group.FlyNest

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.group.FlyNest.databinding.ActivityPhoneNumberBinding
import com.google.firebase.auth.FirebaseAuth
import com.hbb20.CountryCodePicker

class PhoneNumber : AppCompatActivity() {
    private val binding: ActivityPhoneNumberBinding by lazy {
        ActivityPhoneNumberBinding.inflate(layoutInflater)
    }
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val ccp: CountryCodePicker = findViewById(R.id.ccp)
        val etPhoneNumber: EditText = findViewById(R.id.phone)

// Connect EditTextPhoneNumber with CountryCodePicker
        ccp.registerCarrierNumberEditText(etPhoneNumber)

// Set default country to Malaysia (MY)
        ccp.setDefaultCountryUsingNameCode("MY")

        // Connect EditTextPhoneNumber with CountryCodePicker
        ccp.registerCarrierNumberEditText(etPhoneNumber)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Forgot Password Click Listener
        binding.forgotText.setOnClickListener {
            val intent = Intent(this, ForgotPassword::class.java)
            startActivity(intent)
        }

        // Sign In Button Click Listener
        binding.signInButton.setOnClickListener {
            val userName = binding.phone.text.toString()
            val password = binding.password.text.toString()

            if (userName.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill up all the details", Toast.LENGTH_SHORT).show()
            } else {
                auth.signInWithEmailAndPassword(userName, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Log in Successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Log in failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        // Sign Up Button Click Listener
        binding.singUpButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        // Email Button Click Listener
        binding.email.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}