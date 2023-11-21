package com.tushar.dboptimisation

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "users")
data class User(
    @ColumnInfo("email")
    val email: String?,
    @ColumnInfo("first_name")
    val firstName: String?,
    @ColumnInfo("gender")
    val gender: String?,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("id")
    val id: Long = 0L,
    @ColumnInfo("ip_address")
    val ipAddress: String?,
    @ColumnInfo("last_name")
    val lastName: String?
)