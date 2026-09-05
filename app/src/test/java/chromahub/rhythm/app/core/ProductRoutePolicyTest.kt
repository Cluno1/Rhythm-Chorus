package chromahub.rhythm.app.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductRoutePolicyTest {
    @Test
    fun `catalog-only rejects legacy streaming navigation routes`() {
        assertFalse(ProductRoutePolicy.allowsInitialNavigationRoute("streaming_album/1/name", true))
        assertFalse(ProductRoutePolicy.allowsInitialNavigationRoute("streaming_service_setup/JELLYFIN", true))
        assertFalse(ProductRoutePolicy.allowsInitialNavigationRoute("streaming_go_settings", true))
    }

    @Test
    fun `catalog-only keeps first-party navigation routes`() {
        assertTrue(ProductRoutePolicy.allowsInitialNavigationRoute("home", true))
        assertTrue(ProductRoutePolicy.allowsInitialNavigationRoute("rhythm_stats", true))
    }

    @Test
    fun `catalog-only rejects removed settings panes`() {
        assertFalse(ProductRoutePolicy.allowsSettingsSubroute("go_settings", true))
        assertFalse(ProductRoutePolicy.allowsSettingsSubroute("updates_screen", true))
        assertFalse(ProductRoutePolicy.allowsSettingsSubroute("api_management_settings", true))
        assertTrue(ProductRoutePolicy.allowsSettingsSubroute("lyrics_settings", true))
    }

    @Test
    fun `general builds retain legacy routes`() {
        assertTrue(ProductRoutePolicy.allowsInitialNavigationRoute("streaming_album/1/name", false))
        assertTrue(ProductRoutePolicy.allowsSettingsSubroute("updates_screen", false))
    }
}
