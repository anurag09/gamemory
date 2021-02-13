package com.mishra.gamemory

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mishra.gamemory.R.color.color_progress_full
import com.mishra.gamemory.R.color.color_progress_none
import com.mishra.gamemory.modles.BoardSize
import com.mishra.gamemory.modles.BoardSize.*
import com.mishra.gamemory.modles.BoardSize.Companion.getByValue
import com.mishra.gamemory.modles.EXTRA_GAME_NAME
import com.mishra.gamemory.modles.MemoryGame
import com.mishra.gamemory.modles.UserListImages
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView
    private lateinit var boardSize: BoardSize
    private lateinit var clRoot: ConstraintLayout

    private lateinit var memoryGame: MemoryGame
    private lateinit var memoryAdapter: MemoryBoardAdapter

    private val db = Firebase.firestore
    private var gameName : String? = null
    private var customGameImages: List<String>? = null

    companion object{
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 1111
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        boardSize = EASY
        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)
        setUpBoard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_refresh -> {
                return refreshBoard()
            }
            R.id.menu_new_size -> {
                showNewSizeDialog()
                return true
            }
            R.id.menu_custom_game ->{
                createNewCustomBoard()
                return true
            }
            R.id.mi_download -> {
                showDownloadDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDownloadDialog() {
        val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_download_board, null)
        showAlertDialog("Fetch Custom game", boardDownloadView, View.OnClickListener {
            val gameToDownload = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
                .text.trim().toString()
            downLoadCustomGame(gameToDownload)

        })
    }

    private fun createNewCustomBoard() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_zise, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create Custom Game", boardSizeView, View.OnClickListener {
            val desiredBoardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            val intent = Intent(this, CustomGameActivity::class.java)
            intent.putExtra("EXTRA_BOARD_SIZE", desiredBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)

        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == Activity.RESULT_OK){
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME) ?: return
            downLoadCustomGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun downLoadCustomGame(customGameName: String) {
        db.collection("games").document(customGameName).get()
            .addOnSuccessListener {document ->
                val userListImages = document.toObject(UserListImages::class.java)
                if(userListImages?.images == null){
                    Log.i(TAG, "Invalid Custom game fetch error")
                    Snackbar.make(clRoot, "Sorry, not able to fetch $customGameName", Snackbar.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val numCards = userListImages.images.size * 2
                boardSize = getByValue(numCards)
                gameName = customGameName
                customGameImages = userListImages.images
                cacheIt(userListImages.images)
                Snackbar.make(clRoot, "You are playing custom game: $gameName", Snackbar.LENGTH_LONG).show()
                setUpBoard()
            }
            .addOnFailureListener {
                //TODO failure
            }
    }

    private fun cacheIt(images: List<String>) {
        images.forEach {
            Picasso.get().load(it).fetch()
        }
    }

    private fun refreshBoard(): Boolean {
        if (memoryGame.totalFlips() > 0 && !memoryGame.haveWon()) {
            showAlertDialog("Quit your current game", null, View.OnClickListener {
                setUpBoard()
            })
        } else {
            setUpBoard()
        }
        return true
    }

    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_zise, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when(boardSize){
            EASY -> radioGroupSize.check(R.id.rbEasy)
            MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            else -> radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("Choose new size", boardSizeView, View.OnClickListener {
            boardSize = when(radioGroupSize.checkedRadioButtonId){
                    R.id.rbEasy -> EASY
                    R.id.rbMedium -> MEDIUM
                    else -> BoardSize.HARD
                }
            gameName = null
            customGameImages = null
            setUpBoard()
        })
    }

    private fun showAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK"){_, _ ->
                positiveClickListener.onClick(null)
            }.show()


    }

    private fun setUpBoard() {
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        tvNumMoves.text = "Moves: 0"
        tvNumPairs.setTextColor(ContextCompat.getColor(this, color_progress_none))
        memoryGame = MemoryGame(boardSize, customGameImages)

        memoryAdapter =
            MemoryBoardAdapter(this, boardSize, memoryGame.cards, object : MemoryBoardAdapter.CardClickListener {
                override fun onCardClocked(position: Int) {
                    updateGameWithFlip(position)
                }
            })
        rvBoard.adapter = memoryAdapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.widht())
    }

    private fun updateGameWithFlip(position: Int) {
        if(memoryGame.haveWon()) {
            Snackbar.make(clRoot, "You have already won", Snackbar.LENGTH_LONG).show()
            CommonConfetti.rainingConfetti(clRoot, intArrayOf(YELLOW, MAGENTA, GREEN)).oneShot()
            return
        }
        if(memoryGame.isCardFaceUp(position)) {
            Snackbar.make(clRoot, "try another card", Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGame.flipCard(position)){
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat()/memoryGame.boardSize.pairs(),
                ContextCompat.getColor(this, color_progress_none),
                ContextCompat.getColor(this, color_progress_full)
            ) as Int
            tvNumPairs.setTextColor(color)
            tvNumPairs.text = "Pairs: ${memoryGame.numPairsFound} / ${memoryGame.boardSize.pairs()}"
            if(memoryGame.haveWon()) {
                CommonConfetti.rainingConfetti(clRoot, intArrayOf(YELLOW, MAGENTA, GREEN)).oneShot()
                Snackbar.make(clRoot, "Congratulations! You Won", Snackbar.LENGTH_LONG).show()
            }
        }
        tvNumMoves.text = "Moves: ${memoryGame.totalFlips()}"
        memoryAdapter.notifyDataSetChanged()
    }
}

val DEFAULT_LISTS = listOf(
    R.drawable.ic_face,
    R.drawable.ic_airline_seat,
    R.drawable.ic_bulb,
    R.drawable.ic_face_hair,
    R.drawable.ic_iridescent,
    R.drawable.ic_wc,
    R.drawable.ic_subway,
    R.drawable.ic_time_to_leave,
    R.drawable.ic_toys,
    R.drawable.ic_traffic,
    R.drawable.ic_watch,
    R.drawable.ic_spa
)
