package com.kindlevibe.reader.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

object Dispatchers {
    val Main: CoroutineDispatcher = Dispatchers.Main
    val IO: CoroutineDispatcher = Dispatchers.IO
    val Default: CoroutineDispatcher = Dispatchers.Default
    val Unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
