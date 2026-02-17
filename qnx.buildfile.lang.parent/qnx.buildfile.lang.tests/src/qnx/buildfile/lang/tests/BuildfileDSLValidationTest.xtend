package qnx.buildfile.lang.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import qnx.buildfile.lang.buildfileDSL.BuildfileDSLPackage
import qnx.buildfile.lang.buildfileDSL.Model

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for validation rules: unknown attributes, duplicate paths,
 * and attribute value checking (uid, gid, perms, dperms, autoso, compress, type).
 */
@ExtendWith(InjectionExtension)
@InjectWith(BuildfileDSLInjectorProvider)
class BuildfileDSLValidationTest {
	@Inject ParseHelper<Model> parseHelper
	@Inject ValidationTestHelper validationHelper

	// ── BasicDSLValidator: unknown attribute names ────────────────

	@Test
	def void validBooleanAttributeNoError() {
		val model = parseHelper.parse('''
			[+optional]
		''')
		validationHelper.assertNoErrors(model)
	}

	@Test
	def void unknownBooleanAttributeProducesError() {
		val model = parseHelper.parse('''
			[+nonexistent_attr]
		''')
		validationHelper.assertError(model,
			BuildfileDSLPackage.Literals.BOOLEAN_ATTRIBUTE,
			"invalidName")
	}

	@Test
	def void validValuedAttributeNoError() {
		val model = parseHelper.parse('''
			[uid=0]
		''')
		validationHelper.assertNoErrors(model)
	}

	@Test
	def void unknownValuedAttributeProducesError() {
		val model = parseHelper.parse('''
			[nonexistent_attr=value]
		''')
		validationHelper.assertError(model,
			BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE,
			"invalidName")
	}

	@Test
	def void allStandardBooleanAttributesAccepted() {
		// Test a selection of known boolean keywords
		for (name : #["optional", "autolink", "compress", "followlink", "raw", "script"]) {
			val model = parseHelper.parse('''[+«name»]''')
			validationHelper.assertNoError(model, "invalidName")
		}
	}

	@Test
	def void allStandardValuedAttributesAccepted() {
		// Test a selection of known valued keywords with valid values
		val model = parseHelper.parse('''
			[uid=0 gid=0 perms=0755 type=file search=/usr/lib]
		''')
		validationHelper.assertNoError(model, "invalidName")
	}

	// ── DuplicatePathValidator ────────────────────────────────────

	@Test
	def void noDuplicatePathNoWarning() {
		val model = parseHelper.parse('''
			bin/app1=src/app1
			bin/app2=src/app2
		''')
		validationHelper.assertNoIssues(model)
	}

	@Test
	def void duplicatePathProducesWarning() {
		val model = parseHelper.parse('''
			bin/app=aarch64le/bin/app
			bin/app=aarch64le/bin/app_v2
		''')
		validationHelper.assertWarning(model,
			BuildfileDSLPackage.Literals.DEPLOYMENT_STATEMENT,
			"duplicatePath")
	}

	@Test
	def void triplicatePathProducesWarnings() {
		val model = parseHelper.parse('''
			bin/app=src/v1
			bin/app=src/v2
			bin/app=src/v3
		''')
		val issues = validationHelper.validate(model)
		val duplicateWarnings = issues.filter[code == "duplicatePath"]
		assertEquals(3, duplicateWarnings.size, "Each of the 3 duplicate entries should get a warning")
	}

	@Test
	def void differentPathsNoDuplicateWarning() {
		val model = parseHelper.parse('''
			bin/app1=src/same-source
			bin/app2=src/same-source
		''')
		validationHelper.assertNoIssues(model)
	}

	// ── AttributeValueChecker: uid ────────────────────────────────

	@Test
	def void validUidZero() {
		val model = parseHelper.parse('''[uid=0]''')
		validationHelper.assertNoError(model, "invalidUid")
	}

	@Test
	def void validUidPositive() {
		val model = parseHelper.parse('''[uid=1000]''')
		validationHelper.assertNoError(model, "invalidUid")
	}

	@Test
	def void validUidWildcard() {
		val model = parseHelper.parse('''[uid=*]''')
		validationHelper.assertNoError(model, "invalidUid")
	}

	@Test
	def void invalidUidNonNumeric() {
		val model = parseHelper.parse('''[uid=abc]''')
		validationHelper.assertError(model,
			BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE,
			"invalidUid")
	}

	@Test
	def void invalidUidNegative() {
		// Note: the grammar's ALMOST_ANYTHING may not capture a leading '-' here,
		// but if it does, the validator should reject it.
		val model = parseHelper.parse('''[uid=-1]''')
		if (model !== null && model.eResource.errors.isEmpty) {
			validationHelper.assertError(model,
				BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE,
				"invalidUid")
		}
	}

	// ── AttributeValueChecker: gid ────────────────────────────────

	@Test
	def void validGidZero() {
		val model = parseHelper.parse('''[gid=0]''')
		validationHelper.assertNoError(model, "invalidGid")
	}

	@Test
	def void validGidPositive() {
		val model = parseHelper.parse('''[gid=53]''')
		validationHelper.assertNoError(model, "invalidGid")
	}

	@Test
	def void validGidWildcard() {
		val model = parseHelper.parse('''[gid=*]''')
		validationHelper.assertNoError(model, "invalidGid")
	}

	@Test
	def void invalidGidNonNumeric() {
		val model = parseHelper.parse('''[gid=root]''')
		validationHelper.assertError(model,
			BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE,
			"invalidGid")
	}

	// ── AttributeValueChecker: autoso ─────────────────────────────

	@Test
	def void validAutosoValues() {
		for (v : #["n", "none", "l", "list", "a", "add"]) {
			val model = parseHelper.parse('''[autoso=«v»]''')
			validationHelper.assertNoError(model, "invalidAutoso")
		}
	}

	@Test
	def void invalidAutosoValue() {
		val model = parseHelper.parse('''[autoso=invalid]''')
		validationHelper.assertError(model,
			BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE,
			"invalidAutoso")
	}

	// ── AttributeValueChecker: compress ───────────────────────────

	@Test
	def void validCompressValues() {
		for (v : #["1", "2", "3"]) {
			val model = parseHelper.parse('''[compress=«v»]''')
			validationHelper.assertNoError(model, "invalidCompress")
		}
	}

	@Test
	def void invalidCompressValue() {
		val model = parseHelper.parse('''[compress=9]''')
		validationHelper.assertError(model,
			BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE,
			"invalidCompress")
	}

	@Test
	def void invalidCompressNonNumeric() {
		val model = parseHelper.parse('''[compress=fast]''')
		validationHelper.assertError(model,
			BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE,
			"invalidCompress")
	}

	// ── AttributeValueChecker: type ───────────────────────────────

	@Test
	def void validTypeValues() {
		for (v : #["link", "fifo", "file", "dir"]) {
			val model = parseHelper.parse('''[type=«v»]''')
			validationHelper.assertNoError(model, "invalidType")
		}
	}

	@Test
	def void invalidTypeValue() {
		val model = parseHelper.parse('''[type=socket]''')
		validationHelper.assertError(model,
			BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE,
			"invalidType")
	}

	// ── AttributeValueChecker: perms ──────────────────────────────

	@Test
	def void validPermsOctal() {
		for (v : #["0755", "0644", "0400", "777", "444"]) {
			val model = parseHelper.parse('''[perms=«v»]''')
			validationHelper.assertNoError(model, "invalidPerms")
		}
	}

	@Test
	def void validPermsWildcard() {
		val model = parseHelper.parse('''[perms=*]''')
		validationHelper.assertNoError(model, "invalidPerms")
	}

	@Test
	def void validPermsSymbolic() {
		// Symbolic modes like a+rwx, u=rwx,g=rx,o=rx
		val model = parseHelper.parse('''[perms=a+rwx]''')
		validationHelper.assertNoError(model, "invalidPerms")
	}

	@Test
	def void invalidPermsValue() {
		val model = parseHelper.parse('''[perms=9999]''')
		validationHelper.assertError(model,
			BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE,
			"invalidPerms")
	}

	@Test
	def void invalidPermsText() {
		val model = parseHelper.parse('''[perms=readonly]''')
		validationHelper.assertError(model,
			BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE,
			"invalidPerms")
	}

	// ── AttributeValueChecker: dperms ─────────────────────────────

	@Test
	def void validDpermsOctal() {
		val model = parseHelper.parse('''[dperms=0755]''')
		validationHelper.assertNoError(model, "invalidDperms")
	}

	@Test
	def void validDpermsWildcard() {
		val model = parseHelper.parse('''[dperms=*]''')
		validationHelper.assertNoError(model, "invalidDperms")
	}

	@Test
	def void invalidDpermsValue() {
		val model = parseHelper.parse('''[dperms=rwx]''')
		// 'rwx' alone is not valid symbolic (needs [ugoa]*[+-=][rwxst]+)
		validationHelper.assertError(model,
			BuildfileDSLPackage.Literals.VALUED_ATTRIBUTE,
			"invalidDperms")
	}

	// ── Combined validation scenarios ─────────────────────────────

	@Test
	def void multipleValidationErrorsInOneFile() {
		val model = parseHelper.parse('''
			[uid=abc gid=xyz perms=invalid]
		''')
		val issues = validationHelper.validate(model)
		val errors = issues.filter[severity.name == "ERROR"]
		assertTrue(errors.size >= 3, "Should have at least 3 validation errors (uid, gid, perms)")
	}

	@Test
	def void validCompleteDeploymentNoErrors() {
		val model = parseHelper.parse('''
			[uid=0 gid=0 perms=0555] bin/myapp=aarch64le/bin/myapp
		''')
		validationHelper.assertNoErrors(model)
	}

	@Test
	def void validBuildfileWithMixedStatements() {
		val model = parseHelper.parse('''
			[-optional]
			[search=${MKIFS_PATH}]
			[uid=0 gid=0 perms=0555]
			[uid=0 gid=0 perms=0555] bin/app1=aarch64le/bin/app1
			[uid=53 gid=53 perms=0440] etc/config.so=../plugin/${VAR}/file.so
		''')
		validationHelper.assertNoErrors(model)
	}
}
