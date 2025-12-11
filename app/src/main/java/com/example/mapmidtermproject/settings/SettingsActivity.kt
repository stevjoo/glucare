package com.example.mapmidtermproject.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import com.example.mapmidtermproject.R
import com.example.mapmidtermproject.activities.AnalysisActivity
import com.example.mapmidtermproject.activities.LogActivity
import com.example.mapmidtermproject.activities.LoginActivity
import com.example.mapmidtermproject.activities.MainActivity
import com.example.mapmidtermproject.viewmodels.UserViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var viewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth = Firebase.auth
        viewModel = ViewModelProvider(this)[UserViewModel::class.java]

        etUsername = findViewById(R.id.et_username)
        etPhone = findViewById(R.id.et_phone)

        val btnAccount = findViewById<ConstraintLayout>(R.id.cl_menu_account)
        val btnFAQ = findViewById<ConstraintLayout>(R.id.cl_menu_faq)
        val btnPrivacyPolicy = findViewById<ConstraintLayout>(R.id.cl_menu_privacy)
        val btnLogout = findViewById<Button>(R.id.btn_logout)

        etUsername.keyListener = null
        etPhone.keyListener = null

        btnAccount.setOnClickListener { startActivity(Intent(this, AccountSettingsActivity::class.java)) }
        btnFAQ.setOnClickListener { startActivity(Intent(this, FAQActivity::class.java)) }
        btnPrivacyPolicy.setOnClickListener { startActivity(Intent(this, PrivacyPolicyActivity::class.java)) }
        btnLogout.setOnClickListener { signOut() }

        viewModel.userProfile.observe(this) { profile ->
            val currentUser = auth.currentUser
            val googleName = currentUser?.displayName ?: "Pengguna"

            if (profile != null) {
                etUsername.setText(if (!profile.username.isNullOrEmpty()) profile.username else googleName)
                etPhone.setText(if (!profile.phone.isNullOrEmpty()) profile.phone else "Belum diatur")
            } else {
                etUsername.setText(googleName)
                etPhone.setText("Belum diatur")
            }
        }

        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile()
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        if (bottomNav.selectedItemId != R.id.nav_settings) {
            bottomNav.selectedItemId = R.id.nav_settings
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT })
                    true
                }
                R.id.nav_log -> {
                    startActivity(Intent(this, LogActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT })
                    true
                }
                R.id.nav_settings -> true
                else -> false
            }
        }
    }

    private fun signOut() {
        auth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}