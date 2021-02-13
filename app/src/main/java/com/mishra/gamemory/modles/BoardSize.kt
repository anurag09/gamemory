package com.mishra.gamemory.modles

enum class BoardSize(val size: Int) {
    EASY(8),
    MEDIUM(18),
    HARD(24);

    companion object{
        fun getByValue(value: Int) = values().first{ it.size == value }
    }

    fun widht() = when(this){
        EASY -> 2
        MEDIUM -> 3
        HARD -> 4
    }

    fun height() = size / widht()

    fun pairs() = size / 2
}