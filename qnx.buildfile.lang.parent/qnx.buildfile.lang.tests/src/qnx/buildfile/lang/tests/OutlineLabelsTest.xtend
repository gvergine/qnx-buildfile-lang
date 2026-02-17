package qnx.buildfile.lang.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import qnx.buildfile.lang.buildfileDSL.AttributeStatement
import qnx.buildfile.lang.buildfileDSL.BooleanAttribute
import qnx.buildfile.lang.buildfileDSL.DeploymentStatement
import qnx.buildfile.lang.buildfileDSL.Model
import qnx.buildfile.lang.buildfileDSL.ValuedAttribute
import qnx.buildfile.lang.ide.BuildfileDSLOutlineLabels

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for {@link BuildfileDSLOutlineLabels} — verifies the label text
 * used for both the Eclipse outline view and the LSP document symbols (VSCode).
 */
@ExtendWith(InjectionExtension)
@InjectWith(BuildfileDSLInjectorProvider)
class OutlineLabelsTest {
	@Inject ParseHelper<Model> parseHelper

	// ── AttributeStatement labels ─────────────────────────────────

	@Test
	def void attributeStatementLabelSingleBoolean() {
		val model = parseHelper.parse('''[+optional]''')
		val stmt = model.statements.get(0) as AttributeStatement
		assertEquals("[+optional]", BuildfileDSLOutlineLabels.getName(stmt))
	}

	@Test
	def void attributeStatementLabelNegatedBoolean() {
		val model = parseHelper.parse('''[-compress]''')
		val stmt = model.statements.get(0) as AttributeStatement
		assertEquals("[-compress]", BuildfileDSLOutlineLabels.getName(stmt))
	}

	@Test
	def void attributeStatementLabelSingleValued() {
		val model = parseHelper.parse('''[uid=0]''')
		val stmt = model.statements.get(0) as AttributeStatement
		assertEquals("[uid=0]", BuildfileDSLOutlineLabels.getName(stmt))
	}

	@Test
	def void attributeStatementLabelMultipleAttrs() {
		val model = parseHelper.parse('''[uid=0 gid=0 perms=0555]''')
		val stmt = model.statements.get(0) as AttributeStatement
		assertEquals("[uid=0 gid=0 perms=0555]", BuildfileDSLOutlineLabels.getName(stmt))
	}

	@Test
	def void attributeStatementLabelMixedTypes() {
		val model = parseHelper.parse('''[+optional uid=0]''')
		val stmt = model.statements.get(0) as AttributeStatement
		assertEquals("[+optional uid=0]", BuildfileDSLOutlineLabels.getName(stmt))
	}

	@Test
	def void attributeStatementLabelWithVariable() {
		val model = parseHelper.parse('''[search=${MKIFS_PATH}]''')
		val stmt = model.statements.get(0) as AttributeStatement
		assertEquals("[search=${MKIFS_PATH}]", BuildfileDSLOutlineLabels.getName(stmt))
	}

	// ── DeploymentStatement labels ────────────────────────────────

	@Test
	def void deploymentLabelIsPath() {
		val model = parseHelper.parse('''bin/myapp=aarch64le/bin/myapp''')
		val stmt = model.statements.get(0) as DeploymentStatement
		assertEquals("bin/myapp", BuildfileDSLOutlineLabels.getName(stmt))
	}

	@Test
	def void deploymentDetailShowsAssignment() {
		val model = parseHelper.parse('''bin/app=aarch64le/bin/app''')
		val stmt = model.statements.get(0) as DeploymentStatement
		assertEquals("= aarch64le/bin/app", BuildfileDSLOutlineLabels.getDetail(stmt))
	}

	@Test
	def void deploymentFullLabelIncludesAssignment() {
		val model = parseHelper.parse('''bin/app=aarch64le/bin/app''')
		val stmt = model.statements.get(0) as DeploymentStatement
		assertEquals("bin/app = aarch64le/bin/app", BuildfileDSLOutlineLabels.getDeploymentLabel(stmt))
	}

	@Test
	def void deploymentWithInlineContentShowsEllipsis() {
		val model = parseHelper.parse(
			"/etc/myfile.txt={\nsome content\n}"
		)
		val stmt = model.statements.get(0) as DeploymentStatement
		assertEquals("/etc/myfile.txt", BuildfileDSLOutlineLabels.getName(stmt))
		assertEquals("= {...}", BuildfileDSLOutlineLabels.getDetail(stmt))
	}

	@Test
	def void deploymentWithoutAssignmentNoDetail() {
		val model = parseHelper.parse('''/proc/boot/some-binary''')
		val stmt = model.statements.get(0) as DeploymentStatement
		assertEquals("/proc/boot/some-binary", BuildfileDSLOutlineLabels.getName(stmt))
		assertNull(BuildfileDSLOutlineLabels.getDetail(stmt))
	}

	@Test
	def void deploymentWithVariableInContentPath() {
		val model = parseHelper.parse('''etc/plugin.so=../some_plugin/${VARIANT}/file.so''')
		val stmt = model.statements.get(0) as DeploymentStatement
		assertEquals("= ../some_plugin/${VARIANT}/file.so", BuildfileDSLOutlineLabels.getDetail(stmt))
	}

	// ── Individual attribute labels ───────────────────────────────

	@Test
	def void booleanAttributeEnabledLabel() {
		val model = parseHelper.parse('''[+optional]''')
		val attr = (model.statements.get(0) as AttributeStatement)
			.attributesection.attributes.get(0) as BooleanAttribute
		assertEquals("+optional", BuildfileDSLOutlineLabels.getName(attr))
	}

	@Test
	def void booleanAttributeDisabledLabel() {
		val model = parseHelper.parse('''[-compress]''')
		val attr = (model.statements.get(0) as AttributeStatement)
			.attributesection.attributes.get(0) as BooleanAttribute
		assertEquals("-compress", BuildfileDSLOutlineLabels.getName(attr))
	}

	@Test
	def void valuedAttributeLabel() {
		val model = parseHelper.parse('''[uid=53]''')
		val attr = (model.statements.get(0) as AttributeStatement)
			.attributesection.attributes.get(0) as ValuedAttribute
		assertEquals("uid=53", BuildfileDSLOutlineLabels.getName(attr))
	}

	@Test
	def void valuedAttributeWithVariableLabel() {
		val model = parseHelper.parse('''[search=${MKIFS_PATH}]''')
		val attr = (model.statements.get(0) as AttributeStatement)
			.attributesection.attributes.get(0) as ValuedAttribute
		assertEquals("search=${MKIFS_PATH}", BuildfileDSLOutlineLabels.getName(attr))
	}

	// ── Detail for non-deployment returns null ────────────────────

	@Test
	def void detailForAttributeStatementIsNull() {
		val model = parseHelper.parse('''[uid=0]''')
		assertNull(BuildfileDSLOutlineLabels.getDetail(model.statements.get(0)))
	}

	// ── Full buildfile ────────────────────────────────────────────

	@Test
	def void fullBuildfileLabels() {
		val model = parseHelper.parse('''
			[-optional]
			[search=${MKIFS_PATH}]
			[uid=0 gid=0 perms=0555]
			[uid=0 gid=0 perms=0555] bin/something=aarch64le/bin/some-thing
			[uid=53 gid=53 perms=0440] etc/my_plugin.so=../some_plugin/${VARIABLE}/file.so
		''')

		assertEquals("[-optional]", BuildfileDSLOutlineLabels.getName(model.statements.get(0)))
		assertEquals("[search=${MKIFS_PATH}]", BuildfileDSLOutlineLabels.getName(model.statements.get(1)))
		assertEquals("[uid=0 gid=0 perms=0555]", BuildfileDSLOutlineLabels.getName(model.statements.get(2)))
		assertEquals("bin/something", BuildfileDSLOutlineLabels.getName(model.statements.get(3)))
		assertEquals("etc/my_plugin.so", BuildfileDSLOutlineLabels.getName(model.statements.get(4)))
	}
}
