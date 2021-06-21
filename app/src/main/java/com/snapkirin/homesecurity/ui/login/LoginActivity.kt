package com.snapkirin.homesecurity.ui.login

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction

import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.network.websocket.WebSocket
import com.snapkirin.homesecurity.ui.login.fragments.LoginFragment
import com.snapkirin.homesecurity.ui.login.fragments.RegisterInfoFragment
import com.snapkirin.homesecurity.ui.login.fragments.RegisterVerificationFragment
import com.snapkirin.homesecurity.ui.main.MainActivity
import com.snapkirin.homesecurity.ui.login.model.FragmentTag

class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        WebSocket.lifecycle.stop()

        loginViewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

        if (supportFragmentManager.findFragmentById(R.id.loginFragmentContainer) == null) {
            startFragment(null, FragmentTag.LOGIN, null)
        }
        loginViewModel.currentFragment?.let { setActionBarTitle(it) }
    }

    fun startMainActivity(bundle: Bundle) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtras(bundle)
        })
        setResult(Activity.RESULT_OK)
        finish()
    }

    fun startFragment(curFragmentTag: FragmentTag?, newFragmentTag: FragmentTag, bundle: Bundle?) {
        val oldFragment = supportFragmentManager.findFragmentByTag(curFragmentTag?.name)
        val newFragment = supportFragmentManager.findFragmentByTag(newFragmentTag.name)
        supportFragmentManager
            .beginTransaction()
            .apply {
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                if (oldFragment != null)
                    hide(oldFragment)
                if (newFragment != null) {
                    show(newFragment)
                } else
                    add(
                        R.id.loginFragmentContainer,
                        getFragmentInstance(newFragmentTag, bundle),
                        newFragmentTag.name
                    )
                commit()
            }
        setActionBarTitle(newFragmentTag)
        loginViewModel.currentFragment = newFragmentTag
    }

    private fun getFragmentInstance(fragmentTag: FragmentTag, bundle: Bundle?): Fragment {
        return when (fragmentTag) {
            FragmentTag.LOGIN -> LoginFragment.newInstance(bundle)
            FragmentTag.REGISTER_INFO -> RegisterInfoFragment.newInstance(bundle)
            else -> RegisterVerificationFragment.newInstance(bundle)
        }
    }

    fun removeFragmentInstance(fragmentTag: FragmentTag): Boolean {
        supportFragmentManager.findFragmentByTag(fragmentTag.name)?.let {
            supportFragmentManager
                .beginTransaction()
                .apply {
                    remove(it)
                    commit()
                }
            loginViewModel.clearStates(fragmentTag)
            return true
        }
        return false
    }

    private fun setActionBarTitle(fragmentTag: FragmentTag) {
        supportActionBar?.apply {
            when (fragmentTag) {
                FragmentTag.LOGIN -> {
                    this.setTitle(R.string.login)
                    this.setDisplayHomeAsUpEnabled(false)
                }
                FragmentTag.REGISTER_INFO -> {
                    this.setTitle(R.string.register)
                    this.setDisplayHomeAsUpEnabled(true)
                }
                else -> {
                    this.setTitle(R.string.register)
                    this.setDisplayHomeAsUpEnabled(true)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        when (loginViewModel.currentFragment) {
            FragmentTag.REGISTER_INFO -> {
                startFragment(FragmentTag.REGISTER_INFO, FragmentTag.LOGIN, null)
                return
            }
            FragmentTag.REGISTER_VERIFICATION -> {
                startFragment(FragmentTag.REGISTER_VERIFICATION, FragmentTag.REGISTER_INFO, null)
                removeFragmentInstance(FragmentTag.REGISTER_VERIFICATION)
                return
            }
            else -> {
                super.onBackPressed()
                return
            }
        }
    }
}

