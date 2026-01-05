package com.example.spacedodger

interface GameCallback {
    fun updateUI(matrix: Array<IntArray>, score: Int, lives: Int)
    fun showToast(message: String)
    fun gameOver(score: Int)
}