package com.example.spacedodger

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

 //Main Activity: Handles UI, Sensors, Lifecycle, and Game Loop.

class MainActivity : AppCompatActivity(), SensorEventListener, GameCallback {

    // UI Components
    private val ROWS = 7
    private val COLS = 5
    private lateinit var gridLayout: GridLayout
    private lateinit var scoreLabel: TextView
    private lateinit var heartsLabel: TextView
    private val uiMatrix = Array(ROWS) { arrayOfNulls<ImageView>(COLS) }
    private lateinit var menuContainer: View
    private lateinit var gameContainer: View
    private lateinit var buttonsLayout: View
    private lateinit var etName: EditText

    // Logic Managers
    private lateinit var gameManager: GameManager
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var isSensorMode = true

    // Location & Persistence
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private lateinit var prefs: SharedPreferences

    // Game Loop Handler
    private val handler = Handler(Looper.getMainLooper())
    private val gameLoopRunnable = object : Runnable {
        override fun run() {
            // Only update if the game is logically running
            if (gameManager.isRunning) {
                gameManager.updateGameTick()
                handler.postDelayed(this, gameManager.currentDelay)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lock orientation programmatically
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)

        SignalManager.init(this)

        gameManager = GameManager(this, ROWS, COLS)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        prefs = getSharedPreferences("SpaceGamePrefs", Context.MODE_PRIVATE)

        initViews()
        askPermissions()
    }

    // --- Lifecycle Methods ---

    override fun onPause() {
        super.onPause()
        // 1. Stop sensors to save battery
        if (isSensorMode) {
            sensorManager.unregisterListener(this)
        }
        // 2. Stop game loop
        handler.removeCallbacks(gameLoopRunnable)
    }

    override fun onResume() {
        super.onResume()
        // Resume game if it was running before pause
        if (gameManager.isRunning) {
            // 1. Re-register sensors
            if (isSensorMode) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            }
            // 2. Restart game loop
            handler.post(gameLoopRunnable)
        }
    }

    private fun initViews() {
        menuContainer = findViewById(R.id.menu_container)
        gameContainer = findViewById(R.id.game_container)
        gridLayout = findViewById(R.id.game_grid)
        buttonsLayout = findViewById(R.id.buttons_layout)
        scoreLabel = findViewById(R.id.score_lbl)
        heartsLabel = findViewById(R.id.hearts_lbl)
        etName = findViewById(R.id.et_player_name)
        etName.setText(prefs.getString("last_name", ""))

        buildDynamicGrid()

        // Menu Buttons
        findViewById<Button>(R.id.btn_mode_sensors).setOnClickListener { startGame(true, 1000L) }
        findViewById<Button>(R.id.btn_mode_slow).setOnClickListener { startGame(false, 1200L) }
        findViewById<Button>(R.id.btn_mode_fast).setOnClickListener { startGame(false, 600L) }
        findViewById<Button>(R.id.btn_high_scores).setOnClickListener {
            startActivity(Intent(this, ScoreActivity::class.java))
        }

        // Game Controls (Buttons)
        findViewById<Button>(R.id.btn_left).setOnClickListener { gameManager.movePlayer(-1) }
        findViewById<Button>(R.id.btn_right).setOnClickListener { gameManager.movePlayer(1) }
    }

     //Initializes and starts a new game session.

    private fun startGame(sensorMode: Boolean, delay: Long) {
        val name = etName.text.toString()
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter Name!", Toast.LENGTH_SHORT).show()
            return
        }

        // --- Cleanup Phase ---
        // 1. Stop previous loop
        handler.removeCallbacks(gameLoopRunnable)
        // 2. Unregister sensors
        sensorManager.unregisterListener(this)
        // 3. Reset logic state
        gameManager.resetGame()
        // --------------------

        prefs.edit().putString("last_name", name).apply()
        getLocation()

        isSensorMode = sensorMode
        gameManager.baseDelay = delay
        gameManager.currentDelay = delay
        gameManager.isRunning = true

        menuContainer.visibility = View.GONE
        gameContainer.visibility = View.VISIBLE
        buttonsLayout.visibility = if (isSensorMode) View.GONE else View.VISIBLE

        if (isSensorMode) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        handler.post(gameLoopRunnable)
    }

    // --- Sensors Implementation ---
    private var lastMoveTime = 0L

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !gameManager.isRunning || !isSensorMode) return

        val x = event.values[0]
        val y = event.values[1]

        // X-Axis: Movement control
        if (System.currentTimeMillis() - lastMoveTime > 250) {
            if (x > 3.0f) {
                gameManager.movePlayer(-1)
                lastMoveTime = System.currentTimeMillis()
            } else if (x < -3.0f) {
                gameManager.movePlayer(1)
                lastMoveTime = System.currentTimeMillis()
            }
        }

        // Y-Axis: Speed control
        if (y > 9.0f) {
            gameManager.currentDelay = (gameManager.baseDelay * 1.5).toLong() // Slow
        } else if (y < 3.0f) {
            gameManager.currentDelay = (gameManager.baseDelay * 0.5).toLong() // Fast
        } else {
            gameManager.currentDelay = gameManager.baseDelay // Normal
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // --- GameCallback Implementation ---

    override fun updateUI(matrix: Array<IntArray>, score: Int, lives: Int) {
        // Run UI updates on the main thread
        runOnUiThread {
            scoreLabel.text = "Score: $score"
            heartsLabel.text = "❤".repeat(lives)

            for (i in 0 until ROWS) {
                for (j in 0 until COLS) {
                    val img = uiMatrix[i][j]
                    img?.visibility = View.INVISIBLE

                    if (i == ROWS - 1 && j == gameManager.playerPosition) {
                        img?.setImageResource(R.drawable.jet)
                        img?.visibility = View.VISIBLE
                    } else if (matrix[i][j] == 1) {
                        img?.setImageResource(R.drawable.haminai)
                        img?.visibility = View.VISIBLE
                    } else if (matrix[i][j] == 2) {
                        img?.setImageResource(R.drawable.ammo)
                        img?.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun gameOver(finalScore: Int) {
        runOnUiThread {
            saveHighScore(finalScore)
            handler.removeCallbacks(gameLoopRunnable)
            sensorManager.unregisterListener(this)

            gameContainer.visibility = View.GONE
            menuContainer.visibility = View.VISIBLE
            Toast.makeText(this, "Game Over!", Toast.LENGTH_LONG).show()
        }
    }

    // --- Helpers ---

    private fun saveHighScore(score: Int) {
        val name = prefs.getString("last_name", "Unknown") ?: "Unknown"
        val lat = lastLocation?.latitude ?: 0.0
        val lng = lastLocation?.longitude ?: 0.0
        val newEntry = "$name|$score|$lat|$lng"

        val existingSet = prefs.getStringSet("history", mutableSetOf()) ?: mutableSetOf()
        val newSet = existingSet.toMutableSet()
        newSet.add(newEntry)
        prefs.edit().putStringSet("history", newSet).apply()
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc -> lastLocation = loc }
        }
    }

    private fun askPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
        }
    }

    private fun buildDynamicGrid() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val cellWidth = (screenWidth / COLS) - 16
        val cellHeight = cellWidth

        for (i in 0 until ROWS) {
            for (j in 0 until COLS) {
                val img = ImageView(this)
                val params = GridLayout.LayoutParams()
                params.width = cellWidth; params.height = cellHeight
                params.rowSpec = GridLayout.spec(i); params.columnSpec = GridLayout.spec(j)
                params.setMargins(4, 4, 4, 4)
                img.layoutParams = params
                img.visibility = View.INVISIBLE
                gridLayout.addView(img)
                uiMatrix[i][j] = img
            }
        }
    }
}