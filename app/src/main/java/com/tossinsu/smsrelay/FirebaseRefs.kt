package com.tossinsu.smsrelay

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * RTDB 경로 헬퍼.
 * 구조: /relay/{pairCode}/messages/{pushId} = RelayMessage
 * pairCode 는 두 폰에 동일하게 입력하는 공유 코드.
 */
object FirebaseRefs {
    fun messages(pairCode: String): DatabaseReference =
        Firebase.database.reference
            .child("relay")
            .child(pairCode.ifBlank { "default" })
            .child("messages")
}
