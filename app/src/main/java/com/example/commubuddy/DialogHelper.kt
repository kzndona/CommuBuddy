package com.example.commubuddy

import android.app.Activity
import android.app.Dialog
import android.widget.Button

object DialogHelper {

    fun showPermissionDialog(
        activity: Activity,
        onPositiveAction: () -> Unit,
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

//    fun showGPSDialog(
//        activity: Activity,
//        onPositiveAction: () -> Unit,
//        onNegativeAction: () -> Unit = {}
//    ) {
//        val dialog = Dialog(activity)
//        dialog.setContentView(R.layout.dialog_gps) // Your custom layout
//        dialog.setCancelable(false)
//
//        val positiveButton = dialog.findViewById<Button>(R.id.btn_positive)
//        val negativeButton = dialog.findViewById<Button>(R.id.btn_negative)
//
//        positiveButton.setOnClickListener {
//            onPositiveAction()
//            dialog.dismiss()
//        }
//
//        negativeButton.setOnClickListener {
//            onNegativeAction()
//            dialog.dismiss()
//        }
//
//        dialog.show()
//    }
}
