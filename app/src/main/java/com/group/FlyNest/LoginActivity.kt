package com.group.FlyNest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import com.group.FlyNest.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import android.util.Patterns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var auth: FirebaseAuth? = null
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize Credential Manager
        credentialManager = CredentialManager.create(this)

        // Check if user is already signed in
        if (auth?.currentUser != null) {
            Log.d("LoginActivity", "User already signed in: ${auth?.currentUser?.email}")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Email/Password Login
        binding.signInButton.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            Log.d("LoginActivity", "Attempting login with Email: $email")

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else if (!isValidEmail(email)) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            } else if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
            } else {
                if (auth == null) {
                    Toast.makeText(this, "Authentication service not initialized", Toast.LENGTH_LONG).show()
                    Log.e("LoginActivity", "FirebaseAuth is null")
                    return@setOnClickListener
                }

                binding.progressBar.visibility = View.VISIBLE
                binding.overlayView.visibility = View.VISIBLE
                binding.signInButton.isEnabled = false

                auth?.signInWithEmailAndPassword(email, password)
                    ?.addOnCompleteListener { task ->
                        binding.progressBar.visibility = View.GONE
                        binding.overlayView.visibility = View.GONE
                        binding.signInButton.isEnabled = true

                        if (task.isSuccessful) {
                            Log.d("LoginActivity", "Login successful for ${auth?.currentUser?.email}")
                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Log.e("LoginActivity", "Login failed", task.exception)
                            val errorMessage = when (task.exception?.message) {
                                "There is no user record corresponding to this identifier. The user may have been deleted." ->
                                    "User not found. Please sign up first."
                                "The email address is badly formatted." ->
                                    "Invalid email format. Please check your email."
                                "The password is invalid or the user does not have a password." ->
                                    "Incorrect password. Please try again."
                                else -> "Login failed: ${task.exception?.message ?: "Unknown error"}"
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        // Redirect to Sign-Up
        binding.signUpButton.setOnClickListener {
            Log.d("LoginActivity", "Redirecting to SignUpActivity")
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        // Forgot Password
        binding.forgotText.setOnClickListener {
            Log.d("LoginActivity", "Redirecting to ForgotPassword")
            startActivity(Intent(this, ForgotPassword::class.java))
        }

        // Google Sign-In
        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun signInWithGoogle() {
        binding.progressBar.visibility = View.VISIBLE
        binding.overlayView.visibility = View.VISIBLE

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .build()

        Log.d("LoginActivity", "Launching Google Sign-In with client ID: ${getString(R.string.default_web_client_id)}")

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = credentialManager.getCredential(this@LoginActivity, request)
                val credential = result.credential
                Log.d("LoginActivity", "Credential received: ${credential.javaClass.simpleName}, Type: ${credential.type}")

                var googleIdToken: String? = null
                when (credential) {
                    is GoogleIdTokenCredential -> {
                        Log.d("LoginActivity", "Received GoogleIdTokenCredential directly")
                        googleIdToken = credential.idToken
                    }
                    is CustomCredential -> {
                        Log.d("LoginActivity", "Handling CustomCredential with type: ${credential.type}")
                        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            Log.d("LoginActivity", "Converted CustomCredential to GoogleIdTokenCredential")
                            googleIdToken = googleCredential.idToken
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@LoginActivity, "Unsupported credential type: ${credential.type}", Toast.LENGTH_LONG).show()
                                Log.e("LoginActivity", "Unsupported credential type: ${credential.type}")
                            }
                        }
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginActivity, "Unexpected credential type: ${credential.javaClass.simpleName}", Toast.LENGTH_LONG).show()
                            Log.e("LoginActivity", "Unexpected credential type: ${credential.javaClass.simpleName}")
                        }
                    }
                }

                if (googleIdToken == null) {
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        binding.overlayView.visibility = View.GONE
                        Log.e("LoginActivity", "Google ID token is null")
                        Toast.makeText(this@LoginActivity, "Google Sign-In failed: No token received", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                Log.d("LoginActivity", "Google ID Token: $googleIdToken")
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth?.signInWithCredential(firebaseCredential)?.await()

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.overlayView.visibility = View.GONE

                    if (authResult != null && authResult.user != null) {
                        Log.d("LoginActivity", "Google Sign-In successful: ${authResult.user?.email}")
                        Toast.makeText(this@LoginActivity, "Google Sign-In successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Log.e("LoginActivity", "Google Sign-In failed: No user returned")
                        Toast.makeText(this@LoginActivity, "Google Sign-In failed", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: GetCredentialException) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.overlayView.visibility = View.GONE
                    Log.e("LoginActivity", "Credential Manager error: ${e.type}", e)
                    Toast.makeText(this@LoginActivity, "Google Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.overlayView.visibility = View.GONE
                    Log.e("LoginActivity", "Unexpected error during Google Sign-In", e)
                    Toast.makeText(this@LoginActivity, "Unexpected error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}