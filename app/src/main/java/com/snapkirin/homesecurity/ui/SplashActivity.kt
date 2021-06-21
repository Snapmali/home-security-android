package com.snapkirin.homesecurity.ui

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import com.snapkirin.homesecurity.HomeSecurity.USER
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.User
import com.snapkirin.homesecurity.network.websocket.WebSocket
import com.snapkirin.homesecurity.ui.login.LoginActivity
import com.snapkirin.homesecurity.ui.main.MainActivity
import com.snapkirin.homesecurity.util.UserAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        GlobalScope.launch(Dispatchers.IO) {
            login()
        }
    }

    private fun startMainActivity(user: User) {
        val intent = Intent(this, MainActivity::class.java)
        val bundle = Bundle()
        bundle.putParcelable(USER, user)
        intent.putExtras(bundle)
        startActivity(intent)
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        setResult(Activity.RESULT_OK)
        finish()
    }

    private suspend fun login() {
        val result = UserAccount.userLogin(this)
        if (result == null) {
            withContext(Dispatchers.Main) {
                startLoginActivity()
            }
            return
        } else {
            if (result.success) {
                val user = result.user!!
                WebSocket.lifecycle.start()
                withContext(Dispatchers.Main) {
                    startMainActivity(user)
                }
                return
            } else {
                withContext(Dispatchers.Main) {
                    startLoginActivity()
                }
                return
            }
        }
    }
}