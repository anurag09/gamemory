package com.mishra.gamemory

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.mishra.gamemory.modles.BoardSize

class ImagePickerAdapter(
    private val context: Context,
    private val chosenImageUris: MutableList<Uri>,
    private val boardSize: BoardSize,
    private val imageClickListener: ImageClickListener
) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    interface ImageClickListener {
        fun onPlaceHolderCLicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_image, parent, false)
        val cardWidth = parent.width / boardSize.widht()
        val cardHeight = parent.height / boardSize.height()
        val carSideLength = minOf(cardHeight, cardWidth)
        val layoutParam = view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams
        layoutParam.width = carSideLength
        layoutParam.height = carSideLength
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(position < chosenImageUris.size){
            holder.bind(chosenImageUris[position])
        }else{
            holder.bind()
        }
    }

    override fun getItemCount() = boardSize.pairs()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.ivCustomImage)

        fun bind(uri: Uri) {
            ivCustomImage.setImageURI(uri)
            ivCustomImage.setOnClickListener(null)
        }

        fun bind() {
            ivCustomImage.setOnClickListener{
                imageClickListener.onPlaceHolderCLicked()
            }
        }

    }

}
