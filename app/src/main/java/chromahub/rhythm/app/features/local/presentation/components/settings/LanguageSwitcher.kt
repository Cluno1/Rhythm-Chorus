package chromahub.rhythm.app.features.local.presentation.components.settings

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import java.util.Locale
import chromahub.rhythm.app.R
import androidx.compose.ui.res.stringResource

data class LanguageOption(
    val code: String,
    val displayName: String,
    val nativeName: String
)

object LanguageHelper {
    val supportedLanguages = listOf(
        LanguageOption("en", "English", "English"),
        LanguageOption("ar", "Arabic", "العربية"),
        LanguageOption("bn", "Bengali", "বাংলা"),
        LanguageOption("de", "German", "Deutsch"),
        LanguageOption("es", "Spanish", "Español"),
        LanguageOption("fr", "French", "Français"),
        LanguageOption("fr-CA", "French (Canada)", "Français (Canada)"),
        LanguageOption("hi", "Hindi", "हिन्दी"),
        LanguageOption("id", "Indonesian", "Bahasa Indonesia"),
        LanguageOption("it", "Italian", "Italiano"),
        LanguageOption("ja", "Japanese", "日本語"),
        LanguageOption("ko", "Korean", "한국어"),
        LanguageOption("nl", "Dutch", "Nederlands"),
        LanguageOption("pl", "Polish", "Polski"),
        LanguageOption("pt", "Portuguese", "Português"),
        LanguageOption("pt-BR", "Portuguese (Brazil)", "Português (Brasil)"),
        LanguageOption("ru", "Russian", "Русский"),
        LanguageOption("sv", "Swedish", "Svenska"),
        LanguageOption("ta", "Tamil", "தமிழ்"),
        LanguageOption("th", "Thai", "ไทย"),
        LanguageOption("tr", "Turkish", "Türkçe"),
        LanguageOption("uk", "Ukrainian", "Українська"),
        LanguageOption("vi", "Vietnamese", "Tiếng Việt"),
        LanguageOption("zh", "Chinese (Simplified)", "简体中文"),
        LanguageOption("zh-TW", "Chinese (Traditional)", "繁體中文")
    )
    
    fun getCurrentLanguage(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
            localeManager?.applicationLocales?.get(0) ?: Locale.getDefault()
        } else {
            val locales = AppCompatDelegate.getApplicationLocales()
            if (locales.isEmpty) {
                Locale.getDefault()
            } else {
                locales.get(0) ?: Locale.getDefault()
            }
        }
        
        val languageTag = locale.toLanguageTag()
        // Try to match the exact language tag (e.g. fr-CA, pt-BR, zh-TW)
        val exactMatch = supportedLanguages.firstOrNull { it.code.equals(languageTag, ignoreCase = true) }
        if (exactMatch != null) return exactMatch.code
        
        // Try to match by language code (e.g. fr, pt, zh)
        val languageCode = locale.language
        val codeMatch = supportedLanguages.firstOrNull { it.code.equals(languageCode, ignoreCase = true) }
        if (codeMatch != null) return codeMatch.code
        
        return "en" // default fallback
    }
    
    fun setLanguage(context: Context, languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
            localeManager?.applicationLocales = LocaleList.forLanguageTags(languageCode)
        } else {
            val locale = Locale.forLanguageTag(languageCode)
            val localeList = LocaleListCompat.create(locale)
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSwitcherDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(LanguageHelper.getCurrentLanguage(context)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = RhythmIcons.Language,
                contentDescription = stringResource(R.string.cd_language),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.languageswitcher_select_language),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(LanguageHelper.supportedLanguages, key = { "lang_${it.code}" }) { language ->
                    LanguageItem(
                        language = language,
                        isSelected = currentLanguage == language.code,
                        onClick = {
                            currentLanguage = language.code
                            LanguageHelper.setLanguage(context, language.code)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Icon(
                    imageVector = RhythmIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.ui_cancel))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun LanguageItem(
    language: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = language.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = RhythmIcons.Check,
                    contentDescription = stringResource(R.string.streaming_selected),
                    
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}
