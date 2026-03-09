package com.example.animationpokemon


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class AnimationPokemonActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PokemonTransitionScreen()
        }
    }
}