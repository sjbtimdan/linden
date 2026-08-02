package org.sjbtimdan.linden

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform