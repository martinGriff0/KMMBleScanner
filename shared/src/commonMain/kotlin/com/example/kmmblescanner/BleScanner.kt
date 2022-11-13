package com.example.kmmblescanner

import io.ktor.util.logging.Logger

interface BleScanner {
    fun startScanner()

    fun stopScanner()

    fun passwordCheck(password: String): Boolean {
        return password == "password"
    }
}