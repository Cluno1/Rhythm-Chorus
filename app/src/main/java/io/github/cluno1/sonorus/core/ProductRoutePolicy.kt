package io.github.cluno1.sonorus.core

/** Keeps legacy navigation hand-offs from reopening capabilities removed by Catalog-only builds. */
object ProductRoutePolicy {
    private val catalogOnlySettingsRoutes = setOf(
        "go_settings",
        "api_management_settings",
    )

    fun allowsInitialNavigationRoute(route: String?, catalogOnly: Boolean): Boolean {
        if (route.isNullOrBlank()) return false
        return !catalogOnly || !route.startsWith("streaming_", ignoreCase = true)
    }

    fun allowsSettingsSubroute(route: String?, catalogOnly: Boolean): Boolean {
        if (route.isNullOrBlank()) return false
        return !catalogOnly || route !in catalogOnlySettingsRoutes
    }
}
