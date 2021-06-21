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

class RegisterVerificationFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance(username: String, email: String, password: String) =
                RegisterVerificationFragment().apply {
                    arguments = Bundle().apply {
                        putString(USERNAME, username)
                        putString(EMAIL, email)
                        putString(PASSWORD, password)
                    }
                }

        @JvmStatic
        fun newInstance(bundle: Bundle?) =
                RegisterVerificationFragment().apply {
                    arguments = bundle
                }
    }

    private var username: String? = null
    private var email: String? = null
    private var password: String? = null

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var verificationViewModel: VerificationViewModel
    private lateinit var parentActivity: LoginActivity

    private lateinit var codeEditText: TextInputEditText
    private lateinit var codeLayout: TextInputLayout
    private lateinit var verificationButton: Button
    private lateinit var resendButton: Button
    private lateinit var loading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            username = it.getString(USERNAME)
            email = it.getString(EMAIL)
            password = it.getString(PASSWORD)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_register_verification, container, false)

        parentActivity = requireActivity() as LoginActivity

        codeEditText = root.findViewById(R.id.registerVerificationCode)
        codeLayout = root.findViewById(R.id.registerVerificationCodeLayout)
        verificationButton = root.findViewById(R.id.registerVerificationButton)
        resendButton = root.findViewById(R.id.registerResendButton)
        loading = root.findViewById(R.id.registerVerificationLoading)

        loginViewModel = ViewModelProvider(parentActivity).get(LoginViewModel::class.java)

        loginViewModel.verificationCodeState.observe(viewLifecycleOwner, Observer {
            val formState = it ?: return@Observer

            verificationButton.isEnabled = formState.isDataValid

            codeLayout.error = formState.error?.let { it1 -> getString(it1) }
        })

        loginViewModel.registerVerificationResult.observe(viewLifecycleOwner, Observer {
            val result = it ?: return@Observer

            loading.visibility = View.GONE
            parentActivity.showToast(result.stringId)
            if (it.success) {
                returnToLoginFragment()
                return@Observer
            }
            setAllAvailable()
        })

        loginViewModel.resendCodeResult.observe(viewLifecycleOwner, Observer {
            val result = it ?: return@Observer
            parentActivity.showToast(result.stringId)
        })

        verificationViewModel = ViewModelProvider(parentActivity).get(VerificationViewModel::class.java)

        verificationViewModel.countDownTime.observe(viewLifecycleOwner, Observer {
            val countDownTime = it ?: return@Observer

            if (countDownTime > 0) {
                resendButton.isEnabled = false
                val countDownText = "${getString(R.string.resend_code)} (${countDownTime}s)"
                resendButton.text = countDownText
            } else {
                resendButton.isEnabled = true
                resendButton.text = getString(R.string.resend_code)
            }
        })

        verificationViewModel.startCountDown()

        codeEditText.apply {
            afterTextChanged {
                checkTextValid()
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        if (verificationButton.isEnabled) {
                            verificationButton.performClick()
                        }
                    }
                }
                false
            }
        }

        verificationButton.setOnClickListener {
            verify()
        }

        resendButton.setOnClickListener {
            resendCode()
        }

        return root
    }

    private fun verify() {
        loading.visibility = View.VISIBLE
        setAllUnavailable()
        username?.let { loginViewModel.registerVerification(it, codeEditText.text.toString()) }
    }

    private fun resendCode() {
        verificationViewModel.startCountDown()
        resendButton.isEnabled = false
        username?.let { email?.let { it1 -> password?.let { it2 -> loginViewModel.resendCode(it, it1, it2) } } }
    }

    private fun returnToLoginFragment(){
        parentActivity.apply {
            startFragment(
                    FragmentTag.REGISTER_VERIFICATION,
                    FragmentTag.LOGIN,
                    null
            )
            removeFragmentInstance(FragmentTag.REGISTER_INFO)
            removeFragmentInstance(FragmentTag.REGISTER_VERIFICATION)
        }
    }

    private fun checkTextValid() {
        loginViewModel.verificationCodeChanged(
                codeEditText.text.toString()
        )
    }

    private fun setAllAvailable() {
        codeLayout.isEnabled = true
        verificationButton.isEnabled = true
        resendButton.isEnabled = true
    }

    private fun setAllUnavailable() {
        codeLayout.isEnabled = false
        verificationButton.isEnabled = false
        resendButton.isEnabled = false
    }

}