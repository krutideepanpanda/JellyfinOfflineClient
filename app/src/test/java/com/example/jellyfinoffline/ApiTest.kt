package com.example.jellyfinoffline
import org.junit.Test
import org.jellyfin.sdk.api.operations.ItemsApi
import org.jellyfin.sdk.api.operations.UserLibraryApi
import java.util.UUID

class ApiTest {
    @Test
    fun testApi() {
        val a = ItemsApi::getResumeItems
        val b = UserLibraryApi::getLatestMedia
    }
}
