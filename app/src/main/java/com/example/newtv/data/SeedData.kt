package com.example.newtv.data

object SeedData {
    suspend fun seed(database: AppDatabase) {
        val channels = listOf(
            ChannelEntity(
                id = "cartoons",
                name = "Saturday Morning Cartoons",
                ageRating = 7,
                kidSafeOnly = true
            ),
            ChannelEntity(
                id = "classic_nick",
                name = "Classic Nickelodeon",
                ageRating = 10,
                kidSafeOnly = false
            ),
            ChannelEntity(
                id = "disney_after_school",
                name = "Disney After School",
                ageRating = 7,
                kidSafeOnly = true
            )
        )

        val shows = listOf(
            ShowEntity(
                id = "spongebob",
                title = "SpongeBob SquarePants",
                provider = Provider.NETFLIX,
                deepLinkPattern = "intent://www.netflix.com/watch/{contentId}" +
                    "#Intent;scheme=https;package=com.netflix.ninja;end",
                contentId = "70155596",
                seasonCount = 3,
                episodeCount = 10,
                ageRating = 7
            ),
            ShowEntity(
                id = "kim_possible",
                title = "Kim Possible",
                provider = Provider.DISNEY,
                deepLinkPattern = "intent://www.disneyplus.com/video/{contentId}" +
                    "#Intent;scheme=https;package=com.disney.disneyplus;end",
                contentId = "b2b6f5d3-63ab-49df-8d0f-9f1f0c0a1111",
                seasonCount = 3,
                episodeCount = 10,
                ageRating = 7
            ),
            ShowEntity(
                id = "duck_tales",
                title = "DuckTales",
                provider = Provider.DISNEY,
                deepLinkPattern = "intent://www.disneyplus.com/video/{contentId}" +
                    "#Intent;scheme=https;package=com.disney.disneyplus;end",
                contentId = "2d6a2b34-693e-4b9c-9a76-2a2c2d0d2222",
                seasonCount = 2,
                episodeCount = 10,
                ageRating = 7
            ),
            ShowEntity(
                id = "hey_arnold",
                title = "Hey Arnold!",
                provider = Provider.NETFLIX,
                deepLinkPattern = "intent://www.netflix.com/watch/{contentId}" +
                    "#Intent;scheme=https;package=com.netflix.ninja;end",
                contentId = "80030435",
                seasonCount = 2,
                episodeCount = 10,
                ageRating = 10
            )
        )

        val channelShows = listOf(
            ChannelShowCrossRef("cartoons", "spongebob"),
            ChannelShowCrossRef("cartoons", "duck_tales"),
            ChannelShowCrossRef("disney_after_school", "kim_possible"),
            ChannelShowCrossRef("disney_after_school", "duck_tales"),
            ChannelShowCrossRef("classic_nick", "spongebob"),
            ChannelShowCrossRef("classic_nick", "hey_arnold")
        )

        val rules = listOf(
            ScheduleRuleEntity(
                id = "cartoons_morning",
                channelId = "cartoons",
                startMinute = 540,
                endMinute = 720,
                noRepeatWindow = 6,
                slotMinutes = 30,
                rotationMode = RotationMode.ROUND_ROBIN
            ),
            ScheduleRuleEntity(
                id = "cartoons_default",
                channelId = "cartoons",
                startMinute = 0,
                endMinute = 1440,
                noRepeatWindow = 4,
                slotMinutes = 30,
                rotationMode = RotationMode.ROUND_ROBIN
            ),
            ScheduleRuleEntity(
                id = "classic_nick_default",
                channelId = "classic_nick",
                startMinute = 0,
                endMinute = 1440,
                noRepeatWindow = 6,
                slotMinutes = 30,
                rotationMode = RotationMode.ROUND_ROBIN
            ),
            ScheduleRuleEntity(
                id = "disney_default",
                channelId = "disney_after_school",
                startMinute = 0,
                endMinute = 1440,
                noRepeatWindow = 4,
                slotMinutes = 30,
                rotationMode = RotationMode.ROUND_ROBIN
            )
        )

        database.channelDao().insertChannels(channels)
        database.showDao().insertShows(shows)
        database.showDao().insertChannelShows(channelShows)
        database.ruleDao().insertRules(rules)
    }
}
