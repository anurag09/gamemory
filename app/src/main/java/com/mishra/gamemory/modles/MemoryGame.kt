package com.mishra.gamemory.modles

import com.mishra.gamemory.DEFAULT_LISTS

class MemoryGame(
    val boardSize: BoardSize,
    val customGameImages: List<String>?
){
    val cards : List<MemoryCard>
    var numPairsFound = 0
    private var totalFlips = 0
    private var indexOfSingleSelectedCard: Int? = null

    init {
        if(customGameImages == null){
            val selectedIcons = DEFAULT_LISTS.shuffled().take(boardSize.pairs())
            cards = (selectedIcons + selectedIcons).shuffled().map { MemoryCard(it) }
        }else{
            val randomImages = (customGameImages + customGameImages).shuffled()
            cards = randomImages.map { MemoryCard(it.hashCode(), it) }
        }
    }

    fun flipCard(position: Int): Boolean {
        totalFlips++
        var foundMatch = false
        val card = cards[position]
        if (indexOfSingleSelectedCard == null){
            restoreCards()
            indexOfSingleSelectedCard = position
        }else{
            foundMatch = checkForMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }
        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(indexOfSingleSelectedCard: Int, currentPosition: Int):Boolean {
        if(cards[indexOfSingleSelectedCard].identifier != cards[currentPosition].identifier){
            return false
        }
        cards[currentPosition].isMatched = true
        cards[indexOfSingleSelectedCard].isMatched = true
        numPairsFound++
        return true
    }

    private fun restoreCards() {
        cards.map {
            if(!it.isMatched) {
                it.isFaceUp = false
            }
        }
    }

    fun haveWon(): Boolean {
        return numPairsFound == boardSize.pairs()
    }

    fun isCardFaceUp(position: Int) = cards[position].isFaceUp

    fun totalFlips(): Int {
        return totalFlips / 2
    }
}
