package com.thando.accountable.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.fragments.viewmodels.EditFolderViewModel
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.launch

class EditFolderFragment : Fragment() {
    val viewModel : EditFolderViewModel by viewModels { EditFolderViewModel.Factory }
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { galleryUri ->
        try{
            viewModel.setImage(galleryUri)
        }catch(e:Exception){
            e.printStackTrace()
        }
    }
    private var notInitialized = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AccountableTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        EditFolderFragmentView(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }

    @Composable
    fun EditFolderFragmentView(modifier: Modifier = Modifier){
        val editFolder by viewModel.editFolder.collectAsStateWithLifecycle()
        if (notInitialized) {
            viewModel.initializeEditFolder(editFolder)
            notInitialized = false
        }
        val newEditFolder by viewModel.newEditFolder.collectAsStateWithLifecycle()
        val updateButtonTextResId by viewModel.updateButtonText.collectAsStateWithLifecycle()

        if (newEditFolder != null) {
            val uri by newEditFolder!!.getUri(requireContext()).collectAsStateWithLifecycle()
            val folderName by newEditFolder!!.folderName.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()
            Column(
                modifier = modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // folder image
                uri?.let {
                    Image(
                        bitmap = AppResources.getBitmapFromUri(requireContext(), it)?.asImageBitmap()
                            ?: ImageBitmap(1,1),
                        contentDescription = stringResource(R.string.folder_image),
                        contentScale = ContentScale.FillWidth
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                // folder name edit text
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { scope.launch { newEditFolder!!.updateFolderName(it) } },
                    label = { Text(stringResource(R.string.enter_folder_name)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                // choose image button
                Button(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    enabled = viewModel.newEditFolderViewEnabled(newEditFolder)
                ) {
                    Text(stringResource(R.string.choose_image))
                }
                Spacer(modifier = Modifier.width(2.dp))
                // remove image button
                if (uri != null) {
                    Button(
                        onClick = {
                            viewModel.removeImage()
                        },
                        enabled = viewModel.newEditFolderViewEnabled(newEditFolder)
                    ) {
                        Text(stringResource(R.string.remove_image))
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }
                // update folder button
                Button(
                    onClick = {
                        viewModel.saveAndCloseFolder()
                    },
                    enabled = viewModel.setUpdateFolderButtonEnabled(folderName)
                ) {
                    Text(stringResource(updateButtonTextResId?.resId ?: R.string.add_folder))
                }
            }
        }
    }

    override fun onResume() {
        val mainActivity = (requireActivity() as MainActivity)
        mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                    viewModel.closeFolder()
                }
            }
        )
        super.onResume()
    }
}