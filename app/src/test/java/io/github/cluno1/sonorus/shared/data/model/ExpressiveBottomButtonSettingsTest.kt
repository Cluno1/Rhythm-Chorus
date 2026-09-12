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

    @Test
    fun sixOrFewerButtonsRemainInTheBottomBar() {
        val active = listOf("LYRICS", "FAVORITE", "SCORE", "DEVICE", "QUEUE", "MORE")

        assertEquals(
            ExpressiveBottomButtonLayout(visible = active, overflow = emptyList()),
            resolveExpressiveBottomButtonLayout(
                active = active,
                fallback = emptyList(),
                scoreAvailable = true,
            ),
        )
    }

    @Test
    fun extraButtonsMoveUnderMoreAndMoreUsesTheSixthSlot() {
        assertEquals(
            ExpressiveBottomButtonLayout(
                visible = listOf("LYRICS", "FAVORITE", "SCORE", "DEVICE", "QUEUE", "MORE"),
                overflow = listOf("EQUALIZER", "SPEED"),
            ),
            resolveExpressiveBottomButtonLayout(
                active = listOf(
                    "LYRICS", "FAVORITE", "SCORE", "DEVICE", "QUEUE", "EQUALIZER", "SPEED", "MORE",
                ),
                fallback = emptyList(),
                scoreAvailable = true,
            ),
        )
    }

    @Test
    fun moreIsAddedAutomaticallyWhenOverflowExists() {
        assertEquals(
            ExpressiveBottomButtonLayout(
                visible = listOf("DEVICE", "QUEUE", "EQUALIZER", "SPEED", "SLEEP_TIMER", "MORE"),
                overflow = listOf("ADD_TO_PLAYLIST", "ALBUM"),
            ),
            resolveExpressiveBottomButtonLayout(
                active = listOf(
                    "DEVICE", "QUEUE", "EQUALIZER", "SPEED", "SLEEP_TIMER", "ADD_TO_PLAYLIST", "ALBUM",
                ),
                fallback = emptyList(),
                scoreAvailable = false,
            ),
        )
    }

    @Test
    fun unavailableScoreIsRemovedBeforeOverflowIsCalculated() {
        assertEquals(
            ExpressiveBottomButtonLayout(
                visible = listOf("DEVICE", "QUEUE", "EQUALIZER", "SPEED", "SLEEP_TIMER", "MORE"),
                overflow = listOf("ADD_TO_PLAYLIST"),
            ),
            resolveExpressiveBottomButtonLayout(
                active = listOf(
                    "SCORE", "DEVICE", "QUEUE", "EQUALIZER", "SPEED", "SLEEP_TIMER", "ADD_TO_PLAYLIST", "MORE",
                ),
                fallback = emptyList(),
                scoreAvailable = false,
            ),
        )
    }
}
