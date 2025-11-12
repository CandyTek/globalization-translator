# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GlobalizationTranslator is an IntelliJ Platform plugin that automatically translates internationalization (i18n) files into 131 languages using Google Translate API. It supports:
- Android `strings.xml` files (with `string`, `string-array`, and `plurals` tags)
- Java `.properties` files

The plugin preserves placeholders (`%1$s`, `%2$d`, `{0}`, `{1}`) and escape characters (`\n`, `\"`, `\'`) during translation.

**Key Features:**
- Batch translation to 131 languages in one operation
- Incremental translation (merges with existing translations)
- Preserves formatting, placeholders, and escape sequences
- Background task execution with progress indicators
- Compose Desktop UI for modern dialog interfaces

## Quick Reference

### For IntelliJ 2025.2+ Development

**Essential Configuration:**
- Kotlin: 2.1.0, Compose: 1.7.1, Java: 17
- `sinceBuild: 252`
- Use `invokeAndWait` (not `invokeLater`) for file operations
- Resource loading via `javaPainterResource()` helper

**Common Commands:**
```bash
# Build plugin for manual installation
./gradlew buildPlugin    # Output: build/distributions/*.zip

# Build with tests skipped (recommended for 2025.2+)
./gradlew build -x test

# Clean build
./gradlew clean buildPlugin -x test
```

**Critical Files for 2025.2+ Compatibility:**
- `gradle.properties`: Kotlin/Compose versions
- `build.gradle.kts`: Kotlin language/API versions, sinceBuild
- `AndroidXmlTranslator.kt:55`: Memory fix (`invokeAndWait`)
- `PropertiesTranslator.kt:50`: Memory fix (`invokeAndWait`)
- `JarPainterResource.kt`: Resource loading fix

**Troubleshooting Quick Links:**
- Build fails: See [Troubleshooting → Build Issues](#build-issues)
- IDE freezes: See [Runtime Issues → OutOfMemoryError](#ide-freezescrashes-with-outofmemoryerror-during-translation)
- runIde fails: See ["Index: 1, Size: 1" error](#index-1-size-1-when-running-gradlew-runide)

## Build Commands

### Build the plugin
```bash
./gradlew build
```

### Build plugin distribution (for manual installation)
```bash
./gradlew buildPlugin
```
Output: `build/distributions/GlobalizationTranslator-{version}.zip`

### Run the plugin in a test IDE instance
```bash
./gradlew runIde
```
**Note:** Due to gradle-intellij-plugin compatibility issues with IntelliJ 2025.2+, this command may fail with "Index: 1, Size: 1" error. Use manual installation instead.

### Clean build artifacts
```bash
./gradlew clean
```

### Run tests
```bash
./gradlew test
```

**Note:** Some tests require network connectivity and are marked with `@Ignore` annotations. They test translation APIs that depend on external services.

## Build Configuration

### Current Configuration (IntelliJ 2025.2+ Compatible)

- **Target Platform:** IntelliJ IDEA 2025.2+ (`sinceBuild: 252`)
- **Java Compatibility:** Java 17
  - Source compatibility: 17
  - Target compatibility: 17
- **Kotlin Version:** 2.1.0
  - Language version: 2.1
  - API version: 2.1
  - JVM target: 17
- **Compose Version:** 1.7.1
- **Gradle IntelliJ Plugin:** 1.17.3
- **Required Dependencies:**
  - Compose Desktop (ui, foundation, material, runtime)
  - OkHttp 4.10.0 (HTTP client for translation API)
  - Gson 2.10 (JSON parsing)
  - dom4j (XML parsing for Android resources)
  - Custom Properties library (codejive/java-properties:1.0.7)

### Version Compatibility Matrix

| IntelliJ Version | Kotlin | Compose | Since Build | Notes |
|-----------------|---------|---------|-------------|-------|
| 2025.2+         | 2.1.0   | 1.7.1   | 252         | Current configuration |
| 2024.2-2025.1   | 2.0.21  | 1.6.11  | 242         | Previous configuration |
| 2021.3-2024.1   | 1.9.x   | 1.5.x   | 213         | Legacy support |

### Java Home Configuration

The project requires Java 17. Configure in `gradle.properties`:
```properties
org.gradle.java.home=D:\\android-studio\\jbr
```
**Important:** Avoid paths with spaces (e.g., "C:\\Program Files\\...") as they cause issues with gradle-intellij-plugin's path resolution.

## Architecture

### Core Translation Flow

The plugin follows a pipeline architecture for translation:

1. **Actions** (`action/android/` and `action/properties/`)
   - Entry points triggered by user context menu actions
   - `TranslateAction`: Translates entire files
   - `TranslateSelectedAction`: Translates selected text ranges

2. **Tasks** (`task/`)
   - Background tasks that run the translation process with progress indicators
   - `AndroidTranslateTask`: Handles Android XML translation
   - `PropertiesTranslateTask`: Handles .properties file translation

3. **Translators** (`translator/`)
   - **`AndroidXmlTranslator`**: Parses Android XML using dom4j, extracts translatable strings (respecting `translatable="false"` attribute), translates them, and generates new language-specific XML files in `values-{locale}` directories
   - **`PropertiesTranslator`**: Parses .properties files using custom Properties library, translates key-value pairs, and generates new locale-specific .properties files
   - Both translators support incremental translation (merging with existing translated files)

4. **Translation Engine** (`translator/engine/`)
   - **`Translator` interface**: Defines the contract for translation engines
   - **`GoogleTranslator`**: Implements translation using Google Translate's internal API endpoint
     - Endpoint: `https://translate.googleapis.com/translate_a/t`
     - Method: POST with form data
     - Parameters: `sl` (source lang), `tl` (target lang), `tk` (token), `q` (queries)
     - Token generation: Custom algorithm in `token.kt` (based on TKK value)
     - Response: JSON array of translated strings
   - **Important:** Uses unofficial API (no API key required but may break without notice)

### Key Design Patterns

**Placeholder Preservation:**
- Android: Uses `addCodeTag()` to wrap placeholders in HTML `<code>` tags before translation, then `removeCodeTag()` after
- Properties: Same approach with HTML code tags
- Escape handling: Strings are escaped to HTML entities before translation, then unescaped after

**Incremental Translation:**
- `newDocumentFetcher` / `newPropertiesFetcher` callbacks allow merging translations with existing target files
- Only untranslated or updated strings are overwritten when `isOverwriteTargetFile` is false

**File Operations:**
- All IntelliJ VFS operations (file creation, writing) are wrapped in `ApplicationManager.getApplication().runWriteAction {}`
- Ensures thread-safety and proper IDE integration

**Memory Management and Threading (CRITICAL):**
- **Problem:** Translating 131 languages creates 131 file write operations
- **Solution:** Use `invokeAndWait` instead of `invokeLater` to prevent event queue buildup
- **Why:** IntelliJ 2025.2+ has stricter event queue management; async tasks accumulate and cause:
  - Memory exhaustion (OOM errors even with 8GB heap)
  - IDE freeze (event dispatcher thread overload)
  - Translation failures (tasks cancelled before completion)
- **Implementation:** Both `AndroidXmlTranslator` and `PropertiesTranslator` use:
  ```kotlin
  applicationManager.invokeAndWait {  // Synchronous wait
      applicationManager.runWriteAction { /* file write */ }
  }
  ```
- This ensures each translation completes and writes before starting the next, preventing memory buildup

**UI Resource Loading:**
- **Compose 1.7.1 Breaking Change:** `ResourceLoader` interface removed
- **Old approach (deprecated):**
  ```kotlin
  painterResource(resourcePath, customResourceLoader)
  ```
- **New approach (required):**
  ```kotlin
  // Must manually load from JAR classpath
  val stream = javaClass.classLoader.getResourceAsStream(resourcePath)
  loadSvgPainter(stream, Density(1f))
  ```
- **Location:** `ui/util/JarPainterResource.kt` provides `javaPainterResource()` helper
- **Why needed:** IntelliJ plugins run from JARs; standard Compose resource loading expects filesystem access

### Language Mapping

`language/LanguageUtil.kt` contains:
- `androidLanguageDirMap`: Maps language codes to Android resource directory names (e.g., `"zh-CN" -> "values-zh-rCN"`)
- `languages`: List of all 131 supported language codes

## Project Structure

```
src/main/kotlin/com/wilinz/globalization/translator/
├── action/                          # User-triggered actions
│   ├── android/                     # Android strings.xml actions
│   │   ├── TranslateAction.kt       # Translate entire file
│   │   └── TranslateSelectedAction.kt  # Translate selection
│   └── properties/                  # Java .properties actions
│       ├── TranslateAction.kt
│       └── TranslateSelectedAction.kt
├── task/                            # Background tasks with progress
│   ├── AndroidTranslateTask.kt      # Android translation task
│   ├── PropertiesTranslateTask.kt   # Properties translation task
│   └── TranslateTask.kt             # Base task class
├── translator/                      # Core translation logic
│   ├── AndroidXmlTranslator.kt      # Android XML handler
│   ├── PropertiesTranslator.kt      # Properties file handler
│   └── engine/                      # Translation engines
│       ├── Translator.kt            # Interface
│       ├── GoogleTranslator.kt      # Google Translate impl
│       └── token.kt                 # Token generation for API
├── ui/                              # User interface
│   ├── dialog/                      # Compose dialogs
│   │   ├── TranslateDialog.kt       # Base dialog
│   │   ├── AndroidTranslateDialog.kt
│   │   └── PropertiesTranslateDialog.kt
│   ├── theme/                       # UI theming
│   │   ├── IntellijTheme.kt         # IDE theme integration
│   │   └── WidgetTheme.kt
│   ├── widgets/                     # Reusable components
│   │   └── CheckboxWithLabel.kt
│   └── util/                        # UI utilities
│       └── JarPainterResource.kt    # Resource loader for JAR
├── language/                        # Language configuration
│   └── LanguageUtil.kt              # Language codes & mappings
├── network/                         # HTTP client
│   └── OkHttp.kt                    # Configured OkHttp instance
├── util/                            # Utilities
│   ├── AndroidUtil.kt               # Android XML helpers
│   ├── PropertiesUtil.kt            # Properties file helpers
│   └── [various extensions]
└── i18n/                            # Plugin localization
    └── message.kt                   # i18n message keys

src/main/resources/
├── META-INF/
│   └── plugin.xml                   # Plugin configuration
└── icons/                           # UI icons (SVG)
    └── more.svg
```

### Key Files to Know

| File | Purpose | Critical Notes |
|------|---------|----------------|
| `AndroidXmlTranslator.kt` | Android XML translation | Uses `invokeAndWait` at line 55 (memory fix) |
| `PropertiesTranslator.kt` | Properties translation | Uses `invokeAndWait` at line 50 (memory fix) |
| `GoogleTranslator.kt` | Translation API client | Unofficial API, may break |
| `JarPainterResource.kt` | Load resources from JAR | Required for Compose 1.7.1+ |
| `TranslateDialog.kt` | Base UI dialog | Compose Desktop implementation |
| `plugin.xml` | Plugin manifest | Defines actions, notifications, dependencies |
| `token.kt` | Google API tokens | Token generation algorithm |
| `LanguageUtil.kt` | Language mappings | All 131 languages defined here |

## Plugin Registration

Actions are registered in `src/main/resources/META-INF/plugin.xml`:
- Project view context menu actions for translating entire files
- Editor context menu actions for translating selected text
- Two notification groups for user feedback

## Testing Considerations

When writing tests:
- Tests that require network access to Google Translate should be marked with `@org.junit.Ignore`
- Mock the `Translator` interface for unit testing translation logic without network calls
- Use `SAXReader` (dom4j) for parsing Android XML in tests
- Use the custom `org.codejive.properties.Properties` library (not `java.util.Properties`) for .properties file handling to preserve formatting and comments

## Common Modifications

### Adding a new translation engine
1. Implement the `Translator` interface in `translator/engine/`
2. Update UI dialogs to allow selection of translation engine
3. Update both `AndroidXmlTranslator` and `PropertiesTranslator` to accept the new translator

### Supporting additional file formats
1. Create a new translator in `translator/` following the pattern of `AndroidXmlTranslator` or `PropertiesTranslator`
2. Create corresponding actions in `action/`
3. Create a background task in `task/`
4. Register new actions in `plugin.xml`

### Modifying language support
- Update `LanguageUtil.kt` to add/remove languages
- Ensure Android directory name mappings are correct for new locales

## Troubleshooting

### Build Issues

#### "Kotlin metadata version 2.2.0 incompatible with 2.0.0"
**Cause:** IntelliJ 2025.2+ compiled with Kotlin 2.2, but plugin uses older Kotlin
**Solution:** Upgrade to Kotlin 2.1.0+ and Compose 1.7.1+
```properties
# gradle.properties
kotlin.version=2.1.0
compose.version=1.7.1
```
```kotlin
// build.gradle.kts
languageVersion.set(KotlinVersion.KOTLIN_2_1)
apiVersion.set(KotlinVersion.KOTLIN_2_1)
```

#### "Index: 1, Size: 1" when running `./gradlew runIde`
**Cause:** gradle-intellij-plugin 1.17.3 bug with path resolution (especially paths with spaces)
**Solutions:**
1. Use manual installation: `./gradlew buildPlugin` then install ZIP from IDE
2. Ensure `gradle.properties` uses path without spaces:
   ```properties
   org.gradle.java.home=D:\\android-studio\\jbr  # Good
   # NOT: C:\\Program Files\\Android\\... (has spaces)
   ```

#### "Resource icons/more.svg not found"
**Cause:** Compose 1.7.1 removed `ResourceLoader` API
**Solution:** Already fixed in `ui/util/JarPainterResource.kt` using manual classloader loading
```kotlin
val stream = javaClass.classLoader.getResourceAsStream(resourcePath)
loadSvgPainter(stream, Density(1f))
```

### Runtime Issues

#### IDE freezes/crashes with OutOfMemoryError during translation
**Cause:** 131 async write tasks queued with `invokeLater` accumulate in event queue
**Solution:** Already fixed - both translators use `invokeAndWait` for synchronous execution
**Files affected:**
- `translator/AndroidXmlTranslator.kt:55`
- `translator/PropertiesTranslator.kt:50`

#### Translation completes but files not created
**Cause:** Write actions must run on EDT (Event Dispatch Thread)
**Solution:** Ensure all file writes are wrapped:
```kotlin
ApplicationManager.getApplication().invokeAndWait {
    ApplicationManager.getApplication().runWriteAction {
        // VFS operations here
    }
}
```

#### Network errors during translation
**Cause:** Google Translate API rate limiting or network issues
**Solution:**
- Reduce batch size (translate fewer languages at once)
- Add retry logic in `GoogleTranslator.kt`
- Check network connectivity

### Testing Issues

#### Tests fail with "Index: 1, Size: 1"
**Cause:** Test framework incompatibility with IntelliJ 2025.2+
**Workaround:** Skip tests during build: `./gradlew build -x test`

## Known Issues and Limitations

### IntelliJ Platform Compatibility
- **2025.2+:** Fully supported with current configuration (Kotlin 2.1.0, Compose 1.7.1)
- **2024.2-2025.1:** Requires Kotlin 2.0.21, Compose 1.6.11, sinceBuild: 242
- **< 2024.2:** May require further downgrades

### gradle-intellij-plugin Issues
- `runIde` task fails on 2025.2+ with path resolution errors
- `buildSearchableOptions` task disabled due to compatibility issues
- Workaround: Use `buildPlugin` and manual installation

### Performance Considerations
- Translating all 131 languages takes significant time (network-bound)
- Each language requires separate API call to Google Translate
- File writes are now synchronous (slower but memory-safe)
- Recommended: Allow users to select subset of languages

### API Limitations
- Google Translate API uses unofficial endpoint (may break)
- No official API key support
- Rate limiting may occur with heavy usage
- Token generation algorithm in `token.kt` may need updates if Google changes

## Development Best Practices

### When Adding New Features

1. **UI Components:** Use Compose Desktop APIs compatible with 1.7.1+
2. **Resource Loading:** Always use `javaPainterResource()` helper for icons
3. **File Operations:** Always wrap in `invokeAndWait` + `runWriteAction`
4. **Threading:** Avoid `invokeLater` for batch operations (use `invokeAndWait`)
5. **Testing:** Mark network-dependent tests with `@Ignore`

### When Upgrading Dependencies

1. **IntelliJ Version Changes:**
   - Update `sinceBuild` in `build.gradle.kts`
   - Check Kotlin version requirements (see Platform Versions docs)
   - Test resource loading and file operations

2. **Kotlin Version Changes:**
   - Update `kotlin.version` in `gradle.properties`
   - Update `languageVersion` and `apiVersion` in `build.gradle.kts`
   - Check for deprecated API usage

3. **Compose Version Changes:**
   - Review resource loading APIs (breaking changes common)
   - Test UI rendering in different IDE themes
   - Check `javaPainterResource.kt` for compatibility

### Debugging Tips

1. **Enable debug logging:**
   ```kotlin
   printlnDebug(message)  // Conditional logging in debug mode
   ```

2. **Check IDE logs:**
   - Help → Show Log in Explorer/Finder
   - Look for stack traces related to plugin

3. **Memory profiling:**
   - Use VisualVM or YourKit to monitor heap usage
   - Watch for event queue buildup in thread dumps

4. **Network debugging:**
   - Enable OkHttp logging interceptor
   - Monitor requests in `GoogleTranslator.kt`

## Migration Guide

### From 2024.x to 2025.2+

1. Update `gradle.properties`:
   ```diff
   - kotlin.version=2.0.21
   + kotlin.version=2.1.0
   - compose.version=1.6.11
   + compose.version=1.7.1
   ```

2. Update `build.gradle.kts`:
   ```diff
   - sinceBuild.set("242")
   + sinceBuild.set("252")
   - languageVersion.set(KotlinVersion.KOTLIN_2_0)
   + languageVersion.set(KotlinVersion.KOTLIN_2_1)
   ```

3. Update resource loading (if customized):
   ```diff
   - painterResource(path, CustomResourceLoader)
   + javaPainterResource(path)
   ```

4. Verify memory management:
   - Ensure `invokeAndWait` used (not `invokeLater`)
   - Test translation with multiple languages

5. Test build:
   ```bash
   ./gradlew clean buildPlugin -x test
   ```

6. Manual installation test:
   - Install from `build/distributions/*.zip`
   - Test translation workflow
   - Monitor IDE performance
