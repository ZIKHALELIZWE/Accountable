package com.thando.accountable.player

import android.graphics.Bitmap
import android.net.Uri

data class TrackItem (
    var id: Long,
    var audioUrl: Uri,
    var title: String,
    var artistName: String,
    var album:String,
    var duration: String,
    var thumbnail: Bitmap?
)