package com.thando.accountable.recyclerviewadapters

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.OptIn
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.mp4.Track
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.thando.accountable.AccountableRepository
import com.thando.accountable.AccountableRepository.Companion.accountablePlayer
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity.Companion.collectFlow
import com.thando.accountable.MainActivity.Companion.log
import com.thando.accountable.R
import com.thando.accountable.database.tables.AppSettings
import com.thando.accountable.database.tables.Content
import com.thando.accountable.database.tables.Content.ContentType
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.Script
import com.thando.accountable.database.tables.SpecialCharacters
import com.thando.accountable.database.tables.TeleprompterSettings
import com.thando.accountable.databinding.AudioItemBinding
import com.thando.accountable.databinding.DocumentItemBinding
import com.thando.accountable.databinding.ImageItemBinding
import com.thando.accountable.databinding.ScriptContentItemBinding
import com.thando.accountable.databinding.TextItemBinding
import com.thando.accountable.databinding.VideoItemBinding
import com.thando.accountable.fragments.dialogs.AddMediaDialog
import com.thando.accountable.player.TrackItem
import com.thando.accountable.recyclerviewadapters.diffutils.ContentDiffItemCallback
import com.thando.accountable.ui.screens.ContentPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicReference


class ContentItemAdapter(
    private val context: Context,
    val viewLifecycleOwner: LifecycleOwner,
    private val childFragmentManager: FragmentManager,
    private val galleryLauncherMultiple: ActivityResultLauncher<String>? = null,
    private val markupLanguageInitializeScrollUnit:(()->Unit),
    private val script: StateFlow<Script?>,
    private val scriptContentList: StateFlow<MutableList<Content>>,
    val appSettings: StateFlow<AppSettings?>,
    val markupLanguage: StateFlow<MarkupLanguage?>,
    val isEditingScript: MutableStateFlow<Boolean>,
    private val viewModelScope: CoroutineScope,
    private val repository: AccountableRepository,
    private val addTimeStampFunction: MutableStateFlow<((String) -> Unit)?>
): ListAdapter<Content, ContentItemAdapter.ContentItemViewHolder>(ContentDiffItemCallback()) {

    private val viewHolders: MutableList<ContentItemViewHolder> = mutableListOf()

    private var teleprompterSettings: MutableStateFlow<StateFlow<TeleprompterSettings?>?> = MutableStateFlow(null)

    private val multipleContentsStateFlow: MutableStateFlow<List<@JvmSuppressWildcards Uri>?> = MutableStateFlow(null)
    private val multipleContentsJob = AtomicReference<Job?>(null)

    private var storedInputList: List<Content>?=null

    private var appendedFile:Uri?=null

    init {
        var scriptContentListJob: Job? = null
        var markupLanguageJob: Job? = null
        scriptContentListJob = collectFlow(viewLifecycleOwner,scriptContentList){
            submitList(scriptContentList.value)
            appendEditTextIfNecessary()
            markupLanguageJob = collectFlow(viewLifecycleOwner,markupLanguage){
                markupLanguageInitializeScrollUnit.invoke()
                markupLanguageJob?.cancel()
            }
            if (storedInputList!=null){
                setList(storedInputList!!)
                storedInputList = null
            }

            if (appendedFile!=null){
                appendFile(appendedFile!!)
                appendedFile = null
            }
            scriptContentListJob?.cancel()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type.ordinal
    }

    fun getOrdinal(contentType: ContentType): Int{
        return ContentType
            .valueOf(contentType.toString())
            .ordinal
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ContentItemViewHolder {
        return when (viewType) {
            getOrdinal(ContentType.TEXT) -> inflateText(parent)
            getOrdinal(ContentType.IMAGE) -> inflateImage(parent)
            getOrdinal(ContentType.VIDEO) -> inflateVideo(parent)
            getOrdinal(ContentType.DOCUMENT) -> inflateDocument(parent)
            getOrdinal(ContentType.AUDIO) -> inflateAudio(parent)
            getOrdinal(ContentType.SCRIPT) -> inflateScript(parent)
            else -> inflateImage(parent)//todo make an error message view
        }
    }

    override fun onBindViewHolder(holder: ContentItemViewHolder, position: Int) {
        if (!viewHolders.contains(holder)) viewHolders.add(holder)
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ContentItemViewHolder) {
        viewHolders.remove(holder)
        holder.unbind()
        super.onViewRecycled(holder)
    }

    private fun setList(inputContentList: List<Content>){
        val previousSize = scriptContentList.value.size
        clearContentList()
        inputContentList.forEach {
            scriptContentList.value.add(it)
        }
        appendEditTextIfNecessary()
        updateContentList(previousSize)
    }

    fun setTeleprompterSettings(teleprompterSettingsInput: StateFlow<TeleprompterSettings?>){
        teleprompterSettings.value = teleprompterSettingsInput
        viewHolders.forEach { it.setTeleprompterSettings(teleprompterSettingsInput) }
    }

    private fun appendEditTextIfNecessary(previousSize: Int? = scriptContentList.value.size){
        if (previousSize == null) return
        if (scriptContentList.value.lastIndex == -1 || scriptContentList.value.last().type!= ContentType.TEXT){
            // Make a new TextProcessor and add the content's content
            if(script.value!=null && script.value!!.scriptId!=null) {
                scriptContentList.value.add(
                    Content(
                        type = ContentType.TEXT,
                        script = script.value!!.scriptId!!,
                        position = scriptContentList.value.size.toLong()
                    )
                )
            }
        }
        updateContentList(previousSize)
    }

    fun printEntry(title: String){
        val outputStringBuilder = StringBuilder()
        scriptContentList.value.forEach { content ->
            if (content.type == ContentType.TEXT){
                content.content.value.let { outputStringBuilder.append(it) }
            }
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME,
                title
            )
            put(MediaStore.Files.FileColumns.MIME_TYPE,
                "text/plain"
            )
            put(MediaStore.Files.FileColumns.RELATIVE_PATH,
                createFolderInDocuments(context.getString(R.string.app_name))
            )
        }

        val resolver = context.contentResolver
        val uri: Uri? = resolver.insert(
            MediaStore.Files.getContentUri("external"),
            contentValues
        )
        uri?.let {
            val outputStream: OutputStream? = resolver.openOutputStream(it)
            outputStream?.use { stream ->
                stream.write(outputStringBuilder.toString().toByteArray())
                stream.flush()
            }
        }
    }

    private fun createFolderInDocuments(folderName: String): String {
        // Get the Documents directory
        val documentsDir = Environment.DIRECTORY_DOCUMENTS

        // Create the new folder
        val newFolder = File(documentsDir, folderName)

        // Check if the folder already exists, if not, create it
        if (!newFolder.exists()) {
            newFolder.mkdirs()
        }
        return documentsDir + File.separator + folderName
    }

    fun deleteContent(content: Content){
        val previousSize = scriptContentList.value.size
        accountablePlayer.close(content)
        scriptContentList.value.remove(content)
        appendEditTextIfNecessary(previousSize)
        repository.deleteContent(content)
    }

    private fun inflateText(parent: ViewGroup): TextItemViewHolder {
        val binding = TextItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TextItemViewHolder(binding)
    }

    private fun inflateImage(parent: ViewGroup): ImageItemViewHolder {
        val binding = ImageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageItemViewHolder(binding)
    }

    private fun inflateVideo(parent: ViewGroup): VideoItemViewHolder {
        val binding = VideoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoItemViewHolder(binding)
    }

    private fun inflateDocument(parent: ViewGroup): DocumentItemViewHolder {
        val binding = DocumentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DocumentItemViewHolder(binding)
    }

    private fun inflateAudio(parent: ViewGroup): AudioItemViewHolder {
        val binding = AudioItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AudioItemViewHolder(binding)
    }

    private fun inflateScript(parent: ViewGroup): ScriptContentItemViewHolder {
        val binding = ScriptContentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScriptContentItemViewHolder(binding)
    }

    fun multipleContentsCallback(contentList: List<@JvmSuppressWildcards Uri>){
        multipleContentsStateFlow.value = contentList
    }

    private fun getAboveBelowContentType(position: Int):Pair<ContentType?, ContentType?>{
        var above: ContentType? = null
        var below: ContentType? = null
        if (scriptContentList.value.isNotEmpty()){
            if (position!=0){
                above = scriptContentList.value[position-1].type
            }
            if (position!=(scriptContentList.value.size-1)){
                below = scriptContentList.value[position+1].type
            }
        }
        return Pair(above,below)
    }

    abstract inner class ContentItemViewHolder(
        binding: ViewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        val teleprompterSettings = MutableStateFlow<StateFlow<TeleprompterSettings?>?>(null)
        val editTextListener = View.OnFocusChangeListener { editText, hasFocus ->
            if (editText is EditText) {
                if (hasFocus) {
                    addTimeStampFunction.value = { stampString ->
                        val index = editText.selectionStart
                        val stringBuilder = StringBuilder(editText.text.toString())
                        stringBuilder.insert(index, stampString)
                        editText.setText(stringBuilder.toString())
                        editText.setSelection(index+stampString.length)
                    }
                } else {
                    addTimeStampFunction.value = null
                }
            }
        }

        abstract fun bind(item: Content)

        fun setTeleprompterSettings(inputSettings: StateFlow<TeleprompterSettings?>){
            teleprompterSettings.value = inputSettings
        }

        abstract fun unbind()
    }

    inner class TextItemViewHolder(private val binding: TextItemBinding) : ContentItemViewHolder(binding){

        private val jobs: ArrayList<Job> = arrayListOf()

        override fun bind(item: Content)
        {
            binding.task = item
            binding.textItemViewHolder = this
            binding.contentItemAdapter = this@ContentItemAdapter
            binding.lifecycleOwner = viewLifecycleOwner

            jobs.add(collectFlow(viewLifecycleOwner,isEditingScript){
                item.getText(markupLanguage.value,context)
            })

            jobs.add(collectFlow(viewLifecycleOwner,markupLanguage){
                item.getText(it,context)
            })

            binding.editText.setOnLongClickListener { view ->
                onLongClickListenerContent(view,item,binding)
            }
        }

        override fun unbind() {
            jobs.forEach { it.cancel() }
            jobs.clear()
        }
    }

    inner class ImageItemViewHolder(private val binding: ImageItemBinding) : ContentItemViewHolder(binding){

        private val jobs: ArrayList<Job> = arrayListOf()

        override fun bind(
            item: Content
        ) {
            binding.task = item
            binding.imageItemViewHolder = this
            binding.contentItemAdapter = this@ContentItemAdapter
            binding.lifecycleOwner = viewLifecycleOwner

            jobs.add(collectFlow(viewLifecycleOwner,isEditingScript){
                item.getText(markupLanguage.value,context)
            })

            jobs.add(collectFlow(viewLifecycleOwner,markupLanguage){
                item.getText(it,context)
            })

            binding.imageView.setOnLongClickListener { view ->
                onLongClickListenerContent(view,item,null)
            }
        }

        override fun unbind() {
            jobs.forEach { it.cancel() }
            jobs.clear()
        }
    }

    private fun cardViewVisibility(isEditing:Boolean,string: String?):Int{
        return if (!isEditing){
            if (!(string.isNullOrEmpty())
            ) View.VISIBLE
            else View.GONE
        } else View.VISIBLE
    }

    inner class VideoItemViewHolder(private val binding: VideoItemBinding) : ContentItemViewHolder(binding){

        private val jobs: ArrayList<Job> = arrayListOf()

        override fun bind(
            item: Content
        ) {
            binding.task = item
            binding.videoItemViewHolder = this
            binding.contentItemAdapter = this@ContentItemAdapter
            binding.lifecycleOwner = viewLifecycleOwner

            item.getUri(context)?.let { getVideoUri ->
                jobs.add(collectFlow(viewLifecycleOwner,getVideoUri) { videoUri ->
                    if (videoUri==null) return@collectFlow
                    item.trackItem.value = item.id?.let { getVideoMetadata(it,videoUri) }
                })
            }

            jobs.add(collectFlow(viewLifecycleOwner,item.trackItem){ trackItem ->
                if (trackItem!=null){
                    val duration = item.trackItem.value?.duration?.toLongOrNull()
                    binding.videoLengthTextview.text = if (duration==null) item.trackItem.value?.duration else formatMillisToMinutesAndSeconds(duration)

                    binding.videoLengthTextview.visibility = View.VISIBLE
                    if (trackItem.thumbnail!=null){
                        binding.videoThumbnail.setImageBitmap(trackItem.thumbnail)
                    }

                    jobs.add(collectFlow(viewLifecycleOwner,item.isPlaying){
                        if (it && item.trackItem.value!=null){
                            binding.playerView.visibility = View.VISIBLE
                            binding.videoThumbnail.visibility = View.GONE
                            binding.playButton.setImageDrawable(AppCompatResources.getDrawable(context,R.drawable.baseline_close_24))
                            accountablePlayer.addAndPlay(item,binding.playerView,context)
                        }
                        else{
                            if (accountablePlayer.isContentPlaying(item)) {
                                item.isPlaying.value = true
                            }
                            else{
                                accountablePlayer.close(item,binding.playerView)
                                binding.videoThumbnail.visibility = View.VISIBLE
                                binding.playerView.visibility = View.GONE
                                binding.playButton.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.arrow_play))
                            }
                        }
                    })
                }
                else{
                    binding.videoThumbnail.visibility = View.GONE
                    binding.videoLengthTextview.visibility = View.GONE
                }
            })

            jobs.add(collectFlow(viewLifecycleOwner,isEditingScript){
                item.getText(markupLanguage.value,context)
                binding.cardView.visibility = cardViewVisibility(it,binding.task?.description?.value)
            })

            jobs.add(collectFlow(viewLifecycleOwner,markupLanguage){
                item.getText(it,context)
            })

            binding.root.setOnLongClickListener { view ->
                onLongClickListenerContent(view,item,null)
            }
            binding.videoDetailsCardView.setOnLongClickListener{
                onLongClickListenerContent(it,item,null)
            }
            binding.playButton.setOnLongClickListener{
                onLongClickListenerContent(it,item,null)
            }
            binding.videoNameEditText.setOnLongClickListener{
                onLongClickListenerContent(it,item,null)
            }
            binding.editText.setOnLongClickListener{
                onLongClickListenerContent(it,item,null)
            }
        }

        fun playOrClose(){
            binding.task?.isPlaying?.value = binding.task?.isPlaying?.value?.not() == true
        }

        override fun unbind() {
            jobs.forEach { it.cancel() }
            jobs.clear()
        }
    }

    inner class DocumentItemViewHolder(private val binding: DocumentItemBinding) : ContentItemViewHolder(binding){

        private val jobs: ArrayList<Job> = arrayListOf()

        override fun bind(
            item: Content
        ) {
            binding.task = item
            binding.documentItemViewHolder = this
            binding.contentItemAdapter = this@ContentItemAdapter
            binding.lifecycleOwner = viewLifecycleOwner

            jobs.add(collectFlow(viewLifecycleOwner,isEditingScript){
                item.getText(markupLanguage.value,context)
                binding.cardView.visibility = cardViewVisibility(it,binding.task?.description?.value)
            })

            jobs.add(collectFlow(viewLifecycleOwner,markupLanguage){
                item.getText(it,context)
            })

            binding.documentCardView.setOnLongClickListener { view ->
                onLongClickListenerContent(view,item,null)
            }
        }

        override fun unbind() {
            jobs.forEach { it.cancel() }
            jobs.clear()
        }
    }

    inner class AudioItemViewHolder(private val binding: AudioItemBinding) : ContentItemViewHolder(binding){

        private val jobs: ArrayList<Job> = arrayListOf()

        @OptIn(UnstableApi::class)
        override fun bind(
            item: Content
        ) {
            binding.task = item
            binding.audioItemViewHolder = this
            binding.contentItemAdapter = this@ContentItemAdapter
            binding.lifecycleOwner = viewLifecycleOwner

            item.getUri(context)?.let { getAudioUri ->
                jobs.add(collectFlow(viewLifecycleOwner,getAudioUri) { audioUri ->
                    if (audioUri==null) return@collectFlow
                    item.trackItem.value = item.id?.let { getMediaMetadata(it,audioUri) }
                })
            }

            jobs.add(collectFlow(viewLifecycleOwner,item.trackItem){ trackItem ->
                if (trackItem!=null){
                    val duration = item.trackItem.value?.duration?.toLongOrNull()
                    binding.audioLengthTextview.text = if (duration==null) item.trackItem.value?.duration else formatMillisToMinutesAndSeconds(duration)

                    binding.audioAlbum.visibility = View.VISIBLE
                    binding.audioLengthTextview.visibility = View.VISIBLE
                    if (trackItem.thumbnail!=null){
                        binding.audioThumbnail.setImageBitmap(trackItem.thumbnail)
                    }

                    jobs.add(collectFlow(viewLifecycleOwner,item.isPlaying){
                        if (it && item.trackItem.value!=null){
                            binding.playerView.visibility = View.VISIBLE
                            binding.audioThumbnail.visibility = View.GONE
                            binding.playButton.setImageDrawable(AppCompatResources.getDrawable(context,R.drawable.baseline_close_24))
                            accountablePlayer.addAndPlay(item,binding.playerView,context)
                        }
                        else{
                            if (accountablePlayer.isContentPlaying(item)) {
                                item.isPlaying.value = true
                            }
                            else{
                                accountablePlayer.close(item,binding.playerView)
                                binding.audioThumbnail.visibility = View.VISIBLE
                                binding.playerView.visibility = View.GONE
                                binding.playButton.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.arrow_play))
                            }
                        }
                    })
                }
                else{
                    binding.audioAlbum.visibility = View.GONE
                    binding.audioThumbnail.visibility = View.GONE
                    binding.audioLengthTextview.visibility = View.GONE
                }
            })

            jobs.add(collectFlow(viewLifecycleOwner,isEditingScript){
                item.getText(markupLanguage.value,context)
                binding.cardView.visibility = cardViewVisibility(it,binding.task?.description?.value)
            })

            jobs.add(collectFlow(viewLifecycleOwner,markupLanguage){
                item.getText(it,context)
            })

            binding.root.setOnLongClickListener { view ->
                onLongClickListenerContent(view,item,null)
            }
            binding.audioCardView.setOnLongClickListener{
                onLongClickListenerContent(it,item,null)
            }
            binding.playButton.setOnLongClickListener{
                onLongClickListenerContent(it,item,null)
            }
            binding.audioNameEditText.setOnLongClickListener{
                onLongClickListenerContent(it,item,null)
            }
            binding.editText.setOnLongClickListener{
                onLongClickListenerContent(it,item,null)
            }
        }

        fun playOrClose(){
            binding.task?.isPlaying?.value = binding.task?.isPlaying?.value?.not() == true
        }

        override fun unbind() {
            jobs.forEach { it.cancel() }
            jobs.clear()
        }
    }

    @SuppressLint("DefaultLocale")
    fun formatMillisToMinutesAndSeconds(millis: Long): String {
        val minutes = millis / 60000
        val seconds = (millis % 60000) / 1000
        return String.format("%02d:%02d", minutes, seconds)
    }

    suspend fun getVideoMetadata(id:Long,uri: Uri): TrackItem{
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val trackItem = TrackItem(
                id,
                uri,
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown",
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown",
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown",
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?: "Unknown",
                retriever.frameAtTime
            )
            retriever.release()
            trackItem
        }
    }

    suspend fun getMediaMetadata(id:Long,uri: Uri): TrackItem {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val trackItem = TrackItem(
                id,
                uri,
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown",
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown",
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown",
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?: "Unknown",
                AppResources.getBitmapFromByteArray(retriever.embeddedPicture)
            )
            retriever.release()
            trackItem
        }
    }

    inner class ScriptContentItemViewHolder(private val binding: ScriptContentItemBinding) : ContentItemViewHolder(binding){

        private val jobs: ArrayList<Job> = arrayListOf()

        override fun bind(
            item: Content
        ) {
            binding.task = item
            binding.scriptContentItemViewHolder = this
            binding.contentItemAdapter = this@ContentItemAdapter
            binding.lifecycleOwner = viewLifecycleOwner

            jobs.add(collectFlow(viewLifecycleOwner,isEditingScript){
                item.getText(markupLanguage.value,context)
                binding.cardView.visibility = cardViewVisibility(it,binding.task?.description?.value)
            })

            jobs.add(collectFlow(viewLifecycleOwner,markupLanguage){
                item.getText(it,context)
            })

            binding.scriptConstraintLayout.setOnLongClickListener { view ->
                onLongClickListenerContent(view,item,null)
            }
        }

        override fun unbind() {
            jobs.forEach { it.cancel() }
            jobs.clear()
        }
    }

    private fun onLongClickListenerContent(view: View,item: Content,binding: TextItemBinding?):Boolean{
        if (isEditingScript.value && galleryLauncherMultiple!=null) {
            val longClickDialog = AddMediaDialog(
                view,
                galleryLauncherMultiple,
                multipleContentsStateFlow,
                multipleContentsJob,
                context,
                viewLifecycleOwner,
                this,
                item,
                getAboveBelowContentType(scriptContentList.value.indexOf(item))
            ) { multipleContentList, contentType, contentPosition ->
                addContent(multipleContentList,contentType,contentPosition,item,binding)
            }

            // Showing the menu
            longClickDialog.show(childFragmentManager, "Script Edit Text Long Click Dialog")
            return true
        }
        else return false
    }

    private fun addContent(multipleContentList:List<Uri>?, contentType: ContentType, contentPosition:ContentPosition, item: Content, binding: TextItemBinding?){
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val inputIndex = when (contentPosition) {
                    ContentPosition.ABOVE ->
                        scriptContentList.value.indexOf(item)

                    ContentPosition.AT_CURSOR_POINT -> {
                        val inputIndex =
                            scriptContentList.value.indexOf(item) + 1
                        if (binding != null) {
                            // split the string
                            val selectionStart = binding.editText.selectionStart
                            val text = binding.editText.text.toString()

                            // Split the string
                            val topString = text.substring(0, selectionStart)
                            val bottomString =
                                text.substring(selectionStart, text.length)

                            item.content.value = topString
                            val newContent = Content(
                                type = ContentType.TEXT,
                                script = script.value!!.scriptId!!,
                                position = inputIndex.toLong(),
                                content = MutableStateFlow(bottomString)
                            )
                            scriptContentList.value.add(inputIndex, newContent)
                            newContent.id = repository.dao.insert(newContent)
                        } else return@withContext
                        inputIndex
                    }

                    ContentPosition.BELOW ->
                        scriptContentList.value.indexOf(item) + 1
                }
                multipleContentList?.forEach {
                    val newContent = Content(
                        type = contentType,
                        script = script.value!!.scriptId!!,
                        position = inputIndex.toLong(),
                        filename = MutableStateFlow(it.lastPathSegment?:"")
                    )
                    scriptContentList.value.add(inputIndex, newContent)
                    newContent.id = repository.dao.insert(newContent)
                    when (contentType) {
                        ContentType.TEXT,
                        ContentType.SCRIPT -> {}
                        ContentType.IMAGE,
                        ContentType.VIDEO,
                        ContentType.DOCUMENT,
                        ContentType.AUDIO -> newContent.saveFile(context, it)
                    }
                }
                if (multipleContentList == null && (contentType == ContentType.TEXT || contentType == ContentType.SCRIPT)) {
                    val newContent = Content(
                        type = contentType,
                        script = script.value!!.scriptId!!,
                        position = inputIndex.toLong()
                    )
                    scriptContentList.value.add(inputIndex, newContent)
                    newContent.id = repository.dao.insert(newContent)
                }
                withContext(Dispatchers.Main) {
                    appendEditTextIfNecessary()
                }
            }
        }
    }

    private fun clearContentList(){
        scriptContentList.value.clear()
    }

    private fun updateContentList(previousSize: Int){
        val newSize = scriptContentList.value.size
        if (newSize<previousSize){
            notifyItemRangeChanged(0,newSize)
            notifyItemRangeRemoved(newSize,previousSize-newSize)
        }
        else if ( newSize == previousSize) notifyItemRangeChanged(0,newSize)
        else{
            notifyItemRangeChanged(0,previousSize)
            notifyItemRangeInserted(previousSize,newSize-previousSize)
        }
    }

    fun updateSpecialCharacters(specialCharactersList: MutableList<SpecialCharacters>){
        scriptContentList.value.forEach{ content->
            content.replace(
                specialCharactersList,
                context,
                markupLanguage.value,
                isEditingScript.value,
                viewModelScope
            )
        }
    }

    fun appendFile(uri: Uri){
        appendEditTextIfNecessary()
        scriptContentList.value.last().appendFile(uri,context.contentResolver)
    }
}