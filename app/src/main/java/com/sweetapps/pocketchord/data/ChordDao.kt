package com.sweetapps.pocketchord.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChordDao {
    @Transaction
    @Query("SELECT * FROM chords WHERE root = :root ORDER BY name ASC")
    fun getChordsByRoot(root: String): Flow<List<ChordWithVariants>>

    @Query("SELECT * FROM chords WHERE name = :name AND root = :root LIMIT 1")
    suspend fun findChordByNameAndRoot(name: String, root: String): ChordEntity?

    @Transaction
    @Query("SELECT * FROM chords WHERE id = :id")
    fun getChordWithVariants(id: Long): Flow<ChordWithVariants?>

    @Query("SELECT * FROM chords WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchChords(query: String): Flow<List<ChordEntity>>

    @Query("SELECT * FROM variants WHERE chordId = :chordId")
    suspend fun getVariantsByChordId(chordId: Long): List<VariantEntity>

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
}
