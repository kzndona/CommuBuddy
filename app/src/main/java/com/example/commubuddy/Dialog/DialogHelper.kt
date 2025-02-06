package com.example.commubuddy.Dialog

import android.app.Activity
import android.app.Dialog
import android.widget.Button
import com.example.commubuddy.R

object DialogHelper {

    fun showPermissionDialog(
        activity: Activity,
        onPositiveAction: () -> Unit
    ) {
        val dialog = Dialog(activity)
        dialog.setContentView(R.layout.dialog_location_permission) // Your custom layout
        dialog.setCancelable(false)

        val positiveButton = dialog.findViewById<Button>(R.id.button_dialog_location_permission)

        positiveButton.setOnClickListener {
            onPositiveAction()
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showGPSDialog(
        activity: Activity,
        onPositiveAction: () -> Unit
    ) {
        val dialog = Dialog(activity)
        dialog.setContentView(R.layout.dialog_location_service) // Your custom layout
        dialog.setCancelable(false)

        val positiveButton = dialog.findViewById<Button>(R.id.button_dialog_location_service)

        positiveButton.setOnClickListener {
            onPositiveAction()
            dialog.dismiss()
        }

        dialog.show()
    }
}
