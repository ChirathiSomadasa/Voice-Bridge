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
            val intent = Intent(this, SymbolCommunicationActivity::class.java)
            startActivity(intent)
        }

        val painBtn = findViewById<ImageView>(R.id.imgPain)
        painBtn.setOnClickListener {
            val intent = Intent(this, PainSymbolsActivity::class.java)
            startActivity(intent)
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
            R.id.imgHello to Pair(R.drawable.hello, "Hello"),
            R.id.imgGoodBye to Pair(R.drawable.goodbye, "Good Bye"),
            R.id.imgWashroom to Pair(R.drawable.washroom, "I need to use washroom"),
            R.id.imgHungry to Pair(R.drawable.hungry, "I'm hungry"),
            R.id.imgThirsty to Pair(R.drawable.thirsty, "I am thirsty"),
            R.id.imgPlay to Pair(R.drawable.play, "I want to play"),
            R.id.imgPlease to Pair(R.drawable.please, "Please"),
            R.id.imgBad to Pair(R.drawable.bad, "Bad"),
            R.id.imgGood to Pair(R.drawable.good, "Good"),
            //R.id.imgPain to Pair(R.drawable.pain, "I have pain"),
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
