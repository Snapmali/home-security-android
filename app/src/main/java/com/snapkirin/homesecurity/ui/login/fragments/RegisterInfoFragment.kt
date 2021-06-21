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
import com.snapkirin.homesecurity.HomeSecurity.EMAIL
import com.snapkirin.homesecurity.HomeSecurity.PASSWORD
import com.snapkirin.homesecurity.HomeSecurity.USERNAME
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.ui.login.LoginActivity
import com.snapkirin.homesecurity.ui.login.LoginViewModel
import com.snapkirin.homesecurity.ui.login.model.FragmentTag
import com.snapkirin.homesecurity.ui.util.afterTextChanged
import com.snapkirin.homesecurity.ui.util.showToast

class RegisterInfoFragment : Fragment() {

    companion object {
        fun newInstance(bundle: Bundle?) =
                RegisterInfoFragment().apply {
                    arguments = bundle
                }
    }

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var parentActivity: LoginActivity

    private lateinit var usernameEditText: TextInputEditText
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var emailEditText: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var passwordConfirmEditText: TextInputEditText
    private lateinit var passwordConfirmLayout: TextInputLayout
    private lateinit var nextStepButton: Button
    private lateinit var loading: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_register_info, container, false)

        parentActivity = requireActivity() as LoginActivity

        usernameEditText = root.findViewById(R.id.registerUsername)
        usernameLayout = root.findViewById(R.id.registerUsernameLayout)
        emailEditText = root.findViewById(R.id.registerEmail)
        emailLayout = root.findViewById(R.id.registerEmailLayout)
        passwordEditText = root.findViewById(R.id.registerPassword)
        passwordLayout = root.findViewById(R.id.registerPasswordLayout)
        passwordConfirmEditText = root.findViewById(R.id.registerPasswordConfirm)
        passwordConfirmLayout = root.findViewById(R.id.registerPasswordConfirmLayout)
        nextStepButton = root.findViewById(R.id.registerNextStepButton)
        loading = root.findViewById(R.id.registerInfoLoading)

        loginViewModel = ViewModelProvider(parentActivity).get(LoginViewModel::class.java)

        loginViewModel.registerFormState.observe(viewLifecycleOwner, Observer {
            val state = it ?: return@Observer

            nextStepButton.isEnabled = state.isDataValid

            usernameLayout.error = state.usernameError?.let { it1 -> getString(it1) }
            emailLayout.error = state.emailError?.let { it1 -> getString(it1) }
            passwordLayout.error = state.passwordError?.let { it1 -> getString(it1) }
            passwordConfirmLayout.error = state.confirmError?.let { it1 -> getString(it1) }
        })

        loginViewModel.registerInfoResult.observe(viewLifecycleOwner, Observer {
            val result = it ?: return@Observer

            loading.visibility = View.GONE
            if (it.success) {
                startRegisterVerificationFragment(
                        usernameEditText.text.toString(),
                        emailEditText.text.toString(),
                        passwordEditText.text.toString()
                )
            } else {
                parentActivity.showToast(result.stringId)
            }
            setAllAvailable()
        })

        usernameEditText.afterTextChanged {
            checkTextValid()
        }
        emailEditText.afterTextChanged {
            checkTextValid()
        }
        passwordEditText.afterTextChanged {
            checkTextValid()
        }

        passwordConfirmEditText.apply {
            afterTextChanged {
                checkTextValid()
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
            register()
        }

        return root
    }

    private fun register() {
        loading.visibility = View.VISIBLE
        setAllUnavailable()
        loginViewModel.registerInfo(
                usernameEditText.text.toString(),
                emailEditText.text.toString(),
                passwordEditText.text.toString()
        )
    }

    private fun startRegisterVerificationFragment(username: String, email: String, password: String) {
        val bundle = Bundle().apply {
            putString(USERNAME, username)
            putString(EMAIL, email)
            putString(PASSWORD, password)
        }
        parentActivity.startFragment(
                FragmentTag.REGISTER_INFO,
                FragmentTag.REGISTER_VERIFICATION,
                bundle
        )
    }

    private fun checkTextValid() {
        loginViewModel.registerDataChanged(
                usernameEditText.text.toString(),
                emailEditText.text.toString(),
                passwordEditText.text.toString(),
                passwordConfirmEditText.text.toString()
        )
    }

    private fun setAllAvailable() {
        usernameLayout.isEnabled = true
        emailLayout.isEnabled = true
        passwordLayout.isEnabled = true
        passwordConfirmLayout.isEnabled = true
        nextStepButton.isEnabled = true
    }

    private fun setAllUnavailable() {
        usernameLayout.isEnabled = false
        emailLayout.isEnabled = false
        passwordLayout.isEnabled = false
        passwordConfirmLayout.isEnabled = false
        nextStepButton.isEnabled = false
    }
}