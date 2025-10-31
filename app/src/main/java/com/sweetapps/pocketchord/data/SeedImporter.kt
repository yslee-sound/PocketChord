package com.sweetapps.pocketchord.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Data classes for parsing seed JSON. These are separate from Room entities for clarity.
private data class SeedVariant(
    val positions: List<Int>,
    val fingers: List<Int>? = null,
    val firstFretIsNut: Boolean = true,
    val capoFret: Int? = null,
    val difficulty: Int? = null,
    val note: String? = null
)

private data class SeedChord(
    val name: String,
    val root: String,
    val type: String? = null,
    val tags: List<String>? = null,
    val variants: List<SeedVariant> = emptyList()
)

suspend fun importSeedFromAssets(context: Context, assetFileName: String = "chords_seed_template.json") {
    withContext(Dispatchers.IO) {
        val json = context.assets.open(assetFileName).bufferedReader().use { it.readText() }
        val listType = object : TypeToken<List<SeedChord>>() {}.type
        val seedList: List<SeedChord> = Gson().fromJson(json, listType)

        val db = AppDatabase.getInstance(context)
        val dao = db.chordDao()

        // Insert each chord and its variants
        seedList.forEach { sc ->
            val existing = dao.findChordByNameAndRoot(sc.name, sc.root)
            val chordId = existing?.id ?: dao.insertChord(ChordEntity(name = sc.name, root = sc.root, type = sc.type, tagsCsv = sc.tags?.joinToString(",")))
            val variants = sc.variants.map { sv ->
                VariantEntity(
                    chordId = chordId,
                    positionsCsv = sv.positions.joinToString(","),
                    fingersCsv = sv.fingers?.joinToString(","),
                    firstFretIsNut = sv.firstFretIsNut,
                    capoFret = sv.capoFret,
                    difficulty = sv.difficulty,
                    note = sv.note
                )
            }
            if (variants.isNotEmpty()) dao.insertVariants(variants)
        }
    }
}
