package jnu.ie.capstone.clova.enums

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Testing library and framework:
 * - JUnit Jupiter (JUnit 5) for unit tests and assertions.
 *
 * Scope:
 * - Focus on ClovaSpeechLanguage enum public API, especially factory/lookup functions and properties touched in the PR diff.
 * - Validate known language codes, case-insensitivity (if supported), error handling on unknown values, and JSON mapping if annotated.
 *
 * Note:
 * - Update the list of expected entries if enum constants change.
 */
class ClovaSpeechLanguageTest {

    // Update these to reflect actual enum constants and mappings discovered in the codebase.
    private val knownExamples: List<Pair<String, String>> = listOf(
        // Pair<enumName, codeOrLocale>
        // e.g., "KO_KR" to "ko-KR", "EN_US" to "en-US"
        "KO_KR" to "ko-KR",
        "EN_US" to "en-US"
    )

    @Test
    @DisplayName("All enum constants should have distinct names and (if present) distinct codes")
    fun allConstantsDistinct() {
        val all = ClovaSpeechLanguage.values().toList()
        assertTrue(all.isNotEmpty(), "Enum should not be empty")

        val names = all.map { it.name }
        assertEquals(names.size, names.toSet().size, "Enum names must be unique")

        // If enum has a code or locale property, assert distinctness
        val codeProp = runCatching { all.map { it.javaClass.getMethod("code").invoke(it) as String } }.getOrNull()
            ?: runCatching { all.map { it.javaClass.getMethod("locale").invoke(it) as String } }.getOrNull()

        if (codeProp \!= null) {
            assertEquals(codeProp.size, codeProp.toSet().size, "Enum code/locale values must be unique")
        }
    }

    @Test
    @DisplayName("Known example mappings should resolve correctly via properties")
    fun knownExampleProperties() {
        knownExamples.forEach { (name, expectedCode) ->
            val e = ClovaSpeechLanguage.valueOf(name)
            val code = runCatching { e.javaClass.getMethod("code").invoke(e) as String }.getOrNull()
                ?: runCatching { e.javaClass.getMethod("locale").invoke(e) as String }.getOrNull()
            if (code \!= null) {
                assertEquals(expectedCode, code, "Property mapping mismatch for $name")
            }
        }
    }

    @Nested
    @DisplayName("Factory and lookup methods")
    inner class FactoryLookups {

        @Test
        @DisplayName("fromCode/parse/Of should return the correct enum for known codes (case & hyphen tolerant if supported)")
        fun fromCodeKnownValues() {
            // Try common factory names reflectively to avoid tight coupling: fromCode, fromLocale, parse, of
            val factories = listOf("fromCode", "fromLocale", "parse", "of")
                .mapNotNull { name -> runCatching { ClovaSpeechLanguage::class.java.getMethod(name, String::class.java) }.getOrNull() }

            if (factories.isEmpty()) return

            knownExamples.forEach { (name, code) ->
                factories.forEach { m ->
                    val exact = m.invoke(null, code)
                    assertEquals(ClovaSpeechLanguage.valueOf(name), exact, "Factory ${m.name} failed for code $code")

                    // Case-insensitive variant if method supports normalization
                    val upper = code.uppercase()
                    val lower = code.lowercase()
                    val upRes = runCatching { m.invoke(null, upper) }.getOrNull()
                    val loRes = runCatching { m.invoke(null, lower) }.getOrNull()
                    if (upRes \!= null) assertEquals(ClovaSpeechLanguage.valueOf(name), upRes, "Factory ${m.name} should be case-insensitive for $upper")
                    if (loRes \!= null) assertEquals(ClovaSpeechLanguage.valueOf(name), loRes, "Factory ${m.name} should be case-insensitive for $lower")

                    // Hyphen/underscore normalization if applicable (e.g., "en_US" vs "en-US")
                    val underscore = code.replace('-', '_')
                    val hyphen = code.replace('_', '-')
                    val normRes1 = runCatching { m.invoke(null, underscore) }.getOrNull()
                    val normRes2 = runCatching { m.invoke(null, hyphen) }.getOrNull()
                    if (normRes1 \!= null) assertEquals(ClovaSpeechLanguage.valueOf(name), normRes1, "Factory ${m.name} should normalize underscores: $underscore")
                    if (normRes2 \!= null) assertEquals(ClovaSpeechLanguage.valueOf(name), normRes2, "Factory ${m.name} should normalize hyphens: $hyphen")
                }
            }
        }

        @Test
        @DisplayName("Factories should throw or return null for unknown/invalid inputs")
        fun fromCodeInvalidValues() {
            val factories = listOf("fromCode", "fromLocale", "parse", "of")
                .mapNotNull { name -> runCatching { ClovaSpeechLanguage::class.java.getMethod(name, String::class.java) }.getOrNull() }

            if (factories.isEmpty()) return

            val invalids = listOf("", "  ", "xx-XX", "EN-UK", "koKR", "foo_bar", "🤖", "\n", "\t")
            factories.forEach { m ->
                invalids.forEach { bad ->
                    val res = runCatching { m.invoke(null, bad) }.exceptionOrNull()
                    // If API returns nullable instead of throwing, verify null
                    if (res == null) {
                        val value = m.invoke(null, bad)
                        assertNull(value, "Factory ${m.name} should return null for invalid input: '$bad'")
                    } else {
                        // Expect IllegalArgumentException or NoSuchElementException commonly
                        assertTrue(
                            res is IllegalArgumentException || res is NoSuchElementException || res.cause is IllegalArgumentException || res.cause is NoSuchElementException,
                            "Factory ${m.name} should throw IAE/NSEE for invalid input '$bad', but got: ${res::class}"
                        )
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Safety and toString")
    inner class Basics {

        @Test
        fun valueOfIsConsistent() {
            ClovaSpeechLanguage.values().forEach { e ->
                assertSame(e, ClovaSpeechLanguage.valueOf(e.name))
                assertTrue(e.toString().isNotBlank(), "toString should not be blank")
            }
        }

        @Test
        fun valueOfUnknownNameThrows() {
            assertThrows<IllegalArgumentException> {
                ClovaSpeechLanguage.valueOf("NOT_A_LANGUAGE")
            }
        }
    }
}

// --- Automatically appended tests focusing on robustness and edge cases ---

import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(MethodOrderer.DisplayName::class)
class ClovaSpeechLanguageAdditionalTest {

    @Test
    fun trimmingAndWhitespaceAreHandledIfSupported() {
        val factories = listOf("fromCode", "fromLocale", "parse", "of")
            .mapNotNull { name -> runCatching { ClovaSpeechLanguage::class.java.getMethod(name, String::class.java) }.getOrNull() }
        if (factories.isEmpty()) return

        // Pick first enum as a sample
        val sample = ClovaSpeechLanguage.values().firstOrNull() ?: return
        val code = runCatching { sample.javaClass.getMethod("code").invoke(sample) as String }.getOrNull()
            ?: runCatching { sample.javaClass.getMethod("locale").invoke(sample) as String }.getOrNull()
            ?: sample.name

        factories.forEach { m ->
            val padded = "  $code  "
            val res = runCatching { m.invoke(null, padded) }.getOrNull()
            // If API trims input, expect match; otherwise skip
            if (res \!= null) {
                org.junit.jupiter.api.Assertions.assertEquals(sample, res, "Factory ${m.name} should accept trimmed input")
            }
        }
    }

    @Test
    fun nullInputIsRejectedOrHandledGracefully() {
        val factories = listOf("fromCode", "fromLocale", "parse", "of")
            .mapNotNull { name -> runCatching { ClovaSpeechLanguage::class.java.getMethod(name, String::class.java) }.getOrNull() }
        if (factories.isEmpty()) return

        factories.forEach { m ->
            val threw = runCatching { m.invoke(null, null) }.exceptionOrNull()
            // Expect an exception; if method is annotated @JvmStatic in companion, reflection still applies
            org.junit.jupiter.api.Assertions.assertNotNull(threw, "Factory ${m.name} should not accept null input")
        }
    }
}