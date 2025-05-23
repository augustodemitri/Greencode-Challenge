package com.example.greencodechallenge.utils

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic

object LogTestUtils {
    fun mockLog() {
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }
} 