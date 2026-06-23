package org.example.project.core

/** Salted password hash used for the local login (login screen + change-password in settings). */
fun saltedHash(salt: String, value: String): String = sha256Hex("$salt::$value")
