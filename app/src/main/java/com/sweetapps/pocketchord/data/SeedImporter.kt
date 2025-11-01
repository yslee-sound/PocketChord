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

// Public helper to determine per-root seed asset filename (avoid special chars like '#')
fun seedAssetFileNameForRoot(root: String): String {
    // Normalize: C, C#, D, D#, E, F, F#, G, G#, A, A#, B => C, Cs, D, Ds, E, F, Fs, G, Gs, A, As, B
    val normalized = root.trim().replace("#", "s").replace("/", "_")
    return "chords_seed_${normalized}.json"
}

// Internal: parse asset content and get JSONArray of chords for the root.
private fun parseChordArrayFromAssetContent(content: String, root: String): org.json.JSONArray? {
    val trimmed = content.trimStart()
    return try {
        when {
            trimmed.startsWith("{") -> {
                val rootObj = JSONObject(content)
                if (!rootObj.has(root)) return null
                rootObj.getJSONArray(root)
            }
            trimmed.startsWith("[") -> {
                org.json.JSONArray(content)
            }
            else -> null
        }
    } catch (t: Throwable) {
        Log.w("SeedParse", "Failed to parse seed content for root=${root}", t)
        null
    }
}

// Ensure chords for a specific root exist in the DB by reading seed asset (per-root array or combined object) and inserting missing chords.
suspend fun ensureChordsForRoot(context: Context, root: String, assetFileName: String = "chords_seed_by_root.json") {
    withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val dao = db.chordDao()
            val content = context.assets.open(assetFileName).bufferedReader().use { it.readText() }

            // Support proxy format: {"_include":"chords_seed_by_root.json", "root":"C"}
            val trimmed = content.trimStart()
            val arr = try {
                if (trimmed.startsWith("{")) {
                    val obj = org.json.JSONObject(content)
                    if (obj.has("_include")) {
                        val includeFile = obj.optString("_include", assetFileName)
                        val includeRoot = obj.optString("root", root)
                        val base = context.assets.open(includeFile).bufferedReader().use { it.readText() }
                        parseChordArrayFromAssetContent(base, includeRoot)
                    } else {
                        parseChordArrayFromAssetContent(content, root)
                    }
                } else {
                    parseChordArrayFromAssetContent(content, root)
                }
            } catch (_: Throwable) { parseChordArrayFromAssetContent(content, root) }
                ?: return@withContext

            db.withTransaction {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("name")
                    // safe nullable type extraction (org.json.optString requires non-null fallback)
                    val type = if (obj.has("type")) obj.optString("type") else null
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
            Log.w("EnsureSeed", "Failed to ensure seed for root ${root} from asset ${assetFileName}", t)
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
            Log.w("SeedImport", "resetAndReseedRoot failed for ${root}", t)
        }
    }
}

private fun currentAppVersionCode(context: Context): Long {
    val pm = context.packageManager
    val pInfo = pm.getPackageInfo(context.packageName, 0)
    return PackageInfoCompat.getLongVersionCode(pInfo)
}

private val ALL_ROOTS = listOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")

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
                Log.d("SeedVersion", "Already seeded for versionCode=${current}, skipping")
                return@withContext
            }

            Log.i("SeedVersion", "Starting reseed: versionCode changed from ${seen} to ${current}")
            val startTime = System.currentTimeMillis()

            val db = AppDatabase.getInstance(context)
            val dao = db.chordDao()

            // Detect assets
            val assetNames = try { context.assets.list("")?.toSet() } catch (_: Throwable) { null } ?: emptySet()
            val hasPerRoot = assetNames.contains(seedAssetFileNameForRoot("C"))
            val hasCombined = assetNames.contains(assetFileName)

            var dataVersion: String? = null
            if (hasCombined) {
                // Read metadata from combined file if available
                try {
                    val json = context.assets.open(assetFileName).bufferedReader().use { it.readText() }
                    val obj = JSONObject(json)
                    if (obj.has("_metadata")) dataVersion = obj.getJSONObject("_metadata").optString("version", "")
                } catch (_: Throwable) {}
            }

            // 1) Capture favorites by natural key before wipe
            val favoriteKeys = dao.getFavoriteChordNaturalKeys().toSet()
            Log.i("SeedVersion", "Captured ${favoriteKeys.size} favorites before reseed")

            // 2) Wipe all data
            db.clearAllTables()
            Log.i("SeedVersion", "Database cleared")

            // 3) Reseed all roots
            val allRoots: List<String>
            if (hasCombined) {
                // Prefer combined for efficiency
                val json = context.assets.open(assetFileName).bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                val names = obj.names()
                allRoots = names?.let { (0 until it.length()).map { idx -> it.getString(idx) }.filter { !it.startsWith("_") } } ?: emptyList()
                var processedRoots = 0
                for (root in allRoots) {
                    ensureChordsForRoot(context, root, assetFileName)
                    processedRoots++
                    Log.d("SeedVersion", "Reseeded root: ${root} (${processedRoots}/${allRoots.size})")
                }
                Log.i("SeedVersion", "Reseeded ${processedRoots} roots (combined mode)")
            } else if (hasPerRoot) {
                allRoots = ALL_ROOTS
                var processed = 0
                for (r in ALL_ROOTS) {
                    val perRootFile = seedAssetFileNameForRoot(r)
                    if (assetNames.contains(perRootFile)) {
                        ensureChordsForRoot(context, r, perRootFile)
                        processed++
                        Log.d("SeedVersion", "Reseeded root (per-file): ${r}")
                    } else {
                        Log.w(
                            "SeedVersion",
                            "Missing per-root asset ${perRootFile} for root ${r} — skipping"
                        )
                    }
                }
                Log.i("SeedVersion", "Reseeded ${processed} roots (per-file mode)")
            } else {
                // No recognized assets found
                Log.w("SeedVersion", "No seed assets found in assets dir")
                allRoots = emptyList()
            }

            // 4) Restore favorites using natural key (name, root)
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
            val seedVerSuffix = dataVersion?.takeIf { it.isNotEmpty() }?.let { ", seedVersion=${it}" } ?: ""
            val msg = "✓ Reseed completed in ${elapsed}ms: versionCode=${current}, favorites restored=${restoredCount}/${favoriteKeys.size}${seedVerSuffix}"
            Log.i("SeedVersion", msg)
        } catch (t: Throwable) {
            Log.e("SeedVersion", "❌ ensureOrReseedOnAppUpdate failed", t)
            // Don't rethrow - allow app to continue with existing/empty DB
        }
    }
}
