
package com.roxanasultan.memoraid.models

import com.google.firebase.firestore.DocumentId

data class Medicine(
    @DocumentId val id: String = "",
    val name: String = "",
    val date: String = "",
    val time: String = "",
    val dose: String = "",
    val note: String = "",
    var userId: String = "",
    var taken: Boolean = false
)