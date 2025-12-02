package com.example.spacedodger

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // UI Components
    private lateinit var menuContainer: View
    private lateinit var gameContainer: View
    private lateinit var etPlayerName: EditText
    private lateinit var btnStart: Button
    private lateinit var tvHighScores: TextView
    private lateinit var scoreLabel: TextView
    private lateinit var heartsLabel: TextView
    private val uiMatrix = Array(5) { arrayOfNulls<ImageView>(3) }

    // Vibrator
    private lateinit var vibrator: Vibrator

    // Game Logic
    private var playerPosition = 1
    private var score = 0
    private var lives = 3
    private var isGameRunning = false
    private var currentPlayerName = ""

    // Matrix: 0 = Empty, 1 = Enemy
    private val gameMatrix = Array(5) { IntArray(3) { 0 } }

    private val handler = Handler(Looper.getMainLooper())
    private val gameLoopRunnable = object : Runnable {
        override fun run() {
            if (isGameRunning) {
                gameTick()
                handler.postDelayed(this, 1000) // Speed: 1 sec
            }
        }
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Vibrator
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        prefs = getSharedPreferences("SpaceGamePrefs", Context.MODE_PRIVATE)

        // Initialize Views
        menuContainer = findViewById(R.id.menu_container)
        gameContainer = findViewById(R.id.game_container)
        etPlayerName = findViewById(R.id.et_player_name)
        btnStart = findViewById(R.id.btn_start_game)
        tvHighScores = findViewById(R.id.tv_high_scores)
        scoreLabel = findViewById(R.id.score_lbl)
        heartsLabel = findViewById(R.id.hearts_lbl)

        setupViewsMatrix()
        updateHighScoreBoard()

        val lastUsedName = prefs.getString("last_player_name", "")
        etPlayerName.setText(lastUsedName)

        btnStart.setOnClickListener {
            val nameInput = etPlayerName.text.toString().trim()
            if (nameInput.isEmpty()) {
                Toast.makeText(this, "Enter Pilot Name!", Toast.LENGTH_SHORT).show()
            } else {
                currentPlayerName = nameInput
                prefs.edit().putString("last_player_name", currentPlayerName).apply()
                startGame()
            }
        }

        findViewById<Button>(R.id.btn_left).setOnClickListener {
            if (isGameRunning && playerPosition > 0) {
                playerPosition--
                updateGameUI()
            }
        }
        findViewById<Button>(R.id.btn_right).setOnClickListener {
            if (isGameRunning && playerPosition < 2) {
                playerPosition++
                updateGameUI()
            }
        }
    }

    private fun startGame() {
        menuContainer.visibility = View.GONE
        gameContainer.visibility = View.VISIBLE

        score = 0
        lives = 3
        playerPosition = 1
        isGameRunning = true

        for (row in 0 until 5) {
            for (col in 0 until 3) {
                gameMatrix[row][col] = 0
            }
        }

        updateScoreAndLives()
        updateGameUI()
        handler.post(gameLoopRunnable)
    }

    private fun gameTick() {
        score++
        updateScoreAndLives()

        // Check collision
        if (gameMatrix[3][playerPosition] == 1) {
            hitPlayer()
        }

        // Move logic
        for (row in 3 downTo 0) {
            for (col in 0 until 3) {
                if (gameMatrix[row][col] == 1) {
                    gameMatrix[row + 1][col] = 1
                    gameMatrix[row][col] = 0
                }
            }
        }
        for(col in 0 until 3) gameMatrix[4][col] = 0

        val randomCol = Random.nextInt(0, 3)
        gameMatrix[0][randomCol] = 1

        updateGameUI()
    }

    private fun hitPlayer() {
        lives--

        // --- VIBRATION LOGIC ---
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        }

        Toast.makeText(this, "MAYDAY! IMPACT CONFIRMED!", Toast.LENGTH_SHORT).show()

        if (lives <= 0) {
            gameOver()
        }
    }

    private fun gameOver() {
        isGameRunning = false
        handler.removeCallbacks(gameLoopRunnable)
        saveHighScore(currentPlayerName, score)

        Toast.makeText(this, "MISSION FAILED!", Toast.LENGTH_LONG).show()

        gameContainer.visibility = View.GONE
        menuContainer.visibility = View.VISIBLE

        btnStart.text = "RETRY MISSION"
        updateHighScoreBoard()
    }

    private fun updateScoreAndLives() {
        scoreLabel.text = "Targets: $score"
        val hearts = StringBuilder()
        repeat(lives) { hearts.append("❤") }
        heartsLabel.text = hearts.toString()
    }

    private fun updateGameUI() {
        for (row in 0 until 5) {
            for (col in 0 until 3) {
                val img = uiMatrix[row][col]
                img?.visibility = View.INVISIBLE
                img?.setImageResource(R.drawable.haminai)

                if (gameMatrix[row][col] == 1) {
                    img?.visibility = View.VISIBLE
                }
            }
        }

        val playerImg = uiMatrix[4][playerPosition]
        playerImg?.setImageResource(R.drawable.jet)
        playerImg?.visibility = View.VISIBLE
    }

    private fun setupViewsMatrix() {
        uiMatrix[0][0] = findViewById(R.id.r0c0); uiMatrix[0][1] = findViewById(R.id.r0c1); uiMatrix[0][2] = findViewById(R.id.r0c2)
        uiMatrix[1][0] = findViewById(R.id.r1c0); uiMatrix[1][1] = findViewById(R.id.r1c1); uiMatrix[1][2] = findViewById(R.id.r1c2)
        uiMatrix[2][0] = findViewById(R.id.r2c0); uiMatrix[2][1] = findViewById(R.id.r2c1); uiMatrix[2][2] = findViewById(R.id.r2c2)
        uiMatrix[3][0] = findViewById(R.id.r3c0); uiMatrix[3][1] = findViewById(R.id.r3c1); uiMatrix[3][2] = findViewById(R.id.r3c2)
        uiMatrix[4][0] = findViewById(R.id.r4c0); uiMatrix[4][1] = findViewById(R.id.r4c1); uiMatrix[4][2] = findViewById(R.id.r4c2)
    }

    // High Score Data Class
    data class ScoreEntry(val name: String, val score: Int) : Comparable<ScoreEntry> {
        override fun compareTo(other: ScoreEntry): Int { return other.score - this.score }
    }

    private fun saveHighScore(name: String, newScore: Int) {
        val scores = loadHighScores()
        scores.add(ScoreEntry(name, newScore))
        scores.sort()
        val top5 = if (scores.size > 5) scores.subList(0, 5) else scores
        val editor = prefs.edit()
        editor.putInt("score_count", top5.size)
        for (i in top5.indices) {
            editor.putString("name_$i", top5[i].name)
            editor.putInt("score_$i", top5[i].score)
        }
        editor.apply()
    }

    private fun loadHighScores(): ArrayList<ScoreEntry> {
        val list = ArrayList<ScoreEntry>()
        val count = prefs.getInt("score_count", 0)
        for (i in 0 until count) {
            val name = prefs.getString("name_$i", "-") ?: "-"
            val s = prefs.getInt("score_$i", 0)
            list.add(ScoreEntry(name, s))
        }
        return list
    }

    private fun updateHighScoreBoard() {
        val scores = loadHighScores()
        val sb = StringBuilder()
        for (i in 0 until 5) {
            sb.append("${i + 1}. ")
            if (i < scores.size) {
                sb.append("${scores[i].name} - ${scores[i].score}")
            } else {
                sb.append("-")
            }
            sb.append("\n")
        }
        tvHighScores.text = sb.toString()
    }
}