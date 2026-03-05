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

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object LessonRepository {

    private val db = FirebaseFirestore.getInstance()
    private const val TAG = "LessonRepository"

    fun getLessons(
        context: Context,
        age: String,
        subject: String,
        disorderType: String? = null,
        onResult: (List<LessonModel>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            Log.d(TAG, "Loading lessons for age=$age, subject=$subject")
            
            // Load lessons from local JSON file instead of Firestore
            val jsonString = context.assets.open("lessons04.json")
                .bufferedReader()
                .use { it.readText() }
            
            Log.d(TAG, "JSON loaded, length: ${jsonString.length}")
            
            val gson = Gson()
            val lessonListType = object : TypeToken<List<LessonModel>>() {}.type
            val allLessons: List<LessonModel> = gson.fromJson(jsonString, lessonListType)
            
            Log.d(TAG, "Total lessons parsed: ${allLessons.size}")
            
            // Filter lessons by age and subject
            var filteredLessons = allLessons.filter { lesson ->
                lesson.age == age && lesson.subject == subject
            }
            
            Log.d(TAG, "Filtered lessons: ${filteredLessons.size} (age=$age, subject=$subject)")
            
            // SMART FALLBACK: If no lessons found, try alternative subjects
            if (filteredLessons.isEmpty()) {
                Log.w(TAG, "No lessons for '$subject', trying fallback subjects...")
                
                // Try fallback subjects in order of preference
                val fallbackSubjects = when (subject.lowercase()) {
                    "speech" -> listOf("Math", "English", "General")
                    "basic math" -> listOf("Math", "General")
                    "math advanced" -> listOf("Math", "Math Advanced", "General")
                    "reading" -> listOf("English", "General")
                    "science" -> listOf("Math", "General")
                    else -> listOf("General", "Math", "English")
                }
                
                for (fallbackSubject in fallbackSubjects) {
                    filteredLessons = allLessons.filter { lesson ->
                        lesson.age == age && lesson.subject == fallbackSubject
                    }
                    if (filteredLessons.isNotEmpty()) {
                        Log.i(TAG, "✓ Found ${filteredLessons.size} lessons using fallback: '$fallbackSubject'")
                        break
                    }
                }
            }
            
            if (filteredLessons.isEmpty()) {
                Log.w(TAG, "Still no lessons! Checking first few lessons...")
                allLessons.take(5).forEach { lesson ->
                    Log.d(TAG, "Sample: age='${lesson.age}' subject='${lesson.subject}' hint='${lesson.lessonHint}'")
                }
            }
            
            onResult(filteredLessons)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading lessons", e)
            onError(e)
        }
    }
}