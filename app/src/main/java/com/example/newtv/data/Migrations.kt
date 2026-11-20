package com.example.newtv.data

/** Plain SQL of the migrations that are worth checking against the exported schema without Android. */
object Migrations {
    /**
     * v3 -> v4: episode deep links, plus the indices that back the common lookups. The index
     * statements must match what Room generates (see app/schemas/.../4.json).
     */
    val V3_TO_V4: List<String> = listOf(
        "ALTER TABLE shows ADD COLUMN episodeLinkPattern TEXT",
        "CREATE INDEX IF NOT EXISTS `index_channel_shows_showId` ON `channel_shows` (`showId`)",
        "CREATE INDEX IF NOT EXISTS `index_schedule_rules_channelId` ON `schedule_rules` (`channelId`)",
        "CREATE INDEX IF NOT EXISTS `index_recent_episodes_channelId_playedAt` " +
            "ON `recent_episodes` (`channelId`, `playedAt`)"
    )
}
