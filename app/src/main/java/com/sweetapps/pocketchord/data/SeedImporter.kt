package com.sweetapps.pocketchord.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import android.util.Log

// Ensure chords for a specific root exist in the DB by reading `chords_seed_by_root.json` asset and inserting missing chords.
suspend fun ensureChordsForRoot(context: Context, root: String, assetFileName: String = "chords_seed_by_root.json") {
    withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val dao = db.chordDao()
            // Note: previously the function returned early if any chord existed for this root,
            // which prevented adding additional missing chords from the per-root seed file
            // (e.g. app had 2 C chords from the template, but chords_seed_by_root.json contained 3).
            // We intentionally no longer return early here — the insertion loop below already
            // checks for existing chords/variants and will only insert what is missing.
            // Keep a snapshot of existing chords for optional optimization/logging.
            val existing = dao.getChordsByRootOnce(root)
            // existing may be non-empty; continue to attempt inserting any missing chords/variants

            // read per-root seed JSON format: { "C": [ {name, variants: [{positions:[], fingers:[], firstFretIsNut:true}] }, ... ], ... }
            val json = context.assets.open(assetFileName).bufferedReader().use { it.readText() }
            val rootObj = JSONObject(json)
            if (!rootObj.has(root)) return@withContext
            val arr = rootObj.getJSONArray(root)
            db.withTransaction {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("name")
                    val type = obj.optString("type", null)
                    val tags = mutableListOf<String>()
                    if (obj.has("tags")) {
                        val tarr = obj.optJSONArray("tags")
                        if (tarr != null) for (ti in 0 until tarr.length()) tags.add(tarr.optString(ti))
                    }
                    val existingChord = dao.findChordByNameAndRoot(name, root)
                    val seedOrderValue = i
                    val chordId = if (existingChord == null) {
                        dao.insertChord(ChordEntity(name = name, root = root, type = type, tagsCsv = if (tags.isEmpty()) null else tags.joinToString(","), seedOrder = seedOrderValue))
                    } else {
                        // if seedOrder differs, update by replacing the chord entity while preserving id and other fields
                        if (existingChord.seedOrder != seedOrderValue) {
                            dao.insertChord(existingChord.copy(seedOrder = seedOrderValue))
                        } else existingChord.id
                    }

                    if (obj.has("variants")) {
                        val varr = obj.getJSONArray("variants")
                        for (vi in 0 until varr.length()) {
                            val vobj = varr.getJSONObject(vi)
                            val positions = mutableListOf<Int>()
                            val parr = vobj.optJSONArray("positions")
                            if (parr != null) for (pi in 0 until parr.length()) positions.add(parr.optInt(pi))
                            val fingers = mutableListOf<Int>()
                            val farr = vobj.optJSONArray("fingers")
                            if (farr != null) for (fi in 0 until farr.length()) fingers.add(farr.optInt(fi))
                            val positionsCsv = positions.joinToString(",")
                            val fingersCsv = if (fingers.isEmpty()) null else fingers.joinToString(",")
                            val firstNut = vobj.optBoolean("firstFretIsNut", true)
                            val existingVariant = dao.findVariantByChordIdPositionsAndFingers(chordId, positionsCsv, fingersCsv)
                            val shouldInsert = if (existingVariant == null) {
                                if (fingersCsv == null) dao.findVariantByChordIdAndPositionsCsv(chordId, positionsCsv) == null else true
                            } else false
                            if (shouldInsert) {
                                dao.insertVariant(VariantEntity(chordId = chordId, positionsCsv = positionsCsv, fingersCsv = fingersCsv, firstFretIsNut = firstNut))
                            }
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w("EnsureSeed", "Failed to ensure seed for root $root", t)
        }
    }
}

// One-time helper: delete all chords for a specific root and re-seed from `chords_seed_by_root.json`.
// Use for debugging/cleanup only (e.g., when DB already contains template entries you want replaced by per-root seeds).
suspend fun resetAndReseedRoot(context: Context, root: String, assetFileName: String = "chords_seed_by_root.json") {
    withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val dao = db.chordDao()
            db.withTransaction {
                val existing = dao.getChordsByRootOnce(root)
                existing.forEach { cwv ->
                    dao.deleteChord(cwv.chord)
                }
            }
            // Now ensure per-root seeds are inserted
            ensureChordsForRoot(context, root, assetFileName)
        } catch (t: Throwable) {
            Log.w("SeedImport", "resetAndReseedRoot failed for $root", t)
        }
    }
}
