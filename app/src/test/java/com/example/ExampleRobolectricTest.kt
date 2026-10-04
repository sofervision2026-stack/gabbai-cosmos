package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.CosmicVault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Gabbai Cosmos", appName)
    }

    @Test
    fun `test AES-256 encryption and decryption`() {
        val originalText = "Confidential Gabbai note for Sefer Torah dedication"
        val encrypted = CosmicVault.encrypt(originalText)
        assertNotEquals(originalText, encrypted)

        val decrypted = CosmicVault.decrypt(encrypted)
        assertEquals(originalText, decrypted)
    }

    @Test
    fun `test SHA-256 ledger block hash computation`() {
        val hash = CosmicVault.computeLedgerHash(
            id = 1L,
            prevHash = "0000000000000000",
            timestamp = 1700000000000L,
            amount = 1200.0,
            type = "INCOME",
            title = "Annual Membership"
        )
        assertTrue(hash.isNotEmpty())
        assertEquals(64, hash.length) // 64 hex characters for SHA-256
    }
}
