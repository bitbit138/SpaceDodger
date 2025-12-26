package com.example.spacedodger

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.*
import android.util.DisplayMetrics
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlin.random.Random

class MainActivity : AppCompatActivity(), SensorEventListener {

    private val ROWS = 7
    private val COLS = 5

    // UI
    private lateinit var menuContainer: View
    private lateinit var gameContainer: View
    private lateinit var gridLayout: GridLayout
    private lateinit var laneMarkersLayout: LinearLayout
    private lateinit var buttonsLayout: View
    private lateinit var scoreLabel: TextView
    private lateinit var heartsLabel: TextView
    private val uiMatrix = Array(ROWS) { arrayOfNulls<ImageView>(COLS) }

    // Sensors & Sound & Location
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var soundPool: SoundPool
    private var soundBoom = 0
    private var soundCollect = 0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null

    // Game Logic
    private var gameMatrix = Array(ROWS) { IntArray(COLS) { 0 } }
    private var playerPosition = 2
    private var score = 0
    private var lives = 3
    private var isGameRunning = false

    // --- MODE FLAGS ---
    private var isSensorMode = true
    private var baseDelay = 1000L
    private var currentDelay = 1000L

    private val handler = Handler(Looper.getMainLooper())
    private val gameLoopRunnable = object : Runnable {
        override fun run() {
            if (isGameRunning) {
                gameTick()
                handler.postDelayed(this, currentDelay)
            }
        }
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init Services
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        prefs = getSharedPreferences("SpaceGamePrefs", Context.MODE_PRIVATE)

        // Init Sound
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        soundPool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(audioAttributes).build()
        soundBoom = soundPool.load(this, R.raw.boom, 1)
        soundCollect = soundPool.load(this, R.raw.collect, 1)

        // Bind Views
        menuContainer = findViewById(R.id.menu_container)
        gameContainer = findViewById(R.id.game_container)
        gridLayout = findViewById(R.id.game_grid)
        laneMarkersLayout = findViewById(R.id.lane_markers)
        buttonsLayout = findViewById(R.id.buttons_layout)
        scoreLabel = findViewById(R.id.score_lbl)
        heartsLabel = findViewById(R.id.hearts_lbl)

        buildDynamicGrid()
        askPermissions()

        val etName = findViewById<EditText>(R.id.et_player_name)
        etName.setText(prefs.getString("last_name", ""))

        // --- BUTTON LISTENERS ---

        // 1. SENSORS
        findViewById<Button>(R.id.btn_mode_sensors).setOnClickListener {
            startGame(name = etName.text.toString(), sensorMode = true, initialDelay = 1000L)
        }

        // 2. BUTTONS SLOW
        findViewById<Button>(R.id.btn_mode_slow).setOnClickListener {
            startGame(name = etName.text.toString(), sensorMode = false, initialDelay = 1200L)
        }

        // 3. BUTTONS FAST
        findViewById<Button>(R.id.btn_mode_fast).setOnClickListener {
            startGame(name = etName.text.toString(), sensorMode = false, initialDelay = 600L)
        }

        // 4. HIGH SCORES
        findViewById<Button>(R.id.btn_high_scores).setOnClickListener {
            val intent = Intent(this, ScoreActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_left).setOnClickListener { movePlayer(-1) }
        findViewById<Button>(R.id.btn_right).setOnClickListener { movePlayer(1) }
    }

    private fun startGame(name: String, sensorMode: Boolean, initialDelay: Long) {
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter Name!", Toast.LENGTH_SHORT).show()
            return
        }
        prefs.edit().putString("last_name", name).apply()
        getLocation()

        isSensorMode = sensorMode
        baseDelay = initialDelay
        currentDelay = baseDelay

        // Update UI based on mode
        buttonsLayout.visibility = if (isSensorMode) View.GONE else View.VISIBLE

        menuContainer.visibility = View.GONE
        gameContainer.visibility = View.VISIBLE
        score = 0
        lives = 3
        playerPosition = 2
        isGameRunning = true

        // Clear board
        for (i in 0 until ROWS) for (j in 0 until COLS) gameMatrix[i][j] = 0

        if (isSensorMode) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        handler.post(gameLoopRunnable)
    }

