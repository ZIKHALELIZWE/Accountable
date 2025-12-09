package com.thando.accountable.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import java.util.concurrent.atomic.AtomicReference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.MainActivity.Companion.collectFlow
import com.thando.accountable.R
import com.thando.accountable.database.tables.AppSettings
import com.thando.accountable.fragments.viewmodels.AppSettingsViewModel
import com.thando.accountable.ui.NumberPickerPreference
import com.thando.accountable.ui.NumberPickerPreferenceDialog
import kotlinx.coroutines.launch
import kotlin.text.get

class AppSettingsFragment : PreferenceFragmentCompat() {

    private val viewModel : AppSettingsViewModel by viewModels { AppSettingsViewModel.Factory }
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { galleryUri ->
        try{
            viewModel.setCustomImage(galleryUri)
        }catch(e:Exception){
            e.printStackTrace()
        }
    }
    private var pushNotificationUnit:AtomicReference<(()->Unit)?> = AtomicReference(null)
    private val pushNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pushNotificationUnit.get()?.invoke()
        }
    }
    private val getResultRestoreBackup = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if (result.resultCode == Activity.RESULT_OK)
            viewModel.restoreFromBackup(result.data, pushNotificationPermissionLauncher,pushNotificationUnit)
    }
    private val getResultMakeBackup = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if (result.resultCode == Activity.RESULT_OK)
            viewModel.makeBackup(result.data, pushNotificationPermissionLauncher,pushNotificationUnit)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val mainActivity = (requireActivity() as MainActivity)
        mainActivity.viewModel.toolbarVisible.value = true

        collectFlow(this,viewModel.appSettings) { appSettings ->
            val mainDisplayImagePreference = findPreference<ListPreference>("main_image_setting")

            if (appSettings?.getMainPicture() == AppSettings.DEFAULT_IMAGE_ID){
                mainDisplayImagePreference?.setValueIndex(0)
            }
            else{
                mainDisplayImagePreference?.setValueIndex(1)
            }


            appSettings?.getUri(requireContext())?.let {
                collectFlow(this,it) { uri ->
                    mainDisplayImagePreference?.icon = uri?.let { drawable ->
                        AppResources.getDrawableFromUri(requireContext(), drawable)
                    }
                }
            }

            mainDisplayImagePreference?.setOnPreferenceChangeListener { _, newValue ->
                val index = mainDisplayImagePreference.findIndexOfValue(newValue.toString())
                if (index==0) {
                    // Restore default image
                    viewModel.chooseDefaultImage()
                }
                else{
                    // Select custom image
                    viewModel.chooseImage()
                }
                true
            }
        }

        collectFlow(this,viewModel.navigateToChooseImage) { chooseImage ->
            if (chooseImage){
                galleryLauncher.launch(AppResources.ContentTypeAccessor[AppResources.ContentType.IMAGE])
            }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onResume() {
        val mainActivity = (requireActivity() as MainActivity)
        mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    lifecycleScope.launch {
                        if (!mainActivity.viewModel.toggleDrawer(false)){
                            isEnabled = false
                            mainActivity.onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            }
        )

        val restoreFromBackupPreference = findPreference<Preference>("restore_from_backup")
        restoreFromBackupPreference?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            getResultRestoreBackup.launch(intent)
            true
        }

        val makeBackupPreference = findPreference<Preference>("make_backup")
        makeBackupPreference?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            getResultMakeBackup.launch(intent)
            true
        }

        super.onResume()
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (parentFragmentManager.findFragmentByTag("text_size_preference") != null) {
            return
        }
        if (preference is NumberPickerPreference) {
            val dialog = NumberPickerPreferenceDialog.newInstance(preference.key, this)
            parentFragmentManager.setFragmentResultListener(
                "text_size_preference",
                viewLifecycleOwner
            ) { requestKey, result ->
                if (requestKey == "text_size_preference"){
                    viewModel.setTextSize(result.getInt("Text_Size"))
                }
            }

            dialog.show(parentFragmentManager, "text_size_preference")
        } else
            super.onDisplayPreferenceDialog(preference)
    }
}