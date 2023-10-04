/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.fxsuggest.facts

import mozilla.components.feature.fxsuggest.FxSuggestClickInfo
import mozilla.components.support.base.Component
import mozilla.components.support.base.facts.Action
import mozilla.components.support.base.facts.Fact
import mozilla.components.support.base.facts.collect

/**
 * Facts emitted for telemetry related to the Firefox Suggest feature.
 */
class FxSuggestFacts {
    /**
     * Specific types of telemetry items.
     */
    object Items {
        const val SPONSORED_SUGGESTION_CLICKED = "sponsored_suggestion_clicked"
    }
}

private fun emitFxSuggestFact(
    action: Action,
    item: String,
    value: String? = null,
    metadata: Map<String, Any>? = null,
) {
    Fact(
        Component.FEATURE_FXSUGGEST,
        action,
        item,
        value,
        metadata,
    ).collect()
}

internal fun emitSponsoredSuggestionClickedFact(clickInfo: FxSuggestClickInfo) {
    emitFxSuggestFact(
        Action.INTERACTION,
        FxSuggestFacts.Items.SPONSORED_SUGGESTION_CLICKED,
        metadata = mapOf("clickInfo" to clickInfo),
    )
}
