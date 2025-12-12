package com.slooshfilm.app.ui.comments

import com.slooshfilm.app.data.hdrezka.Comment

data class CommentItemState(
    val comment: Comment,
    var isSpoilerRevealed: Boolean = false
)
