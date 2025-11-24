package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class QuickWordsChartsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_words_charts)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        setupIconClickListeners()

//        val playIcon = findViewById<ImageView>(R.id.imgPlay)
//        //val stopIcon = findViewById<ImageView>(R.id.imgStop)
//
//        playIcon.setOnClickListener {
//            val intent = Intent(this, PhraseActivity::class.java)
//            startActivity(intent)
//        }
    }

    private fun setupIconClickListeners() {
        // Map of image view IDs to drawable resources and phrases
        val iconData = mapOf(
            R.id.imgYes to Pair(R.drawable.yes, "Yes"),
            R.id.imgOk to Pair(R.drawable.ok, "Ok"),
            R.id.imgNo to Pair(R.drawable.no, "No"),
            R.id.imgThankyou to Pair(R.drawable.thanks, "Thank you"),
            R.id.imgHelp to Pair(R.drawable.help, "I need help"),
            R.id.imgSorry to Pair(R.drawable.sorry, "I'm sorry"),
            R.id.imgWashroom to Pair(R.drawable.washroom, "I need to use washroom"),
            R.id.imgHungry to Pair(R.drawable.hungry, "I'm hungry"),
            R.id.imgBathe to Pair(R.drawable.bathe, "I want to bathe"),
            R.id.imgPlay to Pair(R.drawable.play, "I want to play"),
            R.id.imgTv to Pair(R.drawable.tv, "I want to watch TV"),
            R.id.imgSnack to Pair(R.drawable.snack, "I want snacks"),
            R.id.imgDrink to Pair(R.drawable.drink, "I want a drink"),
            R.id.imgPain to Pair(R.drawable.pain, "I have pain"),
            R.id.imgStop to Pair(R.drawable.stop, "Stop")
        )

        // Set click listeners for all icons
        iconData.forEach { (imageViewId, data) ->
            val imageView = findViewById<ImageView>(imageViewId)
            imageView.setOnClickListener {
                val (drawableRes, phrase) = data
                navigateToPhraseActivity(drawableRes, phrase)
            }
        }
    }

    private fun navigateToPhraseActivity(drawableRes: Int, phrase: String) {
        val intent = Intent(this, PhraseActivity::class.java).apply {
            putExtra("SELECTED_ICON_DRAWABLE", drawableRes)
            putExtra("SELECTED_PHRASE", phrase)
        }
        startActivity(intent)
    }

}