    private fun gameTick() {
        score += 10

        // Speed up logic every 100 points
        if (score % 100 == 0) {
            if (baseDelay > 300) baseDelay -= 50
            Toast.makeText(this, "SPEED UP!", Toast.LENGTH_SHORT).show()
        }

        // In button mode, keep the fixed speed
        if (!isSensorMode) {
            currentDelay = baseDelay
        }

        // --- GHOSTING FIX START ---
        // Check collision and IMMEDIATELY clear the cell
        if (gameMatrix[ROWS-2][playerPosition] == 1) {
            handleCollision(1)
            gameMatrix[ROWS-2][playerPosition] = 0 // Clear enemy instantly
        } else if (gameMatrix[ROWS-2][playerPosition] == 2) {
            handleCollision(2)
            gameMatrix[ROWS-2][playerPosition] = 0 // Clear ammo instantly
        }
        // --- GHOSTING FIX END ---

        // Shift rows down
        for (i in ROWS-2 downTo 0) {
            for (j in 0 until COLS) {
                gameMatrix[i+1][j] = gameMatrix[i][j]
            }
        }
        // Clear top row
        for (j in 0 until COLS) gameMatrix[0][j] = 0

        // Spawn new items
        val r = Random.nextInt(100)
        val col = Random.nextInt(COLS)
        if (r < 50) gameMatrix[0][col] = 1 // Spawn Enemy
        else if (r > 85) gameMatrix[0][col] = 2 // Spawn Ammo

        updateUI()

        // Update labels
        scoreLabel.text = "Score: $score"
        heartsLabel.text = "❤".repeat(lives)
    }

    // --- SENSORS ---
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isGameRunning || !isSensorMode) return
        val x = event.values[0]
        val y = event.values[1]

        if (x > 3.0f) movePlayer(-1)
        else if (x < -3.0f) movePlayer(1)

        // Only update speed if in sensor mode
        if (y > 9.0f) currentDelay = (baseDelay * 1.5).toLong()
        else if (y < 3.0f) currentDelay = (baseDelay * 0.5).toLong()
        else currentDelay = baseDelay
    }

    private fun gameOver() {
        isGameRunning = false
        handler.removeCallbacks(gameLoopRunnable)
        sensorManager.unregisterListener(this)
        saveHighScore()
        gameContainer.visibility = View.GONE
        menuContainer.visibility = View.VISIBLE
    }

    // --- Helper Methods ---
    private fun saveHighScore() {
        val name = prefs.getString("last_name", "Unknown") ?: "Unknown"
        val lat = lastLocation?.latitude ?: 0.0
        val lng = lastLocation?.longitude ?: 0.0

        val newEntry = "$name|$score|$lat|$lng"

        val existingSet = prefs.getStringSet("history",  mutableSetOf()) ?: mutableSetOf()
        val newSet = existingSet.toMutableSet()

        newSet.add(newEntry)

        prefs.edit().putStringSet("history", newSet).apply()
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
        for (j in 0 until COLS) {
            val dot = View(this)
            val params = LinearLayout.LayoutParams(20, 20)
            val margin = (cellWidth - 20) / 2
            params.setMargins(margin + 4, 0, margin + 4, 0)
            dot.layoutParams = params
            dot.background = ContextCompat.getDrawable(this, android.R.drawable.btn_radio)
            laneMarkersLayout.addView(dot)
        }
    }

    private fun movePlayer(direction: Int) {
        val now = System.currentTimeMillis()
        if (isSensorMode && now - lastMoveTime < 250) return
        val newPos = playerPosition + direction
        if (newPos in 0 until COLS) {
            playerPosition = newPos; lastMoveTime = now; updateUI()
        }
    }
    private var lastMoveTime = 0L

    private fun updateUI() {
        for (i in 0 until ROWS) {
            for (j in 0 until COLS) {
                val img = uiMatrix[i][j]
                img?.visibility = View.INVISIBLE
                if (i == ROWS-1 && j == playerPosition) {
                    img?.setImageResource(R.drawable.jet); img?.visibility = View.VISIBLE
                } else if (gameMatrix[i][j] == 1) {
                    img?.setImageResource(R.drawable.haminai); img?.visibility = View.VISIBLE
                } else if (gameMatrix[i][j] == 2) {
                    img?.setImageResource(R.drawable.ammo); img?.visibility = View.VISIBLE
                }
            }
        }
    }
    private fun updateLabels() {
        scoreLabel.text = "Score: $score"
        heartsLabel.text = "❤".repeat(lives)

        // Optional: Update speed info if you have the TextView for it
        // val nextMilestone = ((score / 100) + 1) * 100
        // nextSpeedLabel.text = "Speed up at: $nextMilestone"
    }
    private fun handleCollision(type: Int) {
        if (type == 1) { // Enemy
            lives--
            soundPool.play(soundBoom, 1f, 1f, 0, 0, 1f)
            vibrate() // Tactile feedback
            Toast.makeText(this, "IMPACT DETECTED!", Toast.LENGTH_SHORT).show()

            if (lives <= 0) gameOver()
        } else { // Ammo
            score += 50
            soundPool.play(soundCollect, 1f, 1f, 0, 0, 1f)
            Toast.makeText(this, "Ammo Refueled (+50)", Toast.LENGTH_SHORT).show()
        }
        // Force UI update immediately
        updateLabels()
    }

    private fun vibrate() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(300)
        }
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
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}