package com.example.newtv.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun providerToString(provider: Provider): String = provider.name

    @TypeConverter
    fun stringToProvider(value: String): Provider = Provider.valueOf(value)

    @TypeConverter
    fun rotationToString(mode: RotationMode): String = mode.name

    @TypeConverter
    fun stringToRotation(value: String): RotationMode = RotationMode.valueOf(value)
}
