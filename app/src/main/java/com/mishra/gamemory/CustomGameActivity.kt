package com.mishra.gamemory

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.mishra.gamemory.modles.BoardSize
import com.mishra.gamemory.modles.EXTRA_GAME_NAME1
import com.mishra.gamemory.modles.GAMES
import com.mishra.gamemory.utils.BitMapScaler
import com.mishra.gamemory.utils.isPermissionGranted
import com.mishra.gamemory.utils.requestPermission
import java.io.ByteArrayOutputStream

class CustomGameActivity : AppCompatActivity() {

    private lateinit var boardSize: BoardSize;
    private lateinit var rvImagePicker: RecyclerView;
    private lateinit var saveButton: Button
    private lateinit var etGameName: TextView
    private lateinit var pbUploading: ProgressBar
    private lateinit var adapter: ImagePickerAdapter
    private val uploadedImagesUrls = mutableListOf<String>()

    companion object {
        const val READ_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        const val REQUEST_CODE = 258
    }

    private val chosenImageUris = mutableListOf<Uri>()
    private val storage = Firebase.storage
    private val db = Firebase.firestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_game)

        rvImagePicker = findViewById(R.id.rvImagePicker)
        saveButton = findViewById(R.id.customSaveBtn)
        etGameName = findViewById(R.id.etDownloadGame)
        pbUploading = findViewById(R.id.pbUploading)
        etGameName.filters = arrayOf(InputFilter.LengthFilter(14))

        etGameName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                saveButton.isEnabled = shouldEnableButton()
            }
        })

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra("EXTRA_BOARD_SIZE") as BoardSize
        supportActionBar?.title = "Choose pics (0/${boardSize.pairs()}))"

        adapter = ImagePickerAdapter(this, chosenImageUris, boardSize, object : ImagePickerAdapter.ImageClickListener {
            override fun onPlaceHolderCLicked() {
                if (isPermissionGranted(context = this@CustomGameActivity, permission = READ_PERMISSION)) {
                    launchIntentForPhotos()
                } else {
                    requestPermission(this@CustomGameActivity, READ_PERMISSION, REQUEST_CODE)
                }
            }
        })
        rvImagePicker.adapter = adapter

        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.widht())

        saveButton.setOnClickListener {
            val gameName = etGameName.text.trim().toString()
            saveButton.isEnabled = false
            db.collection(GAMES)
                .document(gameName)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.data != null) {
                        AlertDialog.Builder(this)
                            .setTitle("Name taken")
                            .setMessage("A game already exists with the name '$gameName'. Please choose another")
                            .setPositiveButton("OK", null)
                            .show()
                        saveButton.isEnabled = true
                    } else {
                        saveDataToFireBase(gameName)
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Something went wrong! Please try again", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE || resultCode != RESULT_OK || data == null) {

        }

        data?.clipData?.let { result ->
            (0..result.itemCount).forEach { index ->
                if (chosenImageUris.size < index) {
                    chosenImageUris.add(result.getItemAt(index - 1).uri)
                }
            }
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose pics ${chosenImageUris.size} / ${boardSize.pairs()}"
        saveButton.isEnabled = shouldEnableButton()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        if (requestCode == REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchIntentForPhotos()
        } else {
            Toast.makeText(this, "Please grant permission in Order to choose photos", Toast.LENGTH_LONG).show()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun shouldEnableButton() =
        chosenImageUris.size == boardSize.pairs() && (etGameName.text.isNotBlank() && etGameName.text.length > 3)

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Choose pics"), 655)
    }

    private fun saveDataToFireBase(gameName: String) {
        pbUploading.visibility = VISIBLE
        for ((index, photoUri) in chosenImageUris.withIndex()) {
            val imageByteArray = getByteArray(photoUri)
            val filepath = "image/$gameName/${System.currentTimeMillis()}-$index.jpg"
            val photoRef = storage.reference.child(filepath)
            photoRef.putBytes(imageByteArray)
                .continueWithTask { photoUpload ->
                    photoRef.downloadUrl
                }.addOnCompleteListener { downloadUrlTask ->
                    if (!downloadUrlTask.isSuccessful) {
                        Log.i("CustomActivity", "Upload Failed", downloadUrlTask.exception)
                        Toast.makeText(this, "Upload Failed", Toast.LENGTH_LONG).show()
                        pbUploading.visibility = GONE
                        return@addOnCompleteListener
                    }
                    pbUploading.progress = uploadedImagesUrls.size * 100 / chosenImageUris.size
                    uploadedImagesUrls.add(downloadUrlTask.result.toString())
                    Log.i("CustomActivity", "Upload Success")
                    if (uploadedImagesUrls.size == chosenImageUris.size) {
                        handleAllImagesUploaded(gameName, uploadedImagesUrls)
                    }
                }
        }
    }

    private fun handleAllImagesUploaded(gameName: String, imagesUrls: MutableList<String>) {
        db.collection("games").document(gameName)
            .set(mapOf("images" to imagesUrls))
            .addOnCompleteListener { gameCreatingTask ->
                if (!gameCreatingTask.isSuccessful) {
                    pbUploading.visibility = GONE
                    Log.i("Activity", "Exception on game creating")
                    Toast.makeText(this, "failed to create your game", Toast.LENGTH_LONG).show()
                    return@addOnCompleteListener
                }
                AlertDialog.Builder(this)
                    .setTitle("Upload Completed! Let's play $gameName")
                    .setPositiveButton("OK") { _, _ ->
                        val resultData = Intent()
                        resultData.putExtra(EXTRA_GAME_NAME1, gameName)
                        setResult(RESULT_OK, resultData)
                        finish()
                    }.show()

            }
    }

    private fun getByteArray(photoUri: Uri): ByteArray {
        val originalBitMap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        val scaledBitMap = BitMapScaler.scaleToFitHeight(originalBitMap, 250)
        val outputSteam = ByteArrayOutputStream()
        scaledBitMap.compress(Bitmap.CompressFormat.JPEG, 60, outputSteam)
        return outputSteam.toByteArray()
    }
}
