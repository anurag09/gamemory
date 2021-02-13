package com.mishra.gamemory

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.mishra.gamemory.modles.BoardSize
import com.mishra.gamemory.modles.MemoryCard
import com.squareup.picasso.Picasso

class MemoryBoardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    val cards: List<MemoryCard>,
    private val cardClickListener: CardClickListener
) :
    RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

    companion object{
        private const val MARGIN = 10
        private const val TAG = "MemoryBoardAdapter"
    }

    interface CardClickListener {
        fun onCardClocked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardWidth = parent.width/ boardSize.widht() - (2 * MARGIN)
        val cardHeight = parent.height/ boardSize.height() - (2 * MARGIN)
        val cardSideLength = minOf(cardHeight, cardWidth)
        val view = LayoutInflater.from(context).inflate(R.layout.memory_card, parent, false)
        val cardLayoutParam = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        cardLayoutParam.height = cardSideLength
        cardLayoutParam.width = cardSideLength
        cardLayoutParam.setMargins(MARGIN, MARGIN, MARGIN, MARGIN)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount() = boardSize.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)
        fun bind(position: Int) {
            val card = cards[position]
            if(card.isFaceUp){
                if(card.imageUrl != null){
                    Picasso.get().load(card.imageUrl).placeholder(R.drawable.ic_airline_seat).into(imageButton)
                }else{
                    imageButton.setImageResource(card.identifier)
                }
            }
            else {
                imageButton.setImageResource(R.drawable.bamboo)
            }


            imageButton.alpha = if(card.isMatched) .4f else 1.0f
            val colorSteList = if(card.isMatched) ContextCompat.getColorStateList(context, R.color.color_gray) else null
            ViewCompat.setBackgroundTintList(imageButton, colorSteList)

            imageButton.setOnClickListener{
                Log.i(TAG, "I just clicked on $position")
                cardClickListener.onCardClocked(position)
            }
        }
    }
}
