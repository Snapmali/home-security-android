package com.snapkirin.homesecurity.ui.resetpassword

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ProgressBar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.snapkirin.homesecurity.HomeSecurity
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.http.BasicRequestResult
import com.snapkirin.homesecurity.network.websocket.WebSocket
import com.snapkirin.homesecurity.util.UserAccount
import com.snapkirin.homesecurity.service.UserTokenHandlerService
import com.snapkirin.homesecurity.ui.login.LoginActivity
import com.snapkirin.homesecurity.ui.util.afterTextChanged
import com.snapkirin.homesecurity.ui.util.showToast

class ResetPasswordActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ResetPasswordActivity"
    }

    private var userId: Long = 0L

    private lateinit var oldPassword: TextInputEditText
    private lateinit var oldPasswordLayout: TextInputLayout
    private lateinit var newPassword: TextInputEditText
    private lateinit var newPasswordLayout: TextInputLayout
    private lateinit var confirmPassword: TextInputEditText
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var nextStepButton: Button
    private lateinit var loadingProgressBar: ProgressBar

    private lateinit var viewModel: ResetPasswordViewModel

    private var userTokenHandlerServiceBinder: UserTokenHandlerService.UserReLoginBinder? = null
    private lateinit var userTokenHandlerServiceIntent: Intent
    private val userTokenHandlerServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            userTokenHandlerServiceBinder = service as UserTokenHandlerService.UserReLoginBinder
            userTokenHandlerServiceBinder?.getService()?.setLoginCallback {
                reLoginHandler(it)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "UserTokenHandlerService disconnected")
            userTokenHandlerServiceBinder = null
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)
        userId = intent.getLongExtra(HomeSecurity.USER_ID, 0L)
        if (userId == 0L) {
            userId = UserAccount.getUserFromSharedPref(this)!!.userId
        }
        oldPassword = findViewById(R.id.resetOldPasswordEditText)
        oldPasswordLayout = findViewById(R.id.resetOldPasswordEditTextLayout)
        newPassword = findViewById(R.id.resetNewPasswordEditText)
        newPasswordLayout = findViewById(R.id.resetNewPasswordEditTextLayout)
        confirmPassword = findViewById(R.id.resetConfirmEditText)
        confirmPasswordLayout = findViewById(R.id.resetConfirmEditTextLayout)
        nextStepButton = findViewById(R.id.resetNextStepButton)
        loadingProgressBar = findViewById(R.id.resetLoading)

        userTokenHandlerServiceIntent = Intent(this, UserTokenHandlerService::class.java)
        startService(userTokenHandlerServiceIntent)
        bindService(
            userTokenHandlerServiceIntent,
            userTokenHandlerServiceConnection,
            BIND_AUTO_CREATE
        )

        viewModel = ViewModelProvider(this).get(ResetPasswordViewModel::class.java)
        viewModel.resetPasswordResult.observe(this, Observer {
            val result = it ?: return@Observer
            loadingProgressBar.visibility = View.INVISIBLE
            showToast(result.stringId)
            if (result.success || result.code == 1)
                startLoginActivity()
            setAllAvailable()
        })
        viewModel.loginNeeded.observe(this, Observer {
            it ?: return@Observer
            if (it) {
                userTokenHandlerServiceBinder?.getService()?.reLogin()
            }
        })
        viewModel.resetPasswordFormState.observe(this, Observer {
            val state = it ?: return@Observer

            nextStepButton.isEnabled = state.isDataValid

            oldPasswordLayout.error = state.oldPasswordError?.let { it1 -> getString(it1) }
            newPasswordLayout.error = state.newPasswordError?.let { it1 -> getString(it1) }
            confirmPasswordLayout.error = state.confirmError?.let { it1 -> getString(it1) }
        })

        oldPassword.afterTextChanged {
            checkFormValid()
        }
        newPassword.afterTextChanged {
            checkFormValid()
        }
        confirmPassword.apply {
            afterTextChanged {
                checkFormValid()
            }
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        if (nextStepButton.isEnabled) {
                            nextStepButton.performClick()
                        }
                    }
                }
                false
            }
        }
        nextStepButton.setOnClickListener {
            resetPassword()
        }
    }

    private fun resetPassword() {
        loadingProgressBar.visibility = View.VISIBLE
        setAllUnavailable()
        viewModel.resetPassword(
            userId,
            oldPassword.text.toString(),
            newPassword.text.toString()
        )
    }

    private fun checkFormValid() {
        viewModel.checkFormState(
            oldPassword.text.toString(),
            newPassword.text.toString(),
            confirmPassword.text.toString()
        )
    }

    private fun setAllAvailable() {
        oldPasswordLayout.isEnabled = true
        newPasswordLayout.isEnabled = true
        confirmPasswordLayout.isEnabled = true
        nextStepButton.isEnabled = true
    }

    private fun setAllUnavailable() {
        oldPasswordLayout.isEnabled = false
        newPasswordLayout.isEnabled = false
        confirmPasswordLayout.isEnabled = false
        nextStepButton.isEnabled = false
    }

    fun reLoginHandler(result: BasicRequestResult): Boolean {
        showToast(result.stringId)
        if (result.loginNeeded) {
            startLoginActivity()
        }
        return true
    }

    /**
     * Start the Login Activity and finish all activities.
     */
    private fun startLoginActivity(bundle: Bundle? = null) {
        WebSocket.lifecycle.stop()
        UserAccount.clearUserFromSharedPref(this)
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            if (bundle != null) {
                putExtras(bundle)
            }
        }
        stopService(userTokenHandlerServiceIntent)
        setResult(Activity.RESULT_OK)
        startActivity(intent)
    }
}