package com.innosage.androidagentictemplate

import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServiceCrashTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @Test
    fun testResumeDoesNotCrash() {
        // Find the button with text "RESUME" and click it
        // Note: The UI starts in IDLE state with a RESUME button
        composeTestRule.onNodeWithText("RESUME").performClick()
        
        // Wait for potential crash
        composeTestRule.waitForIdle()
        
        // If it didn't crash, the PAUSE button should now be visible
        composeTestRule.onNodeWithText("PAUSE").assertExists()
        
        // Click PAUSE to stop
        composeTestRule.onNodeWithText("PAUSE").performClick()
        
        // Should be back to RESUME
        composeTestRule.onNodeWithText("RESUME").assertExists()
    }
}
