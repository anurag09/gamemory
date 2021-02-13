package com.mishra.gamemory.modles

import com.google.j2objc.annotations.Property

data class UserListImages(
    @Property("images") val images: List<String>? = null
)
