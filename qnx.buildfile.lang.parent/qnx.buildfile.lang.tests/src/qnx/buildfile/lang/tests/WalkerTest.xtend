package qnx.buildfile.lang.tests

import com.google.inject.Inject
import java.util.ArrayList
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import qnx.buildfile.lang.buildfileDSL.AttributeSection
import qnx.buildfile.lang.buildfileDSL.AttributeStatement
import qnx.buildfile.lang.buildfileDSL.BooleanAttribute
import qnx.buildfile.lang.buildfileDSL.ContentBlock
import qnx.buildfile.lang.buildfileDSL.DeploymentStatement
import qnx.buildfile.lang.buildfileDSL.Model
import qnx.buildfile.lang.buildfileDSL.Path
import qnx.buildfile.lang.buildfileDSL.ValuedAttribute
import qnx.buildfile.lang.utils.Walker
import qnx.buildfile.lang.utils.Walker.IWalker

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for {@link Walker} — verifies that the tree walker visits all AST nodes
 * in the expected order and invokes the correct {@link IWalker} callbacks.
 */
@ExtendWith(InjectionExtension)
@InjectWith(BuildfileDSLInjectorProvider)
class WalkerTest {
	@Inject ParseHelper<Model> parseHelper

	val walker = new Walker()

	@Test
	def void walkerVisitsModel() {
		val model = parseHelper.parse('''
			[uid=0]
		''')
		val visited = new ArrayList<String>()
		walker.walk(model, new IWalker() {
			override found(Model m) { visited.add("Model") }
		})
		assertTrue(visited.contains("Model"), "Walker should visit the Model node")
	}

	@Test
	def void walkerVisitsAttributeStatement() {
		val model = parseHelper.parse('''
			[uid=0]
		''')
		val visited = new ArrayList<String>()
		walker.walk(model, new IWalker() {
			override found(AttributeStatement a_s) { visited.add("AttributeStatement") }
		})
		assertTrue(visited.contains("AttributeStatement"))
	}

	@Test
	def void walkerVisitsAttributeSection() {
		val model = parseHelper.parse('''
			[uid=0 gid=0]
		''')
		val visited = new ArrayList<String>()
		walker.walk(model, new IWalker() {
			override found(AttributeSection a_s) { visited.add("AttributeSection") }
		})
		assertEquals(1, visited.filter["AttributeSection".equals(it)].size)
	}

	@Test
	def void walkerVisitsBooleanAttribute() {
		val model = parseHelper.parse('''
			[+optional -compress]
		''')
		val names = new ArrayList<String>()
		walker.walk(model, new IWalker() {
			override found(BooleanAttribute ba) { names.add(ba.name) }
		})
		assertEquals(#["optional", "compress"], names)
	}

	@Test
	def void walkerVisitsValuedAttribute() {
		val model = parseHelper.parse('''
			[uid=0 gid=53 perms=0755]
		''')
		val pairs = new ArrayList<String>()
		walker.walk(model, new IWalker() {
			override found(ValuedAttribute va) { pairs.add(va.name + "=" + va.value) }
		})
		assertEquals(#["uid=0", "gid=53", "perms=0755"], pairs)
	}

	@Test
	def void walkerVisitsDeploymentStatement() {
		val model = parseHelper.parse('''
			bin/app=src/app
		''')
		val paths = new ArrayList<String>()
		walker.walk(model, new IWalker() {
			override found(DeploymentStatement ds) { paths.add(ds.path) }
		})
		assertEquals(#["bin/app"], paths)
	}

	@Test
	def void walkerVisitsDeploymentPath() {
		val model = parseHelper.parse('''
			bin/app=aarch64le/bin/app
		''')
		val contentPaths = new ArrayList<String>()
		walker.walk(model, new IWalker() {
			override found(Path p) { contentPaths.add(p.value) }
		})
		assertEquals(#["aarch64le/bin/app"], contentPaths)
	}

	@Test
	def void walkerVisitsContentBlock() {
		val model = parseHelper.parse(
			"/etc/myfile.txt={\nsome content\n}"
		)
		val blocks = new ArrayList<String>()
		walker.walk(model, new IWalker() {
			override found(ContentBlock cb) { blocks.add(cb.value) }
		})
		assertEquals(1, blocks.size, "Should visit exactly one ContentBlock")
	}

	@Test
	def void walkerVisitsDeploymentAttributeSection() {
		val model = parseHelper.parse('''
			[uid=0 gid=0] bin/app=src/app
		''')
		val sections = new ArrayList<Integer>()
		walker.walk(model, new IWalker() {
			override found(AttributeSection a_s) { sections.add(a_s.attributes.size) }
		})
		assertEquals(#[2], sections, "Deployment's attribute section should be visited with 2 attributes")
	}

	@Test
	def void walkerVisitsAllNodesInOrder() {
		val model = parseHelper.parse('''
			[+optional]
			[uid=0 gid=0] bin/app=aarch64le/bin/app
		''')
		val trace = new ArrayList<String>()
		walker.walk(model, new IWalker() {
			override found(Model m) { trace.add("Model") }
			override found(AttributeStatement a_s) { trace.add("AttrStmt") }
			override found(DeploymentStatement ds) { trace.add("DeployStmt:" + ds.path) }
			override found(AttributeSection a_s) { trace.add("AttrSection") }
			override found(BooleanAttribute ba) { trace.add("Bool:" + ba.name) }
			override found(ValuedAttribute va) { trace.add("Val:" + va.name) }
			override found(Path p) { trace.add("Path:" + p.value) }
		})

		assertEquals("Model", trace.get(0))
		assertTrue(trace.contains("AttrStmt"))
		assertTrue(trace.contains("Bool:optional"))
		assertTrue(trace.contains("DeployStmt:bin/app"))
		assertTrue(trace.contains("Val:uid"))
		assertTrue(trace.contains("Val:gid"))
		assertTrue(trace.contains("Path:aarch64le/bin/app"))
	}

	@Test
	def void walkerHandlesMultipleDeployments() {
		val model = parseHelper.parse('''
			bin/app1=src/app1
			bin/app2=src/app2
			bin/app3=src/app3
		''')
		val paths = new ArrayList<String>()
		walker.walk(model, new IWalker() {
			override found(DeploymentStatement ds) { paths.add(ds.path) }
		})
		assertEquals(#["bin/app1", "bin/app2", "bin/app3"], paths)
	}

	@Test
	def void walkerHandlesDeploymentWithoutAttributes() {
		val model = parseHelper.parse('''
			bin/app=src/app
		''')
		val sectionCount = new ArrayList<Integer>()
		walker.walk(model, new IWalker() {
			override found(AttributeSection a_s) { sectionCount.add(1) }
		})
		assertTrue(sectionCount.isEmpty, "Deployment without attributes should not visit AttributeSection")
	}
}
