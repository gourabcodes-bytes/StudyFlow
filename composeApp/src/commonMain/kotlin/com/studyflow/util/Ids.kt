package com.studyflow.util

import kotlin.random.Random

/** Unique enough for a personal database that two devices write to: timestamp + 64 random bits. */
fun newId(): String = nowMillis().toString(36) + "-" + Random.nextLong().toULong().toString(36)
