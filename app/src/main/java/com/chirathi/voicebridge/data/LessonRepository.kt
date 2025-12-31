//package com.chirathi.voicebridge.data
//
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.toObjects
//
//object LessonRepository {
//
//    private val db = FirebaseFirestore.getInstance()
//
//    // We use a 'callback' because fetching from the internet is not instant.
//    fun getLessons(
//        age: String,
//        subject: String,
//        onResult: (List<LessonModel>) -> Unit,
//        onError: (Exception) -> Unit
//    ) {
//
//        // 1. Reference the collection
//        db.collection("lessons")
//            // 2. Filter: Only get lessons where 'ageGroup' matches the button clicked
//            .whereEqualTo("ageGroup", age)
//            // 3. Filter: Only get lessons where 'subject' matches
//            .whereEqualTo("subject", subject)
//            .get()
//            .addOnSuccessListener { documents ->
//                // 4. Convert the Firestore JSON documents into your Kotlin LessonModel objects
//                val lessonList = documents.toObjects<LessonModel>()
//                onResult(lessonList)
//            }
//            .addOnFailureListener { exception ->
//                // 5. Handle errors (like no internet)
//                onError(exception)
//            }
//    }
//}

package com.chirathi.voicebridge.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects

object LessonRepository {

    private val db = FirebaseFirestore.getInstance()

    fun getLessons(
        age: String,
        subject: String,
        onResult: (List<LessonModel>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("lessons03")
            .whereEqualTo("ageGroup", age)
            .whereEqualTo("subject", subject)
            .get()
            .addOnSuccessListener { documents ->
                val lessonList = documents.toObjects<LessonModel>()
                onResult(lessonList)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
}