package li.gkd.app.ui.home

internal data class HomeAppearance(
    val floating: Boolean,
    val blur: Boolean,
    val glassAvailable: Boolean,
    val glass: Boolean,
)

/** Rendered capabilities only; changing a dependency never rewrites the saved glass preference. */
internal fun homeAppearance(
    floating: Boolean,
    blurPreference: Boolean,
    glassPreference: Boolean,
    shaderSupported: Boolean,
): HomeAppearance {
    val blur = shaderSupported && blurPreference
    val glassAvailable = floating && blur
    return HomeAppearance(floating, blur, glassAvailable, glassAvailable && glassPreference)
}
