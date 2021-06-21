package com.snapkirin.homesecurity.ui.devicedetail.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.ui.devicedetail.DeviceDetailViewModel
import com.snapkirin.homesecurity.ui.util.afterTextChanged


class RenameDeviceDialog: DialogFragment() {

    private lateinit var renameTextLayout: TextInputLayout
    private lateinit var renameText: TextInputEditText
    private lateinit var positiveButton: Button
    private lateinit var negativeButton: Button

    private lateinit var renameDeviceViewModel: RenameDeviceViewModel
    private lateinit var deviceDetailViewModel: DeviceDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =  inflater.inflate(R.layout.dialog_rename_device, container, false)
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog!!.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renameDeviceViewModel = ViewModelProvider(this).get(RenameDeviceViewModel::class.java)
        deviceDetailViewModel = ViewModelProvider(requireActivity()).get(DeviceDetailViewModel::class.java)
        renameTextLayout = view.findViewById(R.id.deviceRenameTextLayout)
        renameText = view.findViewById(R.id.deviceRenameText)
        positiveButton = view.findViewById(R.id.deviceRenamePositiveButton)
        negativeButton = view.findViewById(R.id.deviceRenameNegativeButton)
        positiveButton.isEnabled = false
        renameDeviceViewModel.deviceScreenNameFormState.observe(viewLifecycleOwner, Observer {
            val state = it ?: return@Observer
            positiveButton.isEnabled = state.isDataValid
            renameTextLayout.error = state.error?.let { it1 -> getString(it1) }
        })
        positiveButton.setOnClickListener {
            deviceDetailViewModel.renameDevice(renameText.text.toString())
            dialog?.dismiss() ?: dismiss()
        }
        negativeButton.setOnClickListener {
            dialog?.dismiss() ?: dismiss()
        }
        renameText.afterTextChanged {
            renameDeviceViewModel.deviceScreenNameTextChanged(it)
        }
    }
}