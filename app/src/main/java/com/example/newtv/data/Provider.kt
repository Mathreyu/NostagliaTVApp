package com.example.newtv.data

enum class Provider(
    val packageName: String,
    val displayName: String
) {
    NETFLIX("com.netflix.ninja", "Netflix"),
    DISNEY("com.disney.disneyplus", "Disney+");
}
