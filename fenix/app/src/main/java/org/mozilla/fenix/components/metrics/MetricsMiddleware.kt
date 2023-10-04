/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import mozilla.components.feature.fxsuggest.FxSuggestInteractionInfo
import mozilla.components.feature.fxsuggest.facts.FxSuggestFacts
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import org.mozilla.fenix.GleanMetrics.FxSuggest
import org.mozilla.fenix.GleanMetrics.Pings
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState
import java.util.UUID

/**
 * A middleware that will map incoming actions to relevant events for [metrics].
 */
class MetricsMiddleware(
    private val metrics: MetricController,
) : Middleware<AppState, AppAction> {
    override fun invoke(
        context: MiddlewareContext<AppState, AppAction>,
        next: (AppAction) -> Unit,
        action: AppAction,
    ) {
        handleAction(context, action)
        next(action)
    }

    private fun handleAction(context: MiddlewareContext<AppState, AppAction>, action: AppAction) = when (action) {
        is AppAction.AppLifecycleAction.ResumeAction -> {
            metrics.track(Event.GrowthData.SetAsDefault)
            metrics.track(Event.GrowthData.FirstAppOpenForDay)
            metrics.track(Event.GrowthData.FirstWeekSeriesActivity)
            metrics.track(Event.GrowthData.UsageThreshold)
            metrics.track(Event.GrowthData.UserActivated(fromSearch = false))
        }

        is AppAction.AwesomeBarAction.EngagementFinished -> {
            context.state.awesomeBarState.visibilityState.visibleProviderGroups.entries.flatMapIndexed { groupIndex, (_, suggestions) ->
                suggestions.mapIndexedNotNull { suggestionIndex, suggestion ->
                    (suggestion.metadata?.get(FxSuggestFacts.MetadataKeys.CLICK_INFO) as? FxSuggestInteractionInfo)?.let {
                        val positionInGroup = suggestionIndex.toLong() + 1
                        FxSuggestInteraction(
                            interactionInfo = it,
                            positionInGroup = positionInGroup,
                            positionInAwesomeBar = groupIndex.toLong() + positionInGroup,
                            wasClicked = context.state.awesomeBarState.clickedSuggestion == suggestion,
                        )
                    }
                }
            }.forEach { it.submit() }
        }
        else -> Unit
    }

    private data class FxSuggestInteraction(
        val interactionInfo: FxSuggestInteractionInfo,
        val positionInGroup: Long,
        val positionInAwesomeBar: Long,
        val wasClicked: Boolean,
    ) {
        fun submit() {
            if (interactionInfo !is FxSuggestInteractionInfo.Amp) {
                return
            }
            val pingTypes = if (wasClicked) {
                // If the user clicked on a sponsored Firefox Suggestion, send an impression ping and a click
                // ping.
                listOf("fxsuggest-impression", "fxsuggest-click")
            } else {
                // If the user clicked on a different suggestion, but saw a sponsored Firefox Suggestion when
                // they finished engaging with the awesomebar, send an impression ping only.
                listOf("fxsuggest-impression")
            }
            pingTypes.forEach { pingType ->
                FxSuggest.pingType.set(pingType)
                FxSuggest.position.set(positionInAwesomeBar)
                FxSuggest.suggestedIndex.set(positionInGroup)
                FxSuggest.blockId.set(interactionInfo.blockId)
                FxSuggest.advertiser.set(interactionInfo.advertiser)
                FxSuggest.isClicked.set(wasClicked)
                FxSuggest.reportingUrl.set(interactionInfo.clickUrl)
                FxSuggest.iabCategory.set(interactionInfo.iabCategory)
                FxSuggest.contextId.set(UUID.fromString(interactionInfo.contextId))
                Pings.fxSuggest.submit()
            }
        }
    }
}
