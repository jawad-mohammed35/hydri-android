// Copyright 2026 Jawad Mohammed
// SPDX-License-Identifier: Apache-2.0

package com.hydri.app

import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

class LocalizationResourceTest {
    private val root =
        File("src/main/res").let { if (it.exists()) it else File("app/src/main/res") }
    private val folders =
        listOf(
            "values",
            "values-es",
            "values-fr",
            "values-b+zh+Hans",
            "values-ko",
            "values-ja",
            "values-in",
        )

    private fun elements(file: File): List<Element> {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
        val nodes = factory.newDocumentBuilder().parse(file).documentElement.childNodes
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    private fun key(e: Element) = "${e.tagName}/${e.getAttribute("name")}"

    @Test
    fun allSupportedLanguagesHaveEveryTranslatableResource() {
        val expected =
            elements(File(root, "values/strings.xml"))
                .filter { it.getAttribute("translatable") != "false" }
                .map(::key)
                .toSet()
        folders.forEach { folder ->
            val entries = elements(File(root, "$folder/strings.xml"))
            assertEquals("Duplicate keys in $folder", entries.size, entries.map(::key).toSet().size)
            assertTrue(
                "Missing translations in $folder: ${expected - entries.map(::key).toSet()}",
                entries.map(::key).containsAll(expected),
            )
            assertTrue(entries.none { it.textContent.contains('\uFFFD') })
        }
    }

    @Test
    fun placeholdersMatchAndFormatInEveryLanguage() {
        val original = elements(File(root, "values/strings.xml")).associateBy(::key)
        val pattern = Regex("%(\\d+)\\\$([sdf])")
        folders.forEach { folder ->
            elements(File(root, "$folder/strings.xml")).forEach { translated ->
                val source = original.getValue(key(translated))
                fun signature(e: Element) =
                    pattern.findAll(e.textContent).map { it.value }.sorted().toList()
                if (translated.tagName == "plurals") {
                    val sourceOther =
                        (0 until source.getElementsByTagName("item").length)
                            .map { source.getElementsByTagName("item").item(it) as Element }
                            .first { it.getAttribute("quantity") == "other" }
                    val items = translated.getElementsByTagName("item")
                    (0 until items.length).forEach { index ->
                        assertEquals(
                            "Plural format differs: $folder ${key(translated)}",
                            signature(sourceOther),
                            signature(items.item(index) as Element),
                        )
                    }
                } else
                    assertEquals(
                        "Format arguments differ: $folder ${key(translated)}",
                        signature(source),
                        signature(translated),
                    )
                val texts =
                    if (translated.tagName == "plurals") {
                        val items = translated.getElementsByTagName("item")
                        (0 until items.length).map { items.item(it).textContent }
                    } else listOf(translated.textContent)
                texts.forEach { text ->
                    if (pattern.containsMatchIn(text))
                        String.format(Locale.ENGLISH, text, *Array<Any>(8) { "1" })
                }
            }
        }
    }

    @Test
    fun localeConfigAdvertisesExactlyTheBundledLanguages() {
        val entries = elements(File(root, "xml/locales_config.xml"))
        assertEquals(
            setOf("en", "es", "fr", "zh-Hans", "ko", "ja", "id"),
            entries.map { it.getAttribute("android:name") }.toSet(),
        )
    }
}
