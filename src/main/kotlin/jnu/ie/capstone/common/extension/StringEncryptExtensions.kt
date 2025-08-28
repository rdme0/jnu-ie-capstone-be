package jnu.ie.capstone.common.extension

import jnu.ie.capstone.common.security.util.AES256Util

fun String.encrypt(util: AES256Util): String = util.encrypt(this)
fun String.decrypt(util: AES256Util): String = util.decrypt(this)