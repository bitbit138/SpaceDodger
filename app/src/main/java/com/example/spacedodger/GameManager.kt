package com.example.spacedodger

import kotlin.random.Random


 //Manages game logic, matrix state, collision detection, and score.

class GameManager(private val callback: GameCallback, private val rows: Int, private val cols: Int) {

    var score = 0
        private set
    var lives = 3
        private set
    var playerPosition = 2 // Middle
        private set

    // Matrix: 0 = Empty, 1 = Enemy, 2 = Ammo
    val gameMatrix = Array(rows) { IntArray(cols) { 0 } }

    var isRunning = false
    var baseDelay = 1000L
    var currentDelay = 1000L

    fun movePlayer(direction: Int) {
        val newPos = playerPosition + direction
        if (newPos in 0 until cols) {
            playerPosition = newPos
            updateGameState() // Refresh UI immediately on move
        }
    }


     //Main game tick: advances objects, spawns items, checks collisions.

    fun updateGameTick() {
        score += 10

        // Speed up logic
        if (score % 100 == 0 && baseDelay > 300) {
            baseDelay -= 50
            callback.showToast("SPEED UP!")
        }

        // Collision Check Logic
        val playerRow = rows - 2 // The row where the player sits
        val itemAbove = gameMatrix[playerRow][playerPosition]

        if (itemAbove == 1) { // Enemy
            handleCollision(true)
            gameMatrix[playerRow][playerPosition] = 0 // Remove item
        } else if (itemAbove == 2) { // Ammo
            handleCollision(false)
            gameMatrix[playerRow][playerPosition] = 0 // Remove item
        }

        // Shift Matrix Down
        for (i in rows - 2 downTo 0) {
            for (j in 0 until cols) {
                gameMatrix[i + 1][j] = gameMatrix[i][j]
            }
        }
        // Clear top row
        for (j in 0 until cols) gameMatrix[0][j] = 0

        // Spawn new items
        val r = Random.nextInt(100)
        val col = Random.nextInt(cols)
        if (r < 50) gameMatrix[0][col] = 1 // Enemy
        else if (r > 85) gameMatrix[0][col] = 2 // Ammo

        updateGameState()
    }

    private fun handleCollision(isEnemy: Boolean) {
        if (isEnemy) {
            lives--
            SignalManager.vibrate()
            SignalManager.playCrash()
            callback.showToast("IMPACT DETECTED!")
            if (lives <= 0) {
                isRunning = false
                callback.gameOver(score)
            }
        } else {
            score += 50
            SignalManager.playCollect()
            callback.showToast("Ammo Refueled (+50)")
        }
    }

    // Sends the data to the UI to be drawn
    fun updateGameState() {
        callback.updateUI(gameMatrix, score, lives)
    }


     //Resets game state to initial values.
    fun resetGame() {
        score = 0
        lives = 3
        playerPosition = 2
        isRunning = false
        baseDelay = 1000L
        currentDelay = 1000L

        // Clear grid
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                gameMatrix[i][j] = 0
            }
        }
        // Update clean state
        updateGameState()
    }
}