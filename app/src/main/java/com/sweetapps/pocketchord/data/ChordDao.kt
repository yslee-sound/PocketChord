package com.sweetapps.pocketchord.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChordDao {
    @Transaction
    @Query("SELECT * FROM chords WHERE root = :root ORDER BY CASE WHEN seedOrder IS NULL THEN 1 ELSE 0 END, seedOrder ASC, name ASC")
    fun getChordsByRoot(root: String): Flow<List<ChordWithVariants>>

    // one-shot suspend variant used by seed insertion logic
    @Transaction
    @Query("SELECT * FROM chords WHERE root = :root ORDER BY CASE WHEN seedOrder IS NULL THEN 1 ELSE 0 END, seedOrder ASC, name ASC")
    suspend fun getChordsByRootOnce(root: String): List<ChordWithVariants>

    @Query("SELECT * FROM chords WHERE name = :name AND root = :root LIMIT 1")
    suspend fun findChordByNameAndRoot(name: String, root: String): ChordEntity?

    @Transaction
    @Query("SELECT * FROM chords WHERE id = :id")
    fun getChordWithVariants(id: Long): Flow<ChordWithVariants?>

    @Query("SELECT * FROM chords WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchChords(query: String): Flow<List<ChordEntity>>

    @Query("SELECT * FROM variants WHERE chordId = :chordId")
    suspend fun getVariantsByChordId(chordId: Long): List<VariantEntity>

    // debug helper: return all variant rows
    @Query("SELECT * FROM variants")
    suspend fun getAllVariants(): List<VariantEntity>

    @Query("SELECT * FROM variants WHERE chordId = :chordId AND positionsCsv = :positionsCsv LIMIT 1")
    suspend fun findVariantByChordIdAndPositionsCsv(chordId: Long, positionsCsv: String): VariantEntity?

    @Query("SELECT * FROM variants WHERE chordId = :chordId AND positionsCsv = :positionsCsv AND COALESCE(fingersCsv, '') = COALESCE(:fingersCsv, '') LIMIT 1")
    suspend fun findVariantByChordIdPositionsAndFingers(chordId: Long, positionsCsv: String, fingersCsv: String?): VariantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChord(chord: ChordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariant(variant: VariantEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChords(chords: List<ChordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariants(variants: List<VariantEntity>)

    @Delete
    suspend fun deleteChord(chord: ChordEntity)

    @Delete
    suspend fun deleteVariant(variant: VariantEntity)

    // Favorites preservation
    @Query("SELECT name, root FROM chords WHERE favorite = 1")
    suspend fun getFavoriteChordNaturalKeys(): List<FavoriteKey>

    @Query("UPDATE chords SET favorite = :favorite WHERE id = :id")
    suspend fun setChordFavorite(id: Long, favorite: Boolean)
}

// Natural key projection to preserve favorites across reseed
data class FavoriteKey(
    val name: String,
    val root: String
)
