package priv.kit.ui

/** Display-only system guidance. The host owns visibility, localization, and any platform request. */
public data class PrivilegeUiPromptState(
    public val title: String,
    public val message: String,
)
