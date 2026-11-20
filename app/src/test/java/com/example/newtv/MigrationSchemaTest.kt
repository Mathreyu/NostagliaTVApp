package com.example.newtv

import com.example.newtv.data.Migrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Checks the hand-written v3 -> v4 SQL against the schema JSON Room exports, without a device:
 * every index and column that appears in 4.json but not 3.json must be created by the migration
 * with the exact statement Room expects.
 */
class MigrationSchemaTest {
    private fun schema(version: Int): String {
        val file = listOf(
            File("schemas/com.example.newtv.data.AppDatabase/$version.json"),
            File("app/schemas/com.example.newtv.data.AppDatabase/$version.json")
        ).first { it.exists() }
        return file.readText()
    }

    private val indexRegex = Regex("\"tableName\": \"(\\w+)\"|\"createSql\": \"(CREATE INDEX[^\"]*)\"")

    /** Index create statements with the table placeholder resolved. */
    private fun indices(json: String): Set<String> {
        var table = ""
        val result = mutableSetOf<String>()
        for (m in indexRegex.findAll(json)) {
            if (m.groupValues[1].isNotEmpty()) table = m.groupValues[1]
            else result += m.groupValues[2].replace("\${TABLE_NAME}", "`$table`").replace("``", "`")
        }
        return result
    }

    @Test
    fun newIndicesAreCreatedByMigration() {
        val added = indices(schema(4)) - indices(schema(3))
        assertEquals(3, added.size)
        val migrationIndices = Migrations.V3_TO_V4.filter { it.startsWith("CREATE INDEX") }.toSet()
        assertEquals(added, migrationIndices)
    }

    @Test
    fun newColumnExistsInV4ButNotV3() {
        assertTrue(schema(4).contains("`episodeLinkPattern` TEXT"))
        assertTrue(!schema(3).contains("episodeLinkPattern"))
        assertTrue(Migrations.V3_TO_V4.any { it == "ALTER TABLE shows ADD COLUMN episodeLinkPattern TEXT" })
    }

    @Test
    fun schemaVersionIs4() {
        assertTrue(schema(4).contains("\"version\": 4"))
    }
}
