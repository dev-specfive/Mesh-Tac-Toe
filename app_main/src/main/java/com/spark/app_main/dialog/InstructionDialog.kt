package com.spark.app_main.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.spark.app_main.R
import com.spark.app_main.databinding.CustomSimpleInstructionDialogBinding
import com.spark.app_main.extension.gone
import com.spark.app_main.extension.visible
import com.spark.app_main.model.MaterialDialogContent

class InstructionDialog constructor(
    private val materialDialogContent: MaterialDialogContent,
    private val positiveClickClosure: () -> Unit,
    private val negativeClickClosure: () -> Unit = {},
    private val onlyShowNegativeButton: Boolean = false
) : DialogFragment() {
    lateinit var binding: CustomSimpleInstructionDialogBinding
    override fun onStart() {
        super.onStart()
        requireDialog().window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireDialog().window?.setBackgroundDrawableResource(R.drawable.white_curve_with_blue_outline)
        requireDialog().window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#BF303030")))

        isCancelable = false;

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = CustomSimpleInstructionDialogBinding.inflate(inflater, container, false)

        if (materialDialogContent.iconRes != null)
            binding.customSimpleDialogWithLogoContentLogoImageView.setImageResource(
                materialDialogContent.iconRes
            )

        if (materialDialogContent.topTitle != null) {
            binding.topTitleTxt.text = materialDialogContent.topTitle
            binding.topTitleTxt.visible()
        }
        val content = materialDialogContent.content
            ?: if (materialDialogContent.contentRes != null)
                getString(materialDialogContent.contentRes)
            else ""
        binding.customSimpleDialogWithLogoContentTextView.text = content
        binding.customSimpleDialogWithLogoContentTextView.movementMethod =
            ScrollingMovementMethod()

        if (materialDialogContent.negativeButtonText != null) {
            binding.customSimpleInstructionDialogNegativeTextView.setText(
                materialDialogContent.negativeButtonText
            )
            binding.customSimpleInstructionDialogNegativeButton.visible()
            binding.customSimpleInstructionDialogNegativeButton.setOnClickListener {
                dismiss()
                negativeClickClosure.invoke()
            }
        }

        if (onlyShowNegativeButton)
            binding.customSimpleDialogWithLogoContentPositiveButton.gone()
        else {
            binding.customSimpleDialogWithLogoContentPositiveTextView.setText(
                materialDialogContent.positiveButtonText
            )
            binding.customSimpleDialogWithLogoContentPositiveButton.setOnClickListener {
                dismiss()
                positiveClickClosure.invoke()
            }
        }
        binding.customSimpleDialogInstructionDialogClose.setOnClickListener {
            dismiss()
            negativeClickClosure.invoke()
        }
        return binding.root
    }
}