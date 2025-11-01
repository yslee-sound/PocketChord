package com.sweetapps.pocketchord.data

import android.content.Context
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import android.util.Log
import java.security.MessageDigest
import androidx.core.content.pm.PackageInfoCompat

// Compute SHA-256 hex of a string
private fun sha256Hex(s: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val bytes = md.digest(s.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

// Ensure chords for a specific root exist in the DB by reading `chords_seed_by_root.json` asset and inserting missing chords.
suspend fun ensureChordsForRoot(context: Context, root: String, assetFileName: String = "chords_seed_by_root.json") {
    withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val dao = db.chordDao()
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
                            val orderHint = vobj.optString("order", "")
                            val topToBottom = vobj.optBoolean("topToBottom", false) ||
                                    orderHint.equals("topToBottom", true) || orderHint.equals("1-6", true) || orderHint.equals("top", true)
                            if (topToBottom) {
                                positions.reverse()
                                if (fingers.isNotEmpty()) fingers.reverse()
                            }
                            val positionsCsv = positions.joinToString(",")
                            val fingersCsv = if (fingers.isEmpty()) null else fingers.joinToString(",")
                            val firstNut = vobj.optBoolean("firstFretIsNut", true)
                            val barresJson = if (vobj.has("barres")) vobj.optJSONArray("barres")?.toString() else null
                            val existingVariant = dao.findVariantByChordIdPositionsAndFingers(chordId, positionsCsv, fingersCsv)
                            val shouldInsert = if (existingVariant == null) {
                                if (fingersCsv == null) dao.findVariantByChordIdAndPositionsCsv(chordId, positionsCsv) == null else true
                            } else false
                            if (shouldInsert) {
                                dao.insertVariant(VariantEntity(chordId = chordId, positionsCsv = positionsCsv, fingersCsv = fingersCsv, firstFretIsNut = firstNut, barresJson = barresJson))
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
            ensureChordsForRoot(context, root, assetFileName)
        } catch (t: Throwable) {
            Log.w("SeedImport", "resetAndReseedRoot failed for $root", t)
        }
    }
}

private fun currentAppVersionCode(context: Context): Long {
    val pm = context.packageManager
    val pInfo = pm.getPackageInfo(context.packageName, 0)
    return PackageInfoCompat.getLongVersionCode(pInfo)
}

// Only strategy kept: reseed automatically on app update (versionCode change).
suspend fun ensureOrReseedOnAppUpdate(
    context: Context,
    assetFileName: String = "chords_seed_by_root.json",
    prefsName: String = "pocketchord_prefs",
    versionKey: String = "last_seed_app_version_code"
) {
    withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val current = currentAppVersionCode(context)
            val seen = prefs.getLong(versionKey, -1L)
            if (seen == current) {
                Log.d("SeedVersion", "Already seeded for versionCode=$current, skipping")
                return@withContext
            }

            Log.i("SeedVersion", "Starting reseed: versionCode changed from $seen to $current")
            val startTime = System.currentTimeMillis()

            val db = AppDatabase.getInstance(context)
            val dao = db.chordDao()

            // 0) Read and validate JSON
            val json = context.assets.open(assetFileName).bufferedReader().use { it.readText() }
            val obj = JSONObject(json)

            // Check metadata if exists
            if (obj.has("_metadata")) {
                val meta = obj.getJSONObject("_metadata")
                val dataVersion = meta.optString("version", "unknown")
                Log.i("SeedVersion", "Seed data version: $dataVersion")
            }

            // 1) Capture favorites by natural key before wipe
            val favoriteKeys = dao.getFavoriteChordNaturalKeys().toSet()
            Log.i("SeedVersion", "Captured ${favoriteKeys.size} favorites before reseed")

            // 2) Wipe all data
            db.clearAllTables()
            Log.i("SeedVersion", "Database cleared")

            // 3) Reseed all roots from asset
            val names = obj.names()
            if (names != null) {
                var processedRoots = 0
                for (i in 0 until names.length()) {
                    val root = names.getString(i)
                    // Skip metadata entries
                    if (root.startsWith("_")) continue

                    ensureChordsForRoot(context, root, assetFileName)
                    processedRoots++
                    Log.d("SeedVersion", "Reseeded root: $root ($processedRoots/${names.length()})")
                }
                Log.i("SeedVersion", "Reseeded $processedRoots roots")
            }

            // 4) Restore favorites using natural key (name, root)
            val allRoots = names?.let {
                (0 until it.length())
                    .map { idx -> it.getString(idx) }
                    .filter { !it.startsWith("_") }  // Skip metadata
            } ?: emptyList()

            var restoredCount = 0
            for (r in allRoots) {
                val list = dao.getChordsByRootOnce(r)
                list.forEach { cwv ->
                    if (favoriteKeys.any { it.name == cwv.chord.name && it.root == cwv.chord.root }) {
                        dao.setChordFavorite(cwv.chord.id, true)
                        restoredCount++
                    }
                }
            }

            // 5) Update version marker
            prefs.edit().putLong(versionKey, current).apply()

            val elapsed = System.currentTimeMillis() - startTime
            Log.i("SeedVersion", "✓ Reseed completed in ${elapsed}ms: versionCode=$current, favorites restored=$restoredCount/${favoriteKeys.size}")
        } catch (t: Throwable) {
            Log.e("SeedVersion", "❌ ensureOrReseedOnAppUpdate failed", t)
            // Don't rethrow - allow app to continue with existing/empty DB
        }
    }
}
