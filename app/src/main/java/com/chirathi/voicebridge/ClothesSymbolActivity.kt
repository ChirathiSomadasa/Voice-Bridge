package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ClothesSymbolActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clothes_symbol)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        setupIconClickListeners()
    }

    private fun setupIconClickListeners() {
        val itemList = listOf(
            SymbolItem(R.id.imgShirt, R.drawable.shirt, "wear", "shirt"),
            SymbolItem(R.id.imgTShirt, R.drawable.tshirt, "wear", "t-shirt"),
            SymbolItem(R.id.imgTrouser, R.drawable.jeans, "wear", "trouser"),
            SymbolItem(R.id.imgFrock, R.drawable.frock, "wear", "frock"),
            SymbolItem(R.id.imgShort, R.drawable.shorts, "wear", "shorts"),
            SymbolItem(R.id.imgRainCoat, R.drawable.raincoat, "wear", "raincoat"),
            SymbolItem(R.id.imgSkirt, R.drawable.skirt, "wear", "skirt"),
            SymbolItem(R.id.imgBlouse, R.drawable.blouse, "wear", "blouse"),
            SymbolItem(R.id.imgNightSuit, R.drawable.nightsuit, "wear", "night suit"),
            SymbolItem(R.id.imgVest, R.drawable.vest, "wear", "vest"),
            SymbolItem(R.id.imgSlippers, R.drawable.slippers, "wear", "slippers"),
            SymbolItem(R.id.imgSandals, R.drawable.sandals, "wear", "sandals"),
            SymbolItem(R.id.imgWatch, R.drawable.watch, "wear", "watch"),
            SymbolItem(R.id.imgScarf, R.drawable.scarf, "wear", "scarf"),
            SymbolItem(R.id.imgHankerchief, R.drawable.hankerchief, "want", "handkerchief"),
            SymbolItem(R.id.imgWallet, R.drawable.wallet, "want", "wallet"),
            SymbolItem(R.id.imgGlasses, R.drawable.glasses, "wear", "glasses"),
            SymbolItem(R.id.imgSunGlasses, R.drawable.sunglasses, "wear", "sunglasses"),
            SymbolItem(R.id.imgSocks, R.drawable.socks, "wear", "socks"),
            SymbolItem(R.id.imgUmbrella, R.drawable.umbrella, "want", "umbrella"),
            SymbolItem(R.id.imgTie, R.drawable.tie, "wear", "tie"),
            SymbolItem(R.id.imgBowTie, R.drawable.bow_tie, "wear", "bow tie"),
            SymbolItem(R.id.imgCap, R.drawable.cap, "wear", "cap"),
            SymbolItem(R.id.imgBelt, R.drawable.belt, "want", "belt")
        )

        findViewById<ImageView>(R.id.back).setOnClickListener { finish() }

        // 3. Dynamic Click Listener Setup
        itemList.forEach { item ->
            findViewById<ImageView>(item.id)?.setOnClickListener {
                // We send verb and object separately to help Gemini/Logic
                navigateToPhraseActivity(item.imageRes, "${item.verb} ${item.obj}")
            }
        }

    }

    private fun navigateToPhraseActivity(drawableRes: Int, phrase: String) {
        val intent = Intent(this, PhraseActivity::class.java).apply {
            putExtra("SELECTED_ICON_DRAWABLE", drawableRes)
            putExtra("SELECTED_PHRASE", phrase)
            putExtra("IS_SYMBOL_MODE", true)
        }
        startActivity(intent)
    }
}