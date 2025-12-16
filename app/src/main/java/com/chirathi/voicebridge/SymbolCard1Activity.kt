package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SymbolCard1Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symbol_card1)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            val intent = Intent(this, SymbolCommunicationActivity::class.java)
            startActivity(intent)
        }

        val snackBtn = findViewById<ImageView>(R.id.imgSnacks)
        snackBtn.setOnClickListener {
            val intent = Intent(this, SanackSymbolActivity::class.java)
            startActivity(intent)
        }

        val drinkBtn = findViewById<ImageView>(R.id.imgDrinks)
        drinkBtn.setOnClickListener {
            val intent = Intent(this, DrinkSymbolActivity::class.java)
            startActivity(intent)
        }

        val toyBtn = findViewById<ImageView>(R.id.imgToys)
        toyBtn.setOnClickListener {
            val intent = Intent(this, ToysSymbolActivity::class.java)
            startActivity(intent)
        }

        val clothesBtn = findViewById<ImageView>(R.id.imgClothes)
        clothesBtn.setOnClickListener {
            val intent = Intent(this, ClothesSymbolActivity::class.java)
            startActivity(intent)
        }

        val coloursBtn = findViewById<ImageView>(R.id.imgColours)
        coloursBtn.setOnClickListener {
            val intent = Intent(this, ColoursSymbolsActivity::class.java)
            startActivity(intent)
        }

        val foodsBtn = findViewById<ImageView>(R.id.imgFoods)
        foodsBtn.setOnClickListener {
            val intent = Intent(this, FoodSymbolsActivity::class.java)
            startActivity(intent)
        }

        val sanitationsBtn = findViewById<ImageView>(R.id.imgSanitations)
        sanitationsBtn.setOnClickListener {
            val intent = Intent(this, SanitationSymbolsActivity::class.java)
            startActivity(intent)
        }

        val emotionsBtn = findViewById<ImageView>(R.id.imgEmotions)
        emotionsBtn.setOnClickListener {
            val intent = Intent(this, EmotionSymbolsActivity::class.java)
            startActivity(intent)
        }

        val activityBtn = findViewById<ImageView>(R.id.imgActivities)
        activityBtn.setOnClickListener {
            val intent = Intent(this, ActivitySymbolsActivity::class.java)
            startActivity(intent)
        }

    }
}