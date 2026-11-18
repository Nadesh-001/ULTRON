import android.content.Context
import java.util.Locale

/**
 * Enforces English as the application-wide default locale and language.
 * Guarantees that system locale or Russian localization cannot override the English default.
 */
object UltronLanguageEnforcer {

    val DEFAULT_LOCALE: Locale = Locale.ENGLISH

    /**
     * Applies English locale to the application or activity context configuration.
     */
    fun applyEnglishLocale(context: Context): Context {
        Locale.setDefault(DEFAULT_LOCALE)
        val config = context.resources.configuration
        config.setLocale(DEFAULT_LOCALE)
        config.setLayoutDirection(DEFAULT_LOCALE)
        return context.createConfigurationContext(config)
    }

    /**
     * Helper to guarantee English text output over any legacy Russian branches.
     */
    fun resolve(english: String, @Suppress("UNUSED_PARAMETER") fallback: String? = null): String {
        return english
    }
}
