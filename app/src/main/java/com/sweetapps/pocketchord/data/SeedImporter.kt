package com.sweetapps.pocketchord.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import android.util.Log

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

data class SeedImportResult(
    val insertedChords: Int,
    val insertedVariants: Int,
    val skippedVariants: Int,
    val errors: List<String>
)

suspend fun importSeedFromAssets(context: Context, assetFileName: String = "chords_seed_template.json"): SeedImportResult {
    return withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        var insertedChords = 0
        var insertedVariants = 0
        var skippedVariants = 0

        val json = try {
            context.assets.open(assetFileName).bufferedReader().use { it.readText() }
        } catch (t: Throwable) {
            errors.add("Failed to open asset: ${t.message}")
            "[]"
        }

        val seedList = mutableListOf<SeedChord>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name")
                val root = obj.optString("root")
                val type = obj.optString("type", null)
                val tags = mutableListOf<String>()
                if (obj.has("tags")) {
                    val tarr = obj.optJSONArray("tags")
                    if (tarr != null) {
                        for (ti in 0 until tarr.length()) tags.add(tarr.optString(ti))
                    }
                }
                val variants = mutableListOf<SeedVariant>()
                val varr = obj.optJSONArray("variants")
                if (varr != null) {
                    for (vi in 0 until varr.length()) {
                        val vobj = varr.getJSONObject(vi)
                        val positions = mutableListOf<Int>()
                        val parr = vobj.optJSONArray("positions")
                        if (parr != null) {
                            for (pi in 0 until parr.length()) positions.add(parr.optInt(pi))
                        }
                        val fingers = mutableListOf<Int>()
                        val farr = vobj.optJSONArray("fingers")
                        if (farr != null) {
                            for (fi in 0 until farr.length()) fingers.add(farr.optInt(fi))
                        }
                        val firstFretIsNut = vobj.optBoolean("firstFretIsNut", true)
                        val capoFret = if (vobj.has("capoFret")) vobj.optInt("capoFret") else null
                        val difficulty = if (vobj.has("difficulty")) vobj.optInt("difficulty") else null
                        val note = vobj.optString("note", null)
                        variants.add(SeedVariant(positions = positions, fingers = if (fingers.isEmpty()) null else fingers, firstFretIsNut = firstFretIsNut, capoFret = capoFret, difficulty = difficulty, note = note))
                    }
                }
                seedList.add(SeedChord(name = name, root = root, type = type, tags = if (tags.isEmpty()) null else tags, variants = variants))
            }
        } catch (e: Exception) {
            errors.add("Parse error: ${e.message}")
        }

        val db = AppDatabase.getInstance(context)
        val dao = db.chordDao()

        // Insert each chord and its variants (idempotent at variant level)
        seedList.forEach { sc ->
            // use Room's suspend-friendly transaction extension so we can call suspend DAO methods
            db.withTransaction {
                val existing = dao.findChordByNameAndRoot(sc.name, sc.root)
                val chordId = existing?.id ?: dao.insertChord(ChordEntity(name = sc.name, root = sc.root, type = sc.type, tagsCsv = sc.tags?.joinToString(","))).also { if (existing == null) insertedChords++ }

                sc.variants.forEach { sv ->
                    val positionsCsv = sv.positions.joinToString(",")
                    val fingersCsv = sv.fingers?.joinToString(",")
                    // stricter duplicate check: match both positions and fingers (treat null/empty fingers as equivalent)
                    val existingVariant = dao.findVariantByChordIdPositionsAndFingers(chordId, positionsCsv, fingersCsv)
                    // fallback: if fingers were null and no exact match, also check positions-only (older variants may have fingers == null)
                    val shouldInsert = if (existingVariant == null) {
                        if (fingersCsv == null) {
                            dao.findVariantByChordIdAndPositionsCsv(chordId, positionsCsv) == null
                        } else true
                    } else false
                    if (shouldInsert) {
                        dao.insertVariant(VariantEntity(
                            chordId = chordId,
                            positionsCsv = positionsCsv,
                            fingersCsv = fingersCsv,
                            firstFretIsNut = sv.firstFretIsNut,
                            capoFret = sv.capoFret,
                            difficulty = sv.difficulty,
                            note = sv.note
                        ))
                        insertedVariants++
                    } else skippedVariants++
                }
            }
        }

        val result = SeedImportResult(insertedChords = insertedChords, insertedVariants = insertedVariants, skippedVariants = skippedVariants, errors = errors)

        // store a compact summary in SharedPreferences for the UI Settings to read
        try {
            val prefs = context.getSharedPreferences("pocketchord_prefs", Context.MODE_PRIVATE)
            val summary = "insertedChords=${result.insertedChords};insertedVariants=${result.insertedVariants};skippedVariants=${result.skippedVariants};errors=${result.errors.size}"
            prefs.edit().putString("last_seed_import_summary", summary).apply()
        } catch (t: Throwable) {
            Log.w("SeedImport", "Failed to write import summary to prefs", t)
        }

        result
    }
}
