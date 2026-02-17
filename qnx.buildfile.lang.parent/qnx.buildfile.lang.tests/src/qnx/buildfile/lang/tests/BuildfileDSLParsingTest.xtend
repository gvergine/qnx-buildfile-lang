package qnx.buildfile.lang.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import qnx.buildfile.lang.buildfileDSL.AttributeStatement
import qnx.buildfile.lang.buildfileDSL.BooleanAttribute
import qnx.buildfile.lang.buildfileDSL.ContentBlock
import qnx.buildfile.lang.buildfileDSL.DeploymentStatement
import qnx.buildfile.lang.buildfileDSL.Model
import qnx.buildfile.lang.buildfileDSL.Path
import qnx.buildfile.lang.buildfileDSL.ValuedAttribute

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for parsing buildfile syntax into the expected AST structure.
 * Covers valid syntax, edge cases, and malformed input.
 */
@ExtendWith(InjectionExtension)
@InjectWith(BuildfileDSLInjectorProvider)
class BuildfileDSLParsingTest {
	@Inject ParseHelper<Model> parseHelper
	@Inject ValidationTestHelper validationHelper

	// ── Valid parsing ─────────────────────────────────────────────

	@Test
	def void parseEmptyFile() {
		// An empty file should not parse into a model (grammar requires at least one statement)
		// or parse with errors. Let's verify what happens.
		val result = parseHelper.parse("")
		// Empty input may yield null or a model with no statements depending on Xtext version
		if (result !== null) {
			assertTrue(result.eResource.errors.size > 0 || result.statements.isEmpty,
				"Empty input should either fail to parse or produce empty model")
		}
	}

	@Test
	def void parseCommentOnlyFile() {
		val result = parseHelper.parse('''
			# this is a comment
		''')
		// Comments only — no statements expected; may have parse errors since grammar requires statements
		if (result !== null && result.eResource.errors.isEmpty) {
			assertTrue(result.statements.isEmpty, "Comment-only file should have no statements")
		}
	}

	@Test
	def void parseSingleBooleanAttributeStatement() {
		val result = parseHelper.parse('''
			[+optional]
		''')
		assertNotNull(result)
		assertTrue(result.eResource.errors.isEmpty, '''Unexpected errors: «result.eResource.errors.join(", ")»''')
		assertEquals(1, result.statements.size)

		val stmt = result.statements.get(0)
		assertTrue(stmt instanceof AttributeStatement)
		val attrStmt = stmt as AttributeStatement
		assertEquals(1, attrStmt.attributesection.attributes.size)

		val attr = attrStmt.attributesection.attributes.get(0)
		assertTrue(attr instanceof BooleanAttribute)
		val boolAttr = attr as BooleanAttribute
		assertTrue(boolAttr.isEnabled)
		assertEquals("optional", boolAttr.name)
	}

	@Test
	def void parseNegatedBooleanAttribute() {
		val result = parseHelper.parse('''
			[-optional]
		''')
		assertNotNull(result)
		assertTrue(result.eResource.errors.isEmpty, '''Unexpected errors: «result.eResource.errors.join(", ")»''')

		val attr = (result.statements.get(0) as AttributeStatement)
			.attributesection.attributes.get(0) as BooleanAttribute
		assertFalse(attr.isEnabled, "Minus prefix should set enabled=false")
		assertEquals("optional", attr.name)
	}

	@Test
	def void parseSingleValuedAttributeStatement() {
		val result = parseHelper.parse('''
			[uid=0]
		''')
		assertNotNull(result)
		assertTrue(result.eResource.errors.isEmpty, '''Unexpected errors: «result.eResource.errors.join(", ")»''')

		val attr = (result.statements.get(0) as AttributeStatement)
			.attributesection.attributes.get(0) as ValuedAttribute
		assertEquals("uid", attr.name)
		assertEquals("0", attr.value)
	}

	@Test
	def void parseMultipleAttributesInOneSection() {
		val result = parseHelper.parse('''
			[uid=0 gid=0 perms=0555]
		''')
		assertNotNull(result)
		assertTrue(result.eResource.errors.isEmpty, '''Unexpected errors: «result.eResource.errors.join(", ")»''')

		val attrs = (result.statements.get(0) as AttributeStatement).attributesection.attributes
		assertEquals(3, attrs.size)
		assertEquals("uid", (attrs.get(0) as ValuedAttribute).name)
		assertEquals("gid", (attrs.get(1) as ValuedAttribute).name)
		assertEquals("perms", (attrs.get(2) as ValuedAttribute).name)
	}

	@Test
	def void parseMixedBooleanAndValuedAttributes() {
		val result = parseHelper.parse('''
			[+optional uid=0 -compress gid=0]
		''')
		assertNotNull(result)
		assertTrue(result.eResource.errors.isEmpty, '''Unexpected errors: «result.eResource.errors.join(", ")»''')

		val attrs = (result.statements.get(0) as AttributeStatement).attributesection.attributes
		assertEquals(4, attrs.size)
		assertTrue(attrs.get(0) instanceof BooleanAttribute)
		assertTrue(attrs.get(1) instanceof ValuedAttribute)
		assertTrue(attrs.get(2) instanceof BooleanAttribute)
		assertTrue(attrs.get(3) instanceof ValuedAttribute)
	}

	@Test
	def void parseSimpleDeploymentStatement() {
		val result = parseHelper.parse('''
			bin/myapp=aarch64le/bin/myapp
		''')
		assertNotNull(result)
		assertTrue(result.eResource.errors.isEmpty, '''Unexpected errors: «result.eResource.errors.join(", ")»''')

		val stmt = result.statements.get(0) as DeploymentStatement
		assertEquals("bin/myapp", stmt.path)
		assertTrue(stmt.isAssignment)
		assertNotNull(stmt.content)
		assertTrue(stmt.content instanceof Path)
		assertEquals("aarch64le/bin/myapp", (stmt.content as Path).value)
	}

	@Test
	def void parseDeploymentWithAttributes() {
		val result = parseHelper.parse('''
			[uid=0 gid=0 perms=0555] bin/myapp=aarch64le/bin/myapp
		''')
		assertNotNull(result)
		assertTrue(result.eResource.errors.isEmpty, '''Unexpected errors: «result.eResource.errors.join(", ")»''')

		val stmt = result.statements.get(0) as DeploymentStatement
		assertNotNull(stmt.attributesection)
		assertEquals(3, stmt.attributesection.attributes.size)
		assertEquals("bin/myapp", stmt.path)
		assertTrue(stmt.isAssignment)
	}

	@Test
	def void parseDeploymentWithInlineContent() {
		val result = parseHelper.parse(
			"[uid=0 gid=0 perms=0400] /etc/myfile.txt={\n\tsome inline content\n}"
		)
		assertNotNull(result)
		assertTrue(result.eResource.errors.isEmpty, '''Unexpected errors: «result.eResource.errors.join(", ")»''')

		val stmt = result.statements.get(0) as DeploymentStatement
		assertEquals("/etc/myfile.txt", stmt.path)
		assertTrue(stmt.isAssignment)
		assertNotNull(stmt.content)
		assertTrue(stmt.content instanceof ContentBlock)
	}

	@Test
	def void parseDeploymentWithoutContent() {
		val result = parseHelper.parse('''
			/proc/boot/some-binary
		''')
		assertNotNull(result)
		assertTrue(result.eResource.errors.isEmpty, '''Unexpected errors: «result.eResource.errors.join(", ")»''')

		val stmt = result.statements.get(0) as DeploymentStatement
		assertEquals("/proc/boot/some-binary", stmt.path)
		assertFalse(stmt.isAssignment)
		assertNull(stmt.content)
	}

	@Test
	def void parseVariableReferenceInPath() {
		val result = parseHelper.parse('''
			[search=${MKIFS_PATH}]
		''')
		assertNotNull(result)
		assertTrue(result.eResource.errors.isEmpty, '''Unexpected errors: «result.eResource.errors.join(", ")»''')

		val attr = (result.statements.get(0) as AttributeStatement)
			.attributesection.attributes.get(0) as ValuedAttribute
		assertEquals("search", attr.name)
		assertEquals("${MKIFS_PATH}", attr.value)
	}

	@Test
	def void parseVariableInDeploymentPath() {
		val result = parseHelper.parse('''
			etc/plugin.so=../some_plugin/${VARIABLE}/file.so
		''')
		assertNotNull(result)
		assertTrue(result.eResource.errors.isEmpty, '''Unexpected errors: «result.eResource.errors.join(", ")»''')

		val stmt = result.statements.get(0) as DeploymentStatement
		val content = stmt.content as Path
		assertEquals("../some_plugin/${VARIABLE}/file.so", content.value)
	}

	@Test
	def void parseMultipleStatements() {
		val result = parseHelper.parse('''
			[uid=0 gid=0 perms=0555]
			bin/app1=aarch64le/bin/app1
			[+optional]
			bin/app2=aarch64le/bin/app2
		''')
		assertNotNull(result)
		assertTrue(result.eResource.errors.isEmpty, '''Unexpected errors: «result.eResource.errors.join(", ")»''')
		assertEquals(4, result.statements.size)
		assertTrue(result.statements.get(0) instanceof AttributeStatement)
		assertTrue(result.statements.get(1) instanceof DeploymentStatement)
		assertTrue(result.statements.get(2) instanceof AttributeStatement)
		assertTrue(result.statements.get(3) instanceof DeploymentStatement)
	}

	@Test
	def void parseCommentsAreIgnored() {
		val result = parseHelper.parse('''
			# leading comment
			[uid=0] # inline comment
			bin/app=src/app # trailing comment
		''')
		assertNotNull(result)
		assertTrue(result.eResource.errors.isEmpty, '''Unexpected errors: «result.eResource.errors.join(", ")»''')
		assertEquals(2, result.statements.size)
	}

	@Test
	def void parseQuotedPathsWithSpaces() {
		val result = parseHelper.parse('''
			[type=link] /usr/share/doc/"This is a pdf file.pdf"=/mnt/usr/share/doc/"This is a pdf file.pdf"
		''')
		assertNotNull(result)
		assertTrue(result.eResource.errors.isEmpty, '''Unexpected errors: «result.eResource.errors.join(", ")»''')

		val stmt = result.statements.get(0) as DeploymentStatement
		assertTrue(stmt.path.contains("This is a pdf file.pdf"))
	}

	@Test
	def void parseTypeLink() {
		val result = parseHelper.parse('''
			[type=link] /usr/lib/libfoo.so=/usr/lib/libfoo.so.1
		''')
		assertNotNull(result)
		assertTrue(result.eResource.errors.isEmpty, '''Unexpected errors: «result.eResource.errors.join(", ")»''')

		val attr = (result.statements.get(0) as DeploymentStatement)
			.attributesection.attributes.get(0) as ValuedAttribute
		assertEquals("type", attr.name)
		assertEquals("link", attr.value)
	}

	@Test
	def void parseFullBuildfile() {
		val result = parseHelper.parse('''
			[-optional]
			[search=${MKIFS_PATH}]
			[code=u]
			[data=c]
			[uid=0 gid=0 perms=0555]
			
			[uid=0 gid=0 perms=0555] bin/something=aarch64le/bin/some-thing
			[uid=53 gid=53 perms=0440] etc/my_plugin.so=../some_plugin/${VARIABLE}/file.so
		''')
		assertNotNull(result)
		assertTrue(result.eResource.errors.isEmpty, '''Unexpected errors: «result.eResource.errors.join(", ")»''')
		assertTrue(result.statements.size >= 6)
	}

	// ── Negative parsing tests (malformed syntax) ─────────────────

	@Test
	def void rejectUnclosedAttributeSection() {
		val result = parseHelper.parse('''
			[uid=0
		''')
		assertNotNull(result)
		assertFalse(result.eResource.errors.isEmpty, "Unclosed bracket should produce parse errors")
	}

	@Test
	def void rejectEmptyAttributeSection() {
		val result = parseHelper.parse('''
			[] bin/app=src/app
		''')
		assertNotNull(result)
		assertFalse(result.eResource.errors.isEmpty, "Empty attribute section should produce parse errors")
	}

	@Test
	def void rejectAttributeWithoutNameOrValue() {
		val result = parseHelper.parse('''
			[=value]
		''')
		assertNotNull(result)
		// The '=' without a name should fail to parse since ALMOST_ANYTHING is required before '='
		assertFalse(result.eResource.errors.isEmpty, "Attribute without name should produce parse errors")
	}

	@Test
	def void rejectNestedBrackets() {
		val result = parseHelper.parse('''
			[[uid=0]]
		''')
		assertNotNull(result)
		assertFalse(result.eResource.errors.isEmpty, "Nested brackets should produce parse errors")
	}
}
