package com.thando.accountable.ui.management.states.toolbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.rememberSaveable
import com.thando.accountable.ui.management.states.toolbar.scrollflags.EnterAlwaysCollapsedState
import com.thando.accountable.ui.management.states.toolbar.scrollflags.EnterAlwaysState
import com.thando.accountable.ui.management.states.toolbar.scrollflags.ExitUntilCollapsedState
import com.thando.accountable.ui.management.states.toolbar.scrollflags.ScrollState

@Stable
interface ToolbarState {
    val offset: Float
    val height: Float
    val progress: Float
    val consumed: Float
    var scrollTopLimitReached: Boolean
    var scrollOffset: Float

    enum class CollapseType{
        Scroll, EnterAlways, EnterAlwaysCollapsed, ExitUntilCollapsed
    }

    companion object {
        @Composable
        fun rememberToolbarState( collapseType: CollapseType, toolbarHeightRange: IntRange): ToolbarState {
            return when(collapseType){
                CollapseType.Scroll -> rememberSaveable(saver = ScrollState.Saver) {
                    ScrollState(toolbarHeightRange)
                }
                CollapseType.EnterAlways -> rememberSaveable(saver = EnterAlwaysState.Saver) {
                    EnterAlwaysState(toolbarHeightRange)
                }
                CollapseType.EnterAlwaysCollapsed -> rememberSaveable(saver = EnterAlwaysCollapsedState.Saver) {
                    EnterAlwaysCollapsedState(toolbarHeightRange)
                }
                CollapseType.ExitUntilCollapsed -> rememberSaveable(saver = ExitUntilCollapsedState.Saver) {
                    ExitUntilCollapsedState(toolbarHeightRange)
                }
            }
        }
    }
}