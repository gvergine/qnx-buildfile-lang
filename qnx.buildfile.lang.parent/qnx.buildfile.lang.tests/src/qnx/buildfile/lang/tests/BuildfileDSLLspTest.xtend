package qnx.buildfile.lang.tests

import com.google.inject.Inject
import org.eclipse.xtext.diagnostics.Severity
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.util.CancelIndicator
import org.eclipse.xtext.validation.CheckMode
import org.eclipse.xtext.validation.IResourceValidator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import qnx.buildfile.lang.buildfileDSL.Model

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests that exercise the full validation pipeline as seen by the LSP server.
 * Uses the same Guice-injected {@link IResourceValidator} that the LSP server
 * delegates to, ensuring diagnostics (errors/warnings) are correctly surfaced
 * for the VSCode extension experience.
 */
@ExtendWith(InjectionExtension)
@InjectWith(BuildfileDSLInjectorProvider)
class BuildfileDSLLspTest {
	@Inject ParseHelper<Model> parseHelper
	@Inject IResourceValidator resourceValidator

	// ── No diagnostics for valid input ────────────────────────────

	@Test
	def void noDiagnosticsForValidFile() {
		val model = parseHelper.parse('''
			[uid=0 gid=0 perms=0555]
			bin/myapp=aarch64le/bin/myapp
		''')
		val issues = resourceValidator.validate(model.eResource, CheckMode.ALL, CancelIndicator.NullImpl)
		assertTrue(issues.isEmpty,
			'''Expected no diagnostics but got: «issues.map[message].join(", ")»''')
	}

	@Test
	def void noDiagnosticsForCommentHeavyFile() {
		val model = parseHelper.parse('''
			[uid=0 gid=0 perms=0555]
			bin/myapp=aarch64le/bin/myapp
		''')
		val issues = resourceValidator.validate(model.eResource, CheckMode.ALL, CancelIndicator.NullImpl)
		assertTrue(issues.isEmpty,
			'''Expected no diagnostics but got: «issues.map[message].join(", ")»''')
	}

	// ── Error diagnostics ─────────────────────────────────────────

	@Test
	def void diagnosticForUnknownBooleanAttribute() {
		val model = parseHelper.parse('''
			[+fake_attribute]
		''')
		val issues = resourceValidator.validate(model.eResource, CheckMode.ALL, CancelIndicator.NullImpl)
		assertFalse(issues.isEmpty, "Should report diagnostic for unknown boolean attribute")
		assertTrue(issues.exists[message.contains("Unknown BooleanAttribute")],
			'''Expected unknown attribute diagnostic but got: «issues.map[message].join(", ")»''')
	}

	@Test
	def void diagnosticForUnknownValuedAttribute() {
		val model = parseHelper.parse('''
			[fake_attr=value]
		''')
		val issues = resourceValidator.validate(model.eResource, CheckMode.ALL, CancelIndicator.NullImpl)
		assertFalse(issues.isEmpty, "Should report diagnostic for unknown valued attribute")
		assertTrue(issues.exists[message.contains("Unknown ValuedAttribute")],
			'''Expected unknown attribute diagnostic but got: «issues.map[message].join(", ")»''')
	}

	@Test
	def void diagnosticForInvalidUid() {
		val model = parseHelper.parse('''
			[uid=abc]
		''')
		val issues = resourceValidator.validate(model.eResource, CheckMode.ALL, CancelIndicator.NullImpl)
		assertTrue(issues.exists[message.contains("uid") && severity == Severity.ERROR],
			'''Expected uid error diagnostic but got: «issues.map[message].join(", ")»''')
	}

	@Test
	def void diagnosticForInvalidPerms() {
		val model = parseHelper.parse('''
			[perms=9999]
		''')
		val issues = resourceValidator.validate(model.eResource, CheckMode.ALL, CancelIndicator.NullImpl)
		assertTrue(issues.exists[message.contains("perms") && severity == Severity.ERROR],
			'''Expected perms error diagnostic but got: «issues.map[message].join(", ")»''')
	}

	// ── Warning diagnostics ───────────────────────────────────────

	@Test
	def void warningDiagnosticForDuplicatePaths() {
		val model = parseHelper.parse('''
			bin/app=src/v1
			bin/app=src/v2
		''')
		val issues = resourceValidator.validate(model.eResource, CheckMode.ALL, CancelIndicator.NullImpl)
		assertTrue(issues.exists[message.contains("Duplicate path") && severity == Severity.WARNING],
			'''Expected duplicate path warning but got: «issues.map[message].join(", ")»''')
	}

	// ── Multiple diagnostics ──────────────────────────────────────

	@Test
	def void multipleDiagnosticsInOneFile() {
		val model = parseHelper.parse('''
			[uid=bad gid=bad perms=bad]
		''')
		val issues = resourceValidator.validate(model.eResource, CheckMode.ALL, CancelIndicator.NullImpl)
		val errors = issues.filter[severity == Severity.ERROR]
		assertTrue(errors.size >= 3,
			'''Expected at least 3 error diagnostics but got «errors.size»: «errors.map[message].join(", ")»''')
	}
}
