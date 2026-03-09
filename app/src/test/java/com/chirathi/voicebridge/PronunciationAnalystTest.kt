package com.chirathi.voicebridge

import org.junit.Assert.assertEquals
import org.junit.Test

class PronunciationAnalystTest {

    @Test
    fun `test exact clinical word error mapping`() {
        // Verify that the word 'carrot' correctly maps to the Velar Fronting Error
        val carrotResult = PronunciationAnalyst.analyze("carrot", 40, "word")
        assertEquals("Fronting Error (t for k). Check 'K' sound.", carrotResult)

        // Verify that the word 'rabbit' correctly maps to the Gliding Error
        val rabbitResult = PronunciationAnalyst.analyze("rabbit", 40, "word")
        assertEquals("Gliding Error (w for r). Check 'R' sound.", rabbitResult)
    }

    @Test
    fun `test fallback logic for unknown words`() {
        // Test the fallback message for a word not present maps scoring < 40%
        val unknownResult = PronunciationAnalyst.analyze("elephant", 30, "word")
        assertEquals("Major error: Word unrecognizable.", unknownResult)
    }

    @Test
    fun `test sentence fluency logic`() {
        // Test the clinical evaluation for a full sentence scoring < 70%
        val sentenceResult = PronunciationAnalyst.analyze("the quick brown fox", 60, "sentence")
        assertEquals("Mispronunciation: Sentence complete but articulation was unclear.", sentenceResult)
    }

    @Test
    fun `test zero score for letters`() {
        // Test the clinical evaluation for a single letter with a 0% score (no speech detected)
        val letterResult = PronunciationAnalyst.analyze("A", 0, "letter")
        assertEquals("No sound detected for letter 'A'.", letterResult)
    }
}