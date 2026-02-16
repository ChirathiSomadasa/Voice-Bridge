package com.chirathi.voicebridge

object PronunciationAnalyst {

    // 1. WORD ERROR MAPPING
    // Based on Clinician's Assessment Record Sheet
    private val wordErrors = mapOf(
        // --- From PDF Assessment Sheet ---
        "ball" to "Final Consonant Deletion. Check 'L' sound.",
        "cat" to "Fronting Error (t for k). Check 'K' sound.",
        "spoon" to "Cluster Reduction (p for sp). Check 'S' blends.",
        "rabbit" to "Gliding Error (w for r). Check 'R' sound.",
        "chair" to "Deaffrication (sh for ch). Check 'CH' sound.",
        "tap" to "Backing Error (k for t). Check 'T' sound.",
        "leaf" to "Final Consonant Deletion. Check 'F' sound.",
        "shoe" to "Stopping (t for sh). Check 'SH' sound.",
        "bottle" to "Vowelization. Check 'TL' or 'L' sound.",
        "goat" to "Fronting Error (d for g). Check 'G' sound.",
        "sun" to "Stopping (t for s). Check 'S' sound.",
        "star" to "Cluster Reduction or Distortion. Check 'ST' blend.",
        "bed" to "Devoicing (t for d). Check final 'D'.",
        "duck" to "Backing Error (g for d). Check 'D' sound.",
        "lamp" to "Gliding (y for l). Check 'L' sound.",
        "waterfall" to "Epenthesis (added vowels). Check flow.",
        "computer" to "Weak Syllable Deletion. Check all syllables.",
        "zoo" to "Stopping (d for z). Check 'Z' sound.",
        "banana" to "Weak Syllable Deletion (nana). Check rhythm.",

        // --- Common Additional Words in App ---
        "tree" to "Cluster Reduction (tee for tree). Check 'TR' blend.",
        "bird" to "Vowel distortion or R-coloring issue.",
        "flower" to "Cluster Reduction (fl) or Gliding.",
        "apple" to "Vowel precision or 'L' sound at end.",
        "book" to "Final Consonant Deletion. Check 'K' sound.",
        "house" to "Final Consonant Deletion. Check 'S' sound."
    )

    fun analyze(content: String, score: Int, type: String): String {
        val cleanContent = content.trim()

        // CASE 1: LEVEL 1 - LETTERS (Single Character)
        if (cleanContent.length == 1 && cleanContent[0].isLetter()) {
            return when {
                score == 0 -> "No sound detected for letter '$cleanContent'."
                score < 50 -> "Difficulty producing the '$cleanContent' sound."
                else -> "Minor pronunciation error for letter '$cleanContent'."
            }
        }

        // CASE 2: LEVEL 3 - SENTENCES (Multiple words)
        // If it has spaces OR the type is explicitly 'sentence'
        if (cleanContent.contains(" ") || type == "sentence") {
            return when {
                score == 0 -> "No speech detected."
                score < 40 -> "Incomplete: Child likely stopped early or missed many words."
                score < 70 -> "Mispronunciation: Sentence complete but articulation was unclear."
                else -> "Minor fluency errors or speed issues."
            }
        }

        // CASE 3: LEVEL 2 - WORDS
        else {
            val lowerWord = cleanContent.lowercase()

            // 1. Check if the word is in our known error list (Assessment Sheet)
            if (wordErrors.containsKey(lowerWord)) {
                return wordErrors[lowerWord]!!
            }

            // 2. Fallback for any word NOT in the list
            return when {
                score == 0 -> "No speech detected."
                score < 40 -> "Major error: Word unrecognizable."
                score < 70 -> "Articulation error: Needs clarity."
                else -> "Minor pronunciation slip."
            }
        }
    }
}