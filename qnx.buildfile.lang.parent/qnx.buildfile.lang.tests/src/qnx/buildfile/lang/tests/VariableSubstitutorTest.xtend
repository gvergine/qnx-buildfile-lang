package qnx.buildfile.lang.tests

import com.google.inject.Inject
import java.util.HashMap
import java.util.Map
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import qnx.buildfile.lang.buildfileDSL.DeploymentStatement
import qnx.buildfile.lang.buildfileDSL.Model
import qnx.buildfile.lang.buildfileDSL.Path
import qnx.buildfile.lang.buildfileDSL.ValuedAttribute
import qnx.buildfile.lang.buildfileDSL.AttributeStatement
import qnx.buildfile.lang.utils.VariableSubstitutor

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for {@link VariableSubstitutor} — verifies that ${VAR} references
 * are correctly resolved in attribute values, deployment paths, and content paths.
 */
@ExtendWith(InjectionExtension)
@InjectWith(BuildfileDSLInjectorProvider)
class VariableSubstitutorTest {
	@Inject ParseHelper<Model> parseHelper

	val substitutor = new VariableSubstitutor()

	private def Map<String, String> vars(String... pairs) {
		val map = new HashMap<String, String>()
		var i = 0
		while (i < pairs.length) {
			map.put(pairs.get(i), pairs.get(i + 1))
			i += 2
		}
		return map
	}

	// ── Substitution in attribute values ──────────────────────────

	@Test
	def void substituteInValuedAttribute() {
		val model = parseHelper.parse('''
			[search=${MKIFS_PATH}]
		''')
		substitutor.substituteVariables(model, vars("MKIFS_PATH", "/usr/lib/mkifs"))

		val attr = (model.statements.get(0) as AttributeStatement)
			.attributesection.attributes.get(0) as ValuedAttribute
		assertEquals("/usr/lib/mkifs", attr.value)
	}

	@Test
	def void substituteMultipleVariablesInValue() {
		val model = parseHelper.parse('''
			[search=${PREFIX}/${SUFFIX}]
		''')
		substitutor.substituteVariables(model, vars("PREFIX", "/opt", "SUFFIX", "lib"))

		val attr = (model.statements.get(0) as AttributeStatement)
			.attributesection.attributes.get(0) as ValuedAttribute
		assertEquals("/opt/lib", attr.value)
	}

	@Test
	def void undefinedVariableLeftUnchanged() {
		val model = parseHelper.parse('''
			[search=${UNDEFINED_VAR}]
		''')
		substitutor.substituteVariables(model, vars())

		val attr = (model.statements.get(0) as AttributeStatement)
			.attributesection.attributes.get(0) as ValuedAttribute
		assertEquals("${UNDEFINED_VAR}", attr.value,
			"Undefined variable reference should remain as-is")
	}

	@Test
	def void partialSubstitution() {
		val model = parseHelper.parse('''
			[search=${KNOWN}/${UNKNOWN}]
		''')
		substitutor.substituteVariables(model, vars("KNOWN", "/resolved"))

		val attr = (model.statements.get(0) as AttributeStatement)
			.attributesection.attributes.get(0) as ValuedAttribute
		assertEquals("/resolved/${UNKNOWN}", attr.value,
			"Known vars should be replaced, unknown left as-is")
	}

	// ── Substitution in deployment paths ──────────────────────────

	@Test
	def void substituteInDeploymentPath() {
		val model = parseHelper.parse('''
			${TARGET}/bin/app=src/app
		''')
		substitutor.substituteVariables(model, vars("TARGET", "/proc/boot"))

		val stmt = model.statements.get(0) as DeploymentStatement
		assertEquals("/proc/boot/bin/app", stmt.path)
	}

	// ── Substitution in content paths ─────────────────────────────

	@Test
	def void substituteInContentPath() {
		val model = parseHelper.parse('''
			etc/plugin.so=../some_plugin/${VARIANT}/file.so
		''')
		substitutor.substituteVariables(model, vars("VARIANT", "aarch64le"))

		val stmt = model.statements.get(0) as DeploymentStatement
		val content = stmt.content as Path
		assertEquals("../some_plugin/aarch64le/file.so", content.value)
	}

	@Test
	def void substituteInMultipleStatements() {
		val model = parseHelper.parse('''
			[search=${PATH}]
			${PREFIX}/bin/app1=aarch64le/bin/app1
			${PREFIX}/bin/app2=${SRC}/app2
		''')
		substitutor.substituteVariables(model, vars(
			"PATH", "/usr/lib",
			"PREFIX", "/proc/boot",
			"SRC", "aarch64le/bin"
		))

		val attr = (model.statements.get(0) as AttributeStatement)
			.attributesection.attributes.get(0) as ValuedAttribute
		assertEquals("/usr/lib", attr.value)

		val stmt1 = model.statements.get(1) as DeploymentStatement
		assertEquals("/proc/boot/bin/app1", stmt1.path)

		val stmt2 = model.statements.get(2) as DeploymentStatement
		assertEquals("/proc/boot/bin/app2", stmt2.path)
		assertEquals("aarch64le/bin/app2", (stmt2.content as Path).value)
	}

	// ── Edge cases ────────────────────────────────────────────────

	@Test
	def void noVariablesInInputNoChange() {
		val model = parseHelper.parse('''
			[uid=0]
		''')
		substitutor.substituteVariables(model, vars("ANYTHING", "value"))

		val attr = (model.statements.get(0) as AttributeStatement)
			.attributesection.attributes.get(0) as ValuedAttribute
		assertEquals("0", attr.value, "Input without variables should remain unchanged")
	}

	@Test
	def void emptyVarMapNoChange() {
		val model = parseHelper.parse('''
			[search=${MKIFS_PATH}]
		''')
		substitutor.substituteVariables(model, vars())

		val attr = (model.statements.get(0) as AttributeStatement)
			.attributesection.attributes.get(0) as ValuedAttribute
		assertEquals("${MKIFS_PATH}", attr.value)
	}

	@Test
	def void substituteWithSpecialRegexCharacters() {
		val model = parseHelper.parse('''
			[search=${PATH}]
		''')
		// Value contains regex special characters — substitutor should handle via quoteReplacement
		substitutor.substituteVariables(model, vars("PATH", "/usr/lib/$special/path"))

		val attr = (model.statements.get(0) as AttributeStatement)
			.attributesection.attributes.get(0) as ValuedAttribute
		assertEquals("/usr/lib/$special/path", attr.value)
	}
}
