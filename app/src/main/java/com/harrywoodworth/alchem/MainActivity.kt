package com.harrywoodworth.alchem

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.VisibleForTesting
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.maximeroussy.invitrode.WordGenerator
import kotlinx.android.synthetic.main.activity_main.*
import android.text.Editable
import android.text.TextWatcher


class MainActivity : AppCompatActivity(), View.OnClickListener {

    // Start [TYPE GAME VARIABLES]
    private var currentWord = "____DEFAULT_____"
    // End [TYPE GAME VARIABLES]

    @VisibleForTesting
    val progressBar by lazy {
        ProgressBar(this)
    }

    // Firebase Auth Object
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getSupportActionBar()?.hide()

        // Buttons
        emailSignInButton.setOnClickListener(this)
        emailCreateAccountButton.setOnClickListener(this)
        signOutButton.setOnClickListener(this)
        verifyEmailButton.setOnClickListener(this)

        // Get the shared instance of FirebaseAuth
        auth = FirebaseAuth.getInstance()

        editText.addTextChangedListener(textWatcher)
    }

    public override fun onStart() {
        super.onStart()

        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        updateUI(currentUser)

        // Generate new word / set UI
        generateNewWord()
    }

    // Create an account with email and password, log results
    private fun createAccount(email: String, password: String) {
        Log.d(TAG, "createAccount:$email")
        if (!validateForm()) {
            return
        }

        showProgressBar()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }

                hideProgressBar()
            }
    }

    // Log sign in and show
    private fun signIn(email: String, password: String) {
        Log.d(TAG, "signIn:$email")
        if (!validateForm()) {
            return
        }

        showProgressBar()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
                hideProgressBar()
            }
    }

    // Sign out user and update UI
    private fun signOut() {
        auth.signOut()
        updateUI(null)
    }

    // Send email verification and send a toast
    private fun sendEmailVerification() {

        // Disable button
        verifyEmailButton.isEnabled = false

        // Send verification email
        val user = auth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener(this) { task ->

                // Re-enable button
                verifyEmailButton.isEnabled = true

                if (task.isSuccessful) {
                    Toast.makeText(baseContext,
                        "Verification email sent to ${user.email} ",
                        Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "sendEmailVerification", task.exception)
                    Toast.makeText(baseContext,
                        "Failed to send verification email.",
                        Toast.LENGTH_SHORT).show()
                }

            }

    }

    private fun validateForm(): Boolean {
        var valid = true

        val email = fieldEmail.text.toString()
        if (TextUtils.isEmpty(email)) {
            fieldEmail.error = "Required."
            valid = false
        } else {
            fieldEmail.error = null
        }

        val password = fieldPassword.text.toString()
        if (TextUtils.isEmpty(password)) {
            fieldPassword.error = "Required."
            valid = false
        } else {
            fieldPassword.error = null
        }

        return valid
    }

    private fun updateUI(user: FirebaseUser?) {
        hideProgressBar()
        if (user != null) {
            emailPasswordButtons.visibility = View.GONE
            emailPasswordFields.visibility = View.GONE
            signedInButtons.visibility = View.VISIBLE
            verifyEmailButton.isEnabled = !user.isEmailVerified
        } else {
            emailPasswordButtons.visibility = View.VISIBLE
            emailPasswordFields.visibility = View.VISIBLE
            signedInButtons.visibility = View.GONE
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.emailCreateAccountButton -> createAccount(fieldEmail.text.toString(), fieldPassword.text.toString())
            R.id.emailSignInButton -> signIn(fieldEmail.text.toString(), fieldPassword.text.toString())
            R.id.signOutButton -> signOut()
            R.id.verifyEmailButton -> sendEmailVerification()
        }
    }

    private fun showProgressBar() {

        // Remove user interaction
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        // Show indeterminate animation
        progressBar.isIndeterminate = true

        // make visible
        if(progressBar.visibility == View.INVISIBLE) {
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun hideProgressBar() {

        // Get user interaction back
        this.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        if(progressBar.visibility == View.VISIBLE) {
            progressBar.visibility = View.INVISIBLE
        }
    }

    public override fun onStop() {
        super.onStop()
        hideProgressBar()
    }

    // Generate a new random word
    private fun generateNewWord() {
        this.currentWord = WordGenerator().newWord(10).toLowerCase()
        resetUIForNewWord()
    }

    fun generateNewWord(view: View) {
        this.generateNewWord()
    }

    // Reset UI when a new word is generated
    private fun resetUIForNewWord() {
        textView.text = this.currentWord.toUpperCase()
        editText.text.clear()
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            if(!editText.text.equals("")) {
                if (currentWord.equals(s.toString())) {
                    generateNewWord()
                    return
                }
                if (!currentWord.contains(s.toString(), true)) {
                    editText.text.clear()
                    return
                }
            }
        }
    }

    // Logging tag const
    companion object {
        private const val TAG = "EmailPassword"
    }
}
