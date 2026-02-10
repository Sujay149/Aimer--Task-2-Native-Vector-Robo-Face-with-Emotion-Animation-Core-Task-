package com.example.roboface

sealed class EmotionState {
    object Idle : EmotionState()
    object Happy : EmotionState()
    object Angry : EmotionState()
    object Sad : EmotionState()
    object Sleep : EmotionState()
    object Curious : EmotionState()
    object Irritated : EmotionState()
    object Listening : EmotionState()
    object Surprised : EmotionState()
}
