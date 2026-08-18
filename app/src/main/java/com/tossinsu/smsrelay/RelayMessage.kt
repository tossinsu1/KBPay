package com.tossinsu.smsrelay

/**
 * Firebase Realtime Database에 저장되는 메시지 모델.
 * 기본 생성자는 Firebase 역직렬화용으로 필요하다.
 */
data class RelayMessage(
    var from: String = "",   // 원 발신번호
    var body: String = "",   // 문자 본문
    var ts: Long = 0L        // 수신 시각(millis)
)
