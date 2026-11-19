package priv.kit.ui

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Text owned by Privilege UI that is resolved only at a presentation boundary.
 *
 * Resource IDs are process-local presentation references and must not be persisted. [Literal]
 * is intended for already-materialized external or diagnostic text that cannot be represented by
 * a library resource.
 */
internal sealed interface PrivilegeUiText {
    data class Resource(
        @get:StringRes val id: Int,
        val formatArgs: List<Any> = emptyList(),
    ) : PrivilegeUiText

    data class Literal(
        val value: String,
    ) : PrivilegeUiText
}

/** Resolves this text against the current Compose configuration. */
@Composable
internal fun PrivilegeUiText.asString(): String =
    when (this) {
        is PrivilegeUiText.Resource -> stringResource(id, *formatArgs.toTypedArray())
        is PrivilegeUiText.Literal -> value
    }

/** Resolves this text at a non-Compose presentation boundary. */
internal fun PrivilegeUiText.asString(context: Context, locale: Locale? = null): String {
    val localizedContext = if (locale == null) {
        ContextCompat.getContextForLanguage(context)
    } else {
        context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(locale) },
        )
    }
    return when (this) {
        is PrivilegeUiText.Resource -> localizedContext.getString(id, *formatArgs.toTypedArray())
        is PrivilegeUiText.Literal -> value
    }
}

internal fun privilegeUiText(
    @StringRes id: Int,
    vararg formatArgs: Any,
): PrivilegeUiText = PrivilegeUiText.Resource(id, formatArgs.toList())
