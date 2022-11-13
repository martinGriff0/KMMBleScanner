package com.example.kmmblescanner

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform