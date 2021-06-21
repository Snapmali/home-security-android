package com.snapkirin.homesecurity.ui.login.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ProgressBar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.snapkirin.homesecurity.HomeSecurity.USER
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.User
import com.snapkirin.homesecurity.network.websocket.WebSocket
import com.snapkirin.homesecurity.ui.login.LoginActivity
import com.snapkirin.homesecurity.ui.login.LoginViewModel
import com.snapkirin.homesecurity.ui.login.model.FragmentTag
import com.snapkirin.homesecurity.util.UserAccount
import com.snapkirin.homesecurity.ui.util.afterTextChanged
import com.snapkirin.homesecurity.ui.util.showToast

class LoginFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance(bundle: Bundle?) =
                LoginFragment().apply {
                    arguments = bundle
                }
    }

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var parentActivity: LoginActivity

    private lateinit var identifier: TextInputEditText
    private lateinit var identifierLayout: TextInputLayout
    private lateinit var password: TextInputEditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var loading: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_login, container, false)

        parentActivity = requireActivity() as LoginActivity

        identifier = root.findViewById(R.id.loginIdentifier)
        identifierLayout = root.findViewById(R.id.loginIdentifierLayout)
        password = root.findViewById(R.id.loginPassword)
        passwordLayout = root.findViewById(R.id.loginPasswordLayout)
        loginButton = root.findViewById(R.id.loginButton)
        registerButton = root.findViewById(R.id.registerButton)
        loading = root.findViewById(R.id.loginLoading)

        loginViewModel = ViewModelProvider(parentActivity).get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(viewLifecycleOwner, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            loginButton.isEnabled = loginState.isDataValid

            identifierLayout.error = loginState.identifierError?.let { it1 -> getString(it1) }
            passwordLayout.error = loginState.passwordError?.let { it1 -> getString(it1) }
        })

        loginViewModel.loginResult.observe(viewLifecycleOwner, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            parentActivity.showToast(loginResult.stringId)
            if (loginResult.success) {
                val user = loginResult.user!!
                UserAccount.saveUserToSharedPref(user, requireContext(), password.text.toString())
                WebSocket.lifecycle.start()
                startMainActivity(user)
                return@Observer
            }
            setAllAvailable()
        })

        identifier.afterTextChanged {
            checkFormValid()
        }

        password.apply {
            afterTextChanged {
                checkFormValid()
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        if (loginButton.isEnabled) {
                            loginButton.performClick()
                        }
                    }
                }
                false
            }
        }

        loginButton.setOnClickListener {
            login()
        }

        registerButton.setOnClickListener {
            startRegisterInfoFragment()
        }

        return root
    }

    private fun checkFormValid() {
        loginViewModel.loginDataChanged(
                identifier.text.toString(),
                password.text.toString()
        )
    }

    private fun login() {
        loading.visibility = View.VISIBLE
        setAllUnavailable()
        loginViewModel.login(identifier.text.toString(), password.text.toString())
    }

    private fun startMainActivity(user: User) {
        val bundle = Bundle()
        bundle.putParcelable(USER, user)
        parentActivity.startMainActivity(bundle)
    }

    private fun startRegisterInfoFragment() {
        parentActivity.startFragment(FragmentTag.LOGIN, FragmentTag.REGISTER_INFO, null)
    }

    private fun setAllUnavailable() {
        identifierLayout.isEnabled = false
        passwordLayout.isEnabled = false
        loginButton.isEnabled = false
    }

    private fun setAllAvailable() {
        identifierLayout.isEnabled = true
        passwordLayout.isEnabled = true
        loginButton.isEnabled = true
    }
}