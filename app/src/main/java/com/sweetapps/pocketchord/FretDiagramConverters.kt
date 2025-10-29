package com.sweetapps.pocketchord

/**
 * Utilities to convert between DB/JSON string-number based format (1=top) and
 * app-internal positions List format (index 0 = lowest string).
 */

/**
 * Convert a map of (stringNumber -> fret) where stringNumber is 1..stringCount (1 = topmost string)
 * into an internal positions List<Int> of length stringCount where index 0 = lowest string.
 *
 * Missing stringNumbers will be filled with defaultFret (default = -1 for mute)
 */
fun dbMapToInternalPositions(dbMap: Map<Int, Int>, stringCount: Int = 6, defaultFret: Int = -1): List<Int> {
    val res = MutableList(stringCount) { defaultFret }
    for ((stringNumber, fret) in dbMap) {
        if (stringNumber < 1 || stringNumber > stringCount) continue
        val internalIndex = stringCount - stringNumber
        res[internalIndex] = fret
    }
    return res
}

/**
 * Convert internal positions List (index 0 = lowest string) into a map keyed by DB stringNumber (1=top)
 */
fun internalPositionsToDbMap(positions: List<Int>): Map<Int, Int> {
    val stringCount = positions.size
    val map = mutableMapOf<Int, Int>()
    for (i in positions.indices) {
        val internalIndex = i
        val stringNumber = stringCount - internalIndex
        map[stringNumber] = positions[i]
    }
    return map
}
