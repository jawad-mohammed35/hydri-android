package com.hydri.app

import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule

/** Scroll-to semantics know the viewport bounds, but not a floating sibling covering its bottom. */
fun SemanticsNodeInteraction.clearFloatingNavigation(
    rule: ComposeTestRule
): SemanticsNodeInteraction {
    rule.waitForIdle()
    if (
        rule.onAllNodes(isDialog()).fetchSemanticsNodes().isNotEmpty() ||
            fetchSemanticsNode().config.getOrNull(SemanticsProperties.Role) == Role.Tab
    )
        return this
    repeat(3) {
        if (rule.onAllNodesWithTag("sliding-navigation").fetchSemanticsNodes().isEmpty())
            return this
        val bar = rule.onNodeWithTag("sliding-navigation").getUnclippedBoundsInRoot()
        if (getUnclippedBoundsInRoot().bottom <= bar.top) return this
        rule.onNodeWithTag("page-viewport").performTouchInput {
            swipeUp(startY = height * .65f, endY = height * .3f, durationMillis = 350)
        }
        if (!rule.mainClock.autoAdvance) rule.mainClock.advanceTimeBy(400)
        rule.waitForIdle()
    }
    check(
        getUnclippedBoundsInRoot().bottom <=
            rule.onNodeWithTag("sliding-navigation").getUnclippedBoundsInRoot().top
    ) {
        "Control remains covered by floating navigation"
    }
    return this
}
