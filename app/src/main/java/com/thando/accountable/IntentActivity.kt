package com.thando.accountable

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.constraintlayout.widget.ConstraintLayout
import com.thando.accountable.MainActivity.Companion.collectFlow
import com.thando.accountable.MainActivity.ResourceProvider
import com.thando.accountable.databinding.StringIntentDialogBinding
import com.thando.accountable.fragments.FoldersAndScriptsFragment


class IntentActivity : AppCompatActivity() {

    val viewModel: IntentActivityViewModel by viewModels {
        IntentActivityViewModel.Factory(
            if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            } else null
        )
    }
    lateinit var dialogFragment: DialogFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ResourceProvider.init(this.applicationContext)

        val view = ConstraintLayout(applicationContext)
        view.setBackgroundColor(Color.Transparent.toArgb())
        setContentView(view)
        dialogFragment = DialogFragment(intent,viewModel){
            this.finishAndRemoveTask()
        }
        dialogFragment.show(supportFragmentManager, DialogFragment.TAG)
    }

    class DialogFragment(
        private val intent: Intent,
        private val viewModel: IntentActivityViewModel,
        private val onDismissed:()->Unit
    ) : androidx.fragment.app.DialogFragment() {
        companion object{ const val TAG = "IntentDialogFragment" }
        private var _binding: StringIntentDialogBinding? = null
        private val binding get() = _binding!!

        private lateinit var foldersAndScriptsFragment : FoldersAndScriptsFragment

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            _binding = StringIntentDialogBinding.inflate(layoutInflater)
            binding.lifecycleOwner = this

            if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                if(!intent.getStringExtra(Intent.EXTRA_TEXT).isNullOrEmpty()) {
                    for (i in 0 until childFragmentManager.backStackEntryCount) {
                        childFragmentManager.popBackStack()
                    }
                    foldersAndScriptsFragment = FoldersAndScriptsFragment()
                    val transaction = childFragmentManager.beginTransaction()
                    transaction.replace(R.id.fragment_container, foldersAndScriptsFragment)
                    transaction.commit()
                }
                else {
                    Toast.makeText(requireContext(),
                        getString(R.string.no_text_shared),Toast.LENGTH_LONG).show()
                    this.dismiss()
                }

                collectFlow(this, viewModel.direction) { direction ->
                    if (direction != null){
                        viewModel.navController.navigateTo( R.id.fragment_container, direction,childFragmentManager)
                    }
                }
            }
            else{
                Toast.makeText(requireContext(),
                    getString(R.string.no_text_shared),Toast.LENGTH_LONG).show()
                this.dismiss()
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            return binding.root
        }

        override fun onResume() {
            val width = resources.displayMetrics.widthPixels
            val height = resources.displayMetrics.heightPixels*0.9
            dialog?.window?.setLayout(width, height.toInt())
            super.onResume()
        }

        override fun onDismiss(dialog: DialogInterface) {
            super.onDismiss(dialog)
            onDismissed.invoke()
        }

        override fun onDestroy() {
            super.onDestroy()
            _binding = null
        }
    }

    override fun onDestroy() {
        AccountableRepository.getInstance(application).intentString = null
        super.onDestroy()
    }
}