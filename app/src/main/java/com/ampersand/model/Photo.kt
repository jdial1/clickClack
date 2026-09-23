package com.ampersand.model

import java.time.LocalDate

/** A photo kept from a special day. [data] is a base64 JPEG. */
data class DayPhoto(val id: String, val date: LocalDate, val label: String, val byUid: String, val data: String)
