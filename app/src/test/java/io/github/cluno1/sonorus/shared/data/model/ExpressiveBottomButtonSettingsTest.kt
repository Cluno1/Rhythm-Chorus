package io.github.cluno1.sonorus.shared.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpressiveBottomButtonSettingsTest {
    @Test
    fun legacyNormalOrderGainsVisibleScoreButton() {
        assertEquals(
            listOf("SCORE", "DEVICE", "QUEUE", "MORE"),
            migrateLegacyExpressiveBottomButtonOrder(
                listOf("DEVICE", "QUEUE", "MORE"),
                mergeMode = false,
            ),
        )
    }

    @Test
    fun legacyMergeOrderRecoversFixedButtonsAndScoreButton() {
        assertEquals(
            listOf("LYRICS", "FAVORITE", "SCORE", "DEVICE", "QUEUE", "MORE"),
            migrateLegacyExpressiveBottomButtonOrder(
                listOf("DEVICE", "QUEUE", "MORE"),
                mergeMode = true,
            ),
        )
    }

    @Test
    fun currentCustomOrderIsPreserved() {
        val order = listOf("QUEUE", "SCORE", "DEVICE", "MORE")

        assertEquals(
            order,
            migrateLegacyExpressiveBottomButtonOrder(order, mergeMode = false),
        )
    }

    @Test
    fun unavailableScoreIsRemovedWithoutChangingOtherButtons() {
        assertEquals(
            listOf("QUEUE", "MORE"),
            resolveAvailableExpressiveBottomButtons(
                active = listOf("SCORE", "QUEUE", "MORE"),
                fallback = listOf("SCORE", "DEVICE", "QUEUE", "MORE"),
                scoreAvailable = false,
            ),
        )
    }

    @Test
    fun scoreOnlySelectionFallsBackWhenCurrentSongHasNoScore() {
        assertEquals(
            listOf("DEVICE", "QUEUE", "MORE"),
            resolveAvailableExpressiveBottomButtons(
                active = listOf("SCORE"),
                fallback = listOf("SCORE", "DEVICE", "QUEUE", "MORE"),
                scoreAvailable = false,
            ),
        )
    }
}
