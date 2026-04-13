package pl.wsei.pam.lab03

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pl.wsei.pam.lab01.R

class Lab03Activity : AppCompatActivity() {

    lateinit var mBoard: GridLayout
    lateinit var mBoardModel: MemoryBoardView

    lateinit var completionPlayer: MediaPlayer
    lateinit var negativePlayer: MediaPlayer

    // NOWA ZMIENNA - domyślnie dźwięk jest włączony
    var isSound = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lab03)

        supportActionBar?.title = "Gra Memory"

        mBoard = findViewById(R.id.game_board_grid)

        ViewCompat.setOnApplyWindowInsetsListener(mBoard) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val size = intent.getIntArrayExtra("size") ?: intArrayOf(3, 3)
        val rows = size[0]
        val columns = size[1]

        mBoard.columnCount = columns
        mBoard.rowCount = rows

        mBoardModel = MemoryBoardView(mBoard, columns, rows)

        if (savedInstanceState != null) {
            val savedState = savedInstanceState.getIntArray("state")
            if (savedState != null) {
                mBoardModel.setState(savedState)
            }
        }

        mBoardModel.setOnGameChangeListener { e ->
            when (e.state) {
                GameStates.Matching -> {
                    e.tiles.forEach { it.revealed = true }
                }
                GameStates.Match -> {
                    setBoardEnabled(false)

                    // Odtwarzamy dźwięk TYLKO jeśli isSound == true
                    if (isSound) {
                        completionPlayer.start()
                    }

                    e.tiles.forEach { tile ->
                        tile.revealed = true
                        animatePairedButton(tile.button, Runnable {
                            setBoardEnabled(true)
                        })
                    }
                }
                GameStates.NoMatch -> {
                    setBoardEnabled(false)

                    // Odtwarzamy dźwięk TYLKO jeśli isSound == true
                    if (isSound) {
                        negativePlayer.start()
                    }

                    e.tiles.forEach { tile ->
                        tile.revealed = true
                        animateUnpairedButton(tile.button, Runnable {
                            tile.revealed = false
                            setBoardEnabled(true)
                        })
                    }
                }
                GameStates.Finished -> {
                    setBoardEnabled(false)

                    if (isSound) {
                        completionPlayer.start()
                    }

                    e.tiles.forEach { tile ->
                        tile.revealed = true
                        animatePairedButton(tile.button, Runnable {
                            Toast.makeText(this, "Game finished!", Toast.LENGTH_SHORT).show()
                        })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        completionPlayer = MediaPlayer.create(applicationContext, R.raw.completion)
        negativePlayer = MediaPlayer.create(applicationContext, R.raw.negative_guitar)
    }

    override fun onPause() {
        super.onPause()
        completionPlayer.release()
        negativePlayer.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val currentState = mBoardModel.getState()
        outState.putIntArray("state", currentState)
    }

    // NOWY KOD - WYŚWIETLANIE MENU W PASKU ZADAŃ:
    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.board_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // NOWY KOD - OBSŁUGA KLIKNIĘCIA W IKONĘ GŁOŚNIKA:
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            R.id.board_activity_sound -> {
                if (isSound) {
                    // Wyłączamy dźwięk
                    Toast.makeText(this, "Sound turned off", Toast.LENGTH_SHORT).show()
                    item.setIcon(R.drawable.baseline_volume_down_24)
                    isSound = false
                } else {
                    // Włączamy dźwięk
                    Toast.makeText(this, "Sound turned on", Toast.LENGTH_SHORT).show()
                    item.setIcon(R.drawable.baseline_volume_up_24)
                    isSound = true
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun animatePairedButton(button: android.widget.ImageButton, action: Runnable) {
        val set = android.animation.AnimatorSet()
        val random = java.util.Random()
        button.pivotX = random.nextFloat() * 200f
        button.pivotY = random.nextFloat() * 200f

        val rotation = android.animation.ObjectAnimator.ofFloat(button, "rotation", 1080f)
        val scallingX = android.animation.ObjectAnimator.ofFloat(button, "scaleX", 1f, 4f)
        val scallingY = android.animation.ObjectAnimator.ofFloat(button, "scaleY", 1f, 4f)
        val fade = android.animation.ObjectAnimator.ofFloat(button, "alpha", 1f, 0f)

        set.startDelay = 500
        set.duration = 2000
        set.interpolator = android.view.animation.DecelerateInterpolator()
        set.playTogether(rotation, scallingX, scallingY, fade)

        set.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animator: android.animation.Animator) {}
            override fun onAnimationEnd(animator: android.animation.Animator) {
                button.scaleX = 1f
                button.scaleY = 1f
                button.alpha = 0.0f
                action.run()
            }
            override fun onAnimationCancel(animator: android.animation.Animator) {}
            override fun onAnimationRepeat(animator: android.animation.Animator) {}
        })
        set.start()
    }

    private fun animateUnpairedButton(button: android.widget.ImageButton, action: Runnable) {
        val set = android.animation.AnimatorSet()
        val rotation = android.animation.ObjectAnimator.ofFloat(button, "rotation", 0f, 15f, -15f, 15f, -15f, 0f)

        set.startDelay = 500
        set.duration = 800
        set.playTogether(rotation)

        set.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animator: android.animation.Animator) {}
            override fun onAnimationEnd(animator: android.animation.Animator) {
                button.rotation = 0f
                action.run()
            }
            override fun onAnimationCancel(animator: android.animation.Animator) {}
            override fun onAnimationRepeat(animator: android.animation.Animator) {}
        })
        set.start()
    }

    private fun setBoardEnabled(isEnabled: Boolean) {
        for (i in 0 until mBoard.childCount) {
            mBoard.getChildAt(i).isEnabled = isEnabled
        }
    }
}