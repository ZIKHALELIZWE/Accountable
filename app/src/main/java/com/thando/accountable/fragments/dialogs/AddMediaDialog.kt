package com.thando.accountable.fragments.dialogs

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentManager
import java.util.concurrent.atomic.AtomicReference
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.MainActivity.Companion.collectFlow
import com.thando.accountable.R
import com.thando.accountable.database.tables.Content
import com.thando.accountable.databinding.ScriptEditTextLongClickDialogBinding
import com.thando.accountable.fragments.ScriptFragment
import com.thando.accountable.recyclerviewadapters.ContentItemAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow


class AddMediaDialog(
    private val view: View,
    private val galleryLauncherMultiple: ActivityResultLauncher<String>,
    private val multipleContentsStateFlow: MutableStateFlow<List<@JvmSuppressWildcards Uri>?>,
    private val multipleContentsJob: AtomicReference<Job?>,
    private val fragmentContext: Context,
    private val fragmentViewLifecycleOwner: LifecycleOwner,
    private val contentItemAdapter: ContentItemAdapter,
    private val item: Content,
    private val aboveBelowContentType: Pair<Content.ContentType?,Content.ContentType?>,
    private val processResults: (List<Uri>?, Content.ContentType, ScriptFragment.ContentPosition) -> Unit
): BottomSheetDialogFragment() {

    private val contentType: Content.ContentType = item.type
    private var accessor: String? = null
    private var nonMediaType: NonMediaType? = null
    private var chosenContentType: Content.ContentType? = null
    private val accessorChosen = MutableStateFlow(false)
    private val deleteChosen = MutableStateFlow(false)
    private enum class NonMediaType{ TEXT,SCRIPT }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = ScriptEditTextLongClickDialogBinding.inflate(inflater,container,false)

        collectFlow(viewLifecycleOwner,accessorChosen){ accessorChosen ->
            if (accessorChosen){
                binding.mediaTypeGroup.visibility = View.GONE
                binding.deleteGroup.visibility = View.GONE
                binding.positionGroup.visibility = View.VISIBLE
            }
            else{
                binding.positionGroup.visibility = View.GONE
                binding.deleteGroup.visibility = View.GONE
                binding.mediaTypeGroup.visibility = View.VISIBLE
            }
        }

        collectFlow(viewLifecycleOwner,deleteChosen){ deleteChosen ->
            if (deleteChosen){
                binding.mediaTypeGroup.visibility = View.GONE
                binding.positionGroup.visibility = View.GONE
                binding.deleteGroup.visibility = View.VISIBLE
            }
            else{
                binding.positionGroup.visibility = View.GONE
                binding.deleteGroup.visibility = View.GONE
                binding.mediaTypeGroup.visibility = View.VISIBLE
            }
        }

        if (contentType == Content.ContentType.TEXT ||
            (aboveBelowContentType.first == Content.ContentType.TEXT &&
                    aboveBelowContentType.second == Content.ContentType.TEXT)
            ) binding.addText.visibility = View.GONE
        else {
            if (aboveBelowContentType.first == Content.ContentType.TEXT) binding.addAboveButton.visibility = View.GONE
            if (aboveBelowContentType.second == Content.ContentType.TEXT) binding.addBelowButton.visibility = View.GONE
            binding.addAtCursorPointButton.visibility = View.GONE
        }

        binding.addText.setOnClickListener {
            setAccessor(Content.ContentType.TEXT)
        }

        binding.addPicturesButton.setOnClickListener {
            setAccessor(Content.ContentType.IMAGE)
        }

        binding.addAudiosButton.setOnClickListener {
            setAccessor(Content.ContentType.AUDIO)
        }

        binding.addVideosButton.setOnClickListener {
            setAccessor(Content.ContentType.VIDEO)
        }

        binding.addDocumentsButton.setOnClickListener {
            setAccessor(Content.ContentType.DOCUMENT)
        }

        binding.addScriptButton.setOnClickListener {
            setAccessor(Content.ContentType.SCRIPT)
        }

        binding.addAboveButton.setOnClickListener {
            addContentView(ScriptFragment.ContentPosition.ABOVE)
        }

        binding.addAtCursorPointButton.setOnClickListener {
            addContentView(ScriptFragment.ContentPosition.AT_CURSOR_POINT)
        }

        binding.addBelowButton.setOnClickListener {
            addContentView(ScriptFragment.ContentPosition.BELOW)
        }

        binding.deleteButton.setOnClickListener {
            val contentViewHolder = contentItemAdapter.onCreateViewHolder(binding.itemConfirmationPreview,contentItemAdapter.getOrdinal(item.type))
            contentViewHolder.bind(item)
            binding.deleteConfirmationButton.text = getString(
                R.string.delete_content,
                when (item.type) {
                    Content.ContentType.TEXT -> getString(R.string.text)
                    Content.ContentType.IMAGE -> getString(R.string.image)
                    Content.ContentType.SCRIPT -> getString(R.string.script)
                    Content.ContentType.VIDEO -> getString(R.string.video)
                    Content.ContentType.DOCUMENT -> getString(R.string.document)
                    Content.ContentType.AUDIO -> getString(R.string.audio)
                }
            )
            val interceptView = InterceptTouchEventFrameLayout(fragmentContext)
            binding.itemConfirmationPreview.addView(interceptView)
            interceptView.addView(contentViewHolder.itemView)

            deleteChosen.value = true
        }

        binding.deleteConfirmationButton.setOnClickListener {
            contentItemAdapter.deleteContent(item)
            dismiss()
        }

        binding.cancelConfirmationButton.setOnClickListener {
            dismiss()
        }

        val mainActivity = (requireActivity() as MainActivity)
        mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                    if (accessorChosen.value){
                        nonMediaType = null
                        accessor = null
                        chosenContentType = null
                        accessorChosen.value = false
                    }
                    else{
                        isEnabled = false
                        mainActivity.onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )

        return binding.root
    }

    class InterceptTouchEventFrameLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
    ) : FrameLayout(context, attrs) {
        private var interceptTouchEvents = true

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            return interceptTouchEvents || super.onInterceptTouchEvent(ev)
        }
    }

    private fun setAccessor(contentType: Content.ContentType){
        chosenContentType = contentType
        accessor = when(contentType){
            Content.ContentType.TEXT -> null
            Content.ContentType.IMAGE -> AppResources.ContentTypeAccessor[AppResources.ContentType.IMAGE]
            Content.ContentType.SCRIPT -> null
            Content.ContentType.VIDEO -> AppResources.ContentTypeAccessor[AppResources.ContentType.VIDEO]
            Content.ContentType.DOCUMENT -> AppResources.ContentTypeAccessor[AppResources.ContentType.DOCUMENT]
            Content.ContentType.AUDIO -> AppResources.ContentTypeAccessor[AppResources.ContentType.AUDIO]
        }
        nonMediaType = when(contentType){
            Content.ContentType.TEXT -> NonMediaType.TEXT
            Content.ContentType.IMAGE -> null
            Content.ContentType.SCRIPT -> NonMediaType.SCRIPT
            Content.ContentType.VIDEO -> null
            Content.ContentType.DOCUMENT -> null
            Content.ContentType.AUDIO -> null
        }
        accessorChosen.value = true
    }

    private fun addContentView(position: ScriptFragment.ContentPosition){
        if (accessor!=null && chosenContentType!=null) getMultipleContent(accessor!!,chosenContentType!!,position)
        else if (nonMediaType!=null && chosenContentType!=null) getMultipleContent( null, chosenContentType!!, position)
        this.dismiss()
    }

    private fun getMultipleContent(mediaType:String?, contentType: Content.ContentType, position: ScriptFragment.ContentPosition){
        if (mediaType!=null){
            multipleContentsJob.set(collectFlow(fragmentViewLifecycleOwner,multipleContentsStateFlow){ list ->
                    if (list!=null){
                        processResults( list, contentType, position)
                        multipleContentsStateFlow.value = null
                        multipleContentsJob.get()?.cancel()
                    }
                }
            )
            galleryLauncherMultiple.launch(mediaType)
        }
        else {
            processResults( null, contentType, position)
            multipleContentsJob.get()?.cancel()
            multipleContentsStateFlow.value = null
        }
    }

    private fun closeKeyboard(){
        val inputMethodManager = fragmentContext.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun show(manager: FragmentManager, tag: String?) {
        closeKeyboard()
        super.show(manager, tag)
    }
}