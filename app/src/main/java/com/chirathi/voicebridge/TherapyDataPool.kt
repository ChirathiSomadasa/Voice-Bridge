package com.chirathi.voicebridge

object TherapyDataPool {

    data class WordItem(val text: String, val imageResId: Int)

    // StoryItem groups 3 sentences and 3 images together to form a coherent mini-story
    data class StoryItem(val sentences: List<String>, val imageResIds: List<Int>)

    // LEVEL 2: ARTICULATION WORDS (Targeting specific phonemes)
    val articulationWords = listOf(
        WordItem("ball", R.drawable.ball), WordItem("cat", R.drawable.cat),
        WordItem("spoon", R.drawable.spoon), WordItem("rabbit", R.drawable.rabbit),
        WordItem("chair", R.drawable.chair),WordItem("goat", R.drawable.goat),
        WordItem("leaf", R.drawable.leaf), WordItem("shoe", R.drawable.shoe),
        WordItem("bottle", R.drawable.bottle),  WordItem("carrot", R.drawable.carrot),
        WordItem("sun", R.drawable.sun2), WordItem("star", R.drawable.star),
        WordItem("bed", R.drawable.bed), WordItem("duck", R.drawable.duck),
        WordItem("lamp", R.drawable.lamp), WordItem("waterfall", R.drawable.waterfall),
        WordItem("tree", R.drawable.tree), WordItem("bird", R.drawable.bird),
        WordItem("flower", R.drawable.flower), WordItem("apple", R.drawable.apple),
        WordItem("book", R.drawable.book), WordItem("computer", R.drawable.computer),
        WordItem("zoo", R.drawable.zoo), WordItem("banana", R.drawable.banana),
        WordItem("house", R.drawable.house),WordItem("milk", R.drawable.milk2),
        WordItem("train", R.drawable.train),WordItem("hair", R.drawable.hair),
        WordItem("jump", R.drawable.jump),WordItem("flag", R.drawable.flag),
        WordItem("cup", R.drawable.cup),WordItem("umbrella", R.drawable.umbrella2),
        WordItem("egg", R.drawable.egg),WordItem("orange", R.drawable.orange),
        WordItem("dog", R.drawable.dog),WordItem("helicopter", R.drawable.helicopter),
        WordItem("bus", R.drawable.bus),WordItem("fish", R.drawable.fish),
        WordItem("kite", R.drawable.kite2),WordItem("bag", R.drawable.bag),
    )

    // LEVEL 3: ARTICULATION SENTENCES (Mini-Stories)
    val articulationStories = listOf(
        StoryItem(
            sentences = listOf("The big dog ran fast in the park", "The dog jumped into a brown mud puddle", "The dog had a bath with white bubbles"),
            imageResIds = listOf(R.drawable.dog1_level3, R.drawable.dog2_level3, R.drawable.dog3_level3)
        ),
        StoryItem(
            sentences = listOf("The pretty bird flew up to the sky", "The bird looked down at the trees", "The bird sang a happy song for us"),
            imageResIds = listOf(R.drawable.bird1_level3, R.drawable.bird2_level3, R.drawable.bird3_level3)
        ),
        StoryItem(
            sentences = listOf("Ben builds big blue blocks", "Ben makes a very tall tower", "Oh no the tower fell down"),
            imageResIds = listOf(R.drawable.blue_box1_level3, R.drawable.blue_box2_level3, R.drawable.blue_box3_level3)
        ),
        StoryItem(
            sentences = listOf("Sam saw a sunfish swimming", "The fish jumped high in the air", "The fish swam away quickly"),
            imageResIds = listOf(R.drawable.fish1_level3, R.drawable.fish2_level3, R.drawable.fish3_level3)
        ),
        StoryItem(
            sentences = listOf("Kate keeps her kite in the kit", "Kate takes the kite out on windy days", "The kite flies high in the air"),
            imageResIds = listOf(R.drawable.kite1_level3, R.drawable.kite2_level3, R.drawable.kite3_level3)
        ),
        StoryItem(
            sentences = listOf("The little lion likes to play", "The lion runs in the green grass", "The lion roars at the little bugs"),
            imageResIds = listOf(R.drawable.lion1_level3, R.drawable.lion2_level3, R.drawable.lion3_level3)
        ),
        StoryItem(
            sentences = listOf("The rabbit runs around the rock", "The rabbit is hiding from the fox", "The rabbit finds a safe hole to sleep"),
            imageResIds = listOf(R.drawable.rabbit1_level3, R.drawable.rabbit2_level3, R.drawable.rabbit3_level3)
        ),
        StoryItem(
            sentences = listOf("The sun is bright and warm today", "Sam can go to the beach", "Sam will swim in the cool water"),
            imageResIds = listOf(R.drawable.sun1_level3, R.drawable.sun2_level3, R.drawable.sun3_level3)
        ),
        StoryItem(
            sentences = listOf("The girl gets the green grapes", "The grapes are sweet and yummy", "The girl shares them with her friends"),
            imageResIds = listOf(R.drawable.grapes1_level3, R.drawable.grapes2_level3, R.drawable.grapes3_level3)
        ),
        StoryItem(
            sentences = listOf("John rides a race car really fast", "He overtakes the leading car", "John wins the big race today"),
            imageResIds = listOf(R.drawable.car1_level3, R.drawable.car2_level3, R.drawable.car3_level3)
        )
    )
}