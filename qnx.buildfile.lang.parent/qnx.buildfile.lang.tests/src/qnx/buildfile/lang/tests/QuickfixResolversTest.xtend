package qnx.buildfile.lang.tests

import org.junit.jupiter.api.Test
import qnx.buildfile.lang.ide.BuildfileDSLQuickfixResolvers

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for {@link BuildfileDSLQuickfixResolvers} — verifies the shared
 * quickfix logic used by both Eclipse and VSCode/LSP quickfix providers.
 */
class QuickfixResolversTest {

	// ── extractBadNameFromMessage ──────────────────────────────────

	@Test
	def void extractNameFromValuedAttributeMessage() {
		val name = BuildfileDSLQuickfixResolvers.extractBadNameFromMessage(
			'Unknown ValuedAttribute "permjs"')
		assertEquals("permjs", name)
	}

	@Test
	def void extractNameFromBooleanAttributeMessage() {
		val name = BuildfileDSLQuickfixResolvers.extractBadNameFromMessage(
			'Unknown BooleanAttribute "optonal')
		assertEquals("optonal", name)
	}

	@Test
	def void extractNameReturnsNullForNoQuotes() {
		assertNull(BuildfileDSLQuickfixResolvers.extractBadNameFromMessage("No quotes here"))
	}

	@Test
	def void extractNameReturnsNullForNull() {
		assertNull(BuildfileDSLQuickfixResolvers.extractBadNameFromMessage(null))
	}

	// ── suggestAttributeNames ─────────────────────────────────────

	@Test
	def void suggestsPermsForPermjs() {
		val suggestions = BuildfileDSLQuickfixResolvers.suggestAttributeNames("permjs")
		assertFalse(suggestions.isEmpty, "Should suggest something for 'permjs'")
		assertTrue(suggestions.stream.anyMatch[keyword == "perms"],
			'''Should suggest 'perms' but got: «suggestions.map[keyword].join(", ")»''')
	}

	@Test
	def void suggestsOptionalForOptonal() {
		val suggestions = BuildfileDSLQuickfixResolvers.suggestAttributeNames("optonal")
		assertFalse(suggestions.isEmpty, "Should suggest something for 'optonal'")
		assertTrue(suggestions.stream.anyMatch[keyword == "optional"],
			'''Should suggest 'optional' but got: «suggestions.map[keyword].join(", ")»''')
	}

	@Test
	def void suggestsUidForUdi() {
		val suggestions = BuildfileDSLQuickfixResolvers.suggestAttributeNames("udi")
		assertTrue(suggestions.stream.anyMatch[keyword == "uid"],
			'''Should suggest 'uid' but got: «suggestions.map[keyword].join(", ")»''')
	}

	@Test
	def void suggestsGidForGdi() {
		val suggestions = BuildfileDSLQuickfixResolvers.suggestAttributeNames("gdi")
		assertTrue(suggestions.stream.anyMatch[keyword == "gid"],
			'''Should suggest 'gid' but got: «suggestions.map[keyword].join(", ")»''')
	}

	@Test
	def void noSuggestionsForCompletelyUnrelated() {
		val suggestions = BuildfileDSLQuickfixResolvers.suggestAttributeNames("xxxxxxxxxxx")
		assertTrue(suggestions.isEmpty, "Should not suggest anything for completely unrelated string")
	}

	@Test
	def void noSuggestionsForNull() {
		val suggestions = BuildfileDSLQuickfixResolvers.suggestAttributeNames(null)
		assertTrue(suggestions.isEmpty)
	}

	@Test
	def void noSuggestionsForEmpty() {
		val suggestions = BuildfileDSLQuickfixResolvers.suggestAttributeNames("")
		assertTrue(suggestions.isEmpty)
	}

	@Test
	def void suggestionsAreSortedByDistance() {
		val suggestions = BuildfileDSLQuickfixResolvers.suggestAttributeNames("compres")
		if (suggestions.size > 1) {
			for (var i = 1; i < suggestions.size; i++) {
				assertTrue(suggestions.get(i).distance >= suggestions.get(i - 1).distance,
					"Suggestions should be sorted by Levenshtein distance")
			}
		}
	}

	@Test
	def void atMostThreeSuggestions() {
		val suggestions = BuildfileDSLQuickfixResolvers.suggestAttributeNames("a")
		assertTrue(suggestions.size <= 3, "Should return at most 3 suggestions")
	}

	@Test
	def void caseInsensitiveMatching() {
		val suggestions = BuildfileDSLQuickfixResolvers.suggestAttributeNames("PERM")
		assertTrue(suggestions.stream.anyMatch[keyword == "perms"],
			'''Should match case-insensitively but got: «suggestions.map[keyword].join(", ")»''')
	}

	// ── Levenshtein distance ──────────────────────────────────────

	@Test
	def void levenshteinIdenticalStrings() {
		assertEquals(0, BuildfileDSLQuickfixResolvers.levenshteinDistance("perms", "perms"))
	}

	@Test
	def void levenshteinSingleInsertion() {
		assertEquals(1, BuildfileDSLQuickfixResolvers.levenshteinDistance("perm", "perms"))
	}

	@Test
	def void levenshteinSingleDeletion() {
		assertEquals(1, BuildfileDSLQuickfixResolvers.levenshteinDistance("perms", "perm"))
	}

	@Test
	def void levenshteinSingleSubstitution() {
		assertEquals(1, BuildfileDSLQuickfixResolvers.levenshteinDistance("perms", "perks"))
	}

	@Test
	def void levenshteinTransposition() {
		assertEquals(2, BuildfileDSLQuickfixResolvers.levenshteinDistance("prems", "perms"))
	}

	@Test
	def void levenshteinEmptyStrings() {
		assertEquals(0, BuildfileDSLQuickfixResolvers.levenshteinDistance("", ""))
		assertEquals(5, BuildfileDSLQuickfixResolvers.levenshteinDistance("perms", ""))
		assertEquals(5, BuildfileDSLQuickfixResolvers.levenshteinDistance("", "perms"))
	}
}
