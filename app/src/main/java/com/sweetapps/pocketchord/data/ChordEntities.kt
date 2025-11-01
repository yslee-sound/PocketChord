package com.sweetapps.pocketchord.data

import androidx.room.*

@Entity(tableName = "chords")
data class ChordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val root: String,
    val type: String? = null,
    val tagsCsv: String? = null,
    // seedOrder: small integer representing the order from chords_seed_by_root.json for that root.
    // Lower values are shown earlier. Null means unspecified (will sort after specified orders).
    val seedOrder: Int? = null,
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "variants",
    foreignKeys = [
        ForeignKey(
            entity = ChordEntity::class,
            parentColumns = ["id"],
            childColumns = ["chordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // make uniqueness consider both positions and fingers so variants with same positions but different fingers are allowed
    indices = [Index(value = ["chordId", "positionsCsv", "fingersCsv"], unique = true), Index("chordId")]
)
data class VariantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chordId: Long,
    // positions stored as CSV string like "-1,3,2,0,1,0" representing strings 6..1
    val positionsCsv: String,
    // fingers stored as CSV string like "0,3,2,0,1,0" (same length as positions)
    val fingersCsv: String? = null,
    val firstFretIsNut: Boolean = true,
    val capoFret: Int? = null,
    val difficulty: Int? = null,
    val note: String? = null,
    // Optional barre specification persisted as JSON string (same schema as in seed: array of {fret,finger,fromString,toString})
    val barresJson: String? = null
)

// Relation wrapper used by Room to load a chord with all its variants
data class ChordWithVariants(
    @Embedded val chord: ChordEntity,
    @Relation(parentColumn = "id", entityColumn = "chordId") val variants: List<VariantEntity>
)
