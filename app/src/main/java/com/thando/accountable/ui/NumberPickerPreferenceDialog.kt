package com.thando.accountable.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.NumberPicker
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceDialogFragmentCompat


class NumberPickerPreferenceDialog : PreferenceDialogFragmentCompat() {
    private lateinit var numberPicker: NumberPicker

    override fun onCreateDialogView(context: Context): View {
        numberPicker = NumberPicker(context)
        numberPicker.minValue = NumberPickerPreference.MIN_VALUE
        numberPicker.maxValue = NumberPickerPreference.MAX_VALUE
        numberPicker.wrapSelectorWheel = false

        return numberPicker
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        numberPicker.value = (preference as NumberPickerPreference).getPersistedInt()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            numberPicker.clearFocus()
            val newValue: Int = numberPicker.value
            val bundle = Bundle()
            bundle.putInt("Text_Size", newValue)
            parentFragmentManager.setFragmentResult("text_size_preference", bundle)
            if (preference.callChangeListener(newValue)) {
                (preference as NumberPickerPreference).doPersistInt(newValue)
                preference.summary
            }
        }
    }

    companion object {
        fun newInstance(key: String, targetFragment: Fragment): NumberPickerPreferenceDialog {
            val fragment = NumberPickerPreferenceDialog()
            fragment.setTargetFragment( targetFragment,0)
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}