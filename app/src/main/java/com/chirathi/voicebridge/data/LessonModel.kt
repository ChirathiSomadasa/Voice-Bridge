//package com.chirathi.voicebridge.data
//
//import android.content.Context
//import com.google.firebase.firestore.DocumentId
//
//data class LessonModel(
//    @DocumentId
//    var id: String = "", // Firestore ID
//
//    val ageGroup: String = "",
//    val subject: String = "",
//    val disorderType: String = "",
//    val lessonTitle: String = "",
//    val lessonContent: String = "",
//    val question: String = "",
//    val correctAnswer: String = "",
//    val iconName: String = "" // Matches Firestore Field
//) {
//    // Helper to convert String "speech_dashboard" -> R.drawable.speech_dashboard
//    fun getIconResId(context: Context): Int {
//        return context.resources.getIdentifier(
//            iconName,
//            "drawable",
//            context.packageName
//        )
//    }
//}


//package com.chirathi.voicebridge.data
//
//import android.content.Context
//import android.os.Parcelable
//import com.google.firebase.firestore.DocumentId
//import kotlinx.parcelize.Parcelize
//
//@Parcelize
//data class LessonModel(
//    @DocumentId
//    var id: String = "",
//
//    val ageGroup: String = "",
//    val subject: String = "",
//    val disorderType: String = "",
//    val lessonTitle: String = "",
//    val lessonContent: String = "",
//    val question: String = "",
//    val correctAnswer: String = "",
//    val iconName: String = ""
//) : Parcelable {
//    fun getIconResId(context: Context): Int {
//        return context.resources.getIdentifier(
//            iconName,
//            "drawable",
//            context.packageName
//        )
//    }
//}

package com.chirathi.voicebridge.data

import android.content.Context
import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class LessonModel(
    @DocumentId
    var id: String = "",
    val ageGroup: String = "",
    val subject: String = "",
    val disorderType: String = "",
    val lessonTitle: String = "",
    val lessonContent: String = "",
    val question: String = "",
    val correctAnswer: String = "",
    val iconName: String = "",
    val subLessons: List<SubLessonModel> = emptyList() // NEW FIELD
) : Parcelable {
    fun getIconResId(context: Context): Int {
        return context.resources.getIdentifier(
            iconName,
            "drawable",
            context.packageName
        )
    }
}

@Parcelize
data class SubLessonModel(
    val lessonTitle: String = "",
    val lessonContent: String = "",
    val question: String = "",
    val correctAnswer: String = "",
    val iconName: String = "" // Add this property
) : Parcelable



