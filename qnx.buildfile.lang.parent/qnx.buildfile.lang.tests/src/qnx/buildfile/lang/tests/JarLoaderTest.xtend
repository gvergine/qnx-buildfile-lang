package qnx.buildfile.lang.tests

import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import qnx.buildfile.lang.utils.JarLoader
import qnx.buildfile.lang.utils.JarLoader.JarLoadException

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for {@link JarLoader} — verifies JAR loading, Main-Class instantiation,
 * caching, timestamp-based reloading, and error handling.
 */
class JarLoaderTest {

	JarLoader jarLoader

	@BeforeEach
	def void setUp() {
		jarLoader = new JarLoader(JarLoaderTest)
	}

	// ── Error handling ────────────────────────────────────────────

	@Test
	def void loadNonExistentJarThrows(@TempDir Path tempDir) {
		val missingJar = tempDir.resolve("does_not_exist.jar")
		try {
			jarLoader.loadJar(missingJar)
			fail("Expected JarLoadException for non-existent JAR")
		} catch (JarLoadException e) {
			// expected
		}
	}

	@Test
	def void loadNonJarFileThrows(@TempDir Path tempDir) {
		val fakeJar = tempDir.resolve("fake.jar")
		Files.writeString(fakeJar, "this is not a jar file")
		try {
			jarLoader.loadJar(fakeJar)
			fail("Expected JarLoadException for non-JAR file")
		} catch (JarLoadException e) {
			// expected
		}
	}

	@Test
	def void loadJarWithoutManifestThrows(@TempDir Path tempDir) {
		val jarPath = tempDir.resolve("no-manifest.jar")
		createJarWithoutManifest(jarPath)
		try {
			jarLoader.loadJar(jarPath)
			fail("Expected JarLoadException for JAR without manifest")
		} catch (JarLoadException e) {
			// expected
		}
	}

	@Test
	def void loadJarWithoutMainClassThrows(@TempDir Path tempDir) {
		val jarPath = tempDir.resolve("no-mainclass.jar")
		createJarWithManifest(jarPath, null)
		try {
			jarLoader.loadJar(jarPath)
			fail("Expected JarLoadException for JAR without Main-Class")
		} catch (JarLoadException e) {
			// expected
		}
	}

	@Test
	def void loadJarWithEmptyMainClassThrows(@TempDir Path tempDir) {
		val jarPath = tempDir.resolve("empty-mainclass.jar")
		createJarWithManifest(jarPath, "   ")
		try {
			jarLoader.loadJar(jarPath)
			fail("Expected JarLoadException for JAR with blank Main-Class")
		} catch (JarLoadException e) {
			// expected
		}
	}

	@Test
	def void loadJarWithNonExistentClassThrows(@TempDir Path tempDir) {
		val jarPath = tempDir.resolve("bad-class.jar")
		createJarWithManifest(jarPath, "com.nonexistent.ClassName")
		try {
			jarLoader.loadJar(jarPath)
			fail("Expected JarLoadException for JAR with non-existent Main-Class")
		} catch (JarLoadException e) {
			// expected
		}
	}

	// ── Successful loading ────────────────────────────────────────

	@Test
	def void loadValidJarReturnsInstance(@TempDir Path tempDir) {
		val jarPath = createValidTestJar(tempDir, "test.jar")
		val instance = jarLoader.loadJar(jarPath)
		assertNotNull(instance, "Should return an instance of the Main-Class")
	}

	// ── Caching ───────────────────────────────────────────────────

	@Test
	def void cachedInstanceReturnedOnSecondLoad(@TempDir Path tempDir) {
		val jarPath = createValidTestJar(tempDir, "cached.jar")
		val instance1 = jarLoader.loadJar(jarPath)
		val instance2 = jarLoader.loadJar(jarPath)
		assertSame(instance1, instance2, "Same instance should be returned from cache")
	}

	@Test
	def void reloadsWhenTimestampChanges(@TempDir Path tempDir) {
		val jarPath = createValidTestJar(tempDir, "reload.jar")
		val instance1 = jarLoader.loadJar(jarPath)

		// Ensure filesystem timestamp granularity is exceeded
		Thread.sleep(1100)
		createValidTestJar(tempDir, "reload.jar")

		val instance2 = jarLoader.loadJar(jarPath)
		assertNotSame(instance1, instance2,
			"New instance should be created after JAR timestamp changes")
	}

	@Test
	def void failedLoadCachedAndRethrown(@TempDir Path tempDir) {
		val jarPath = tempDir.resolve("broken.jar")
		createJarWithManifest(jarPath, "com.nonexistent.ClassName")

		// First load should fail
		try {
			jarLoader.loadJar(jarPath)
			fail("Expected JarLoadException on first load")
		} catch (JarLoadException e) {
			// expected
		}

		// Second load with same timestamp should also fail (cached error)
		try {
			jarLoader.loadJar(jarPath)
			fail("Expected JarLoadException on second load (cached)")
		} catch (JarLoadException e) {
			assertTrue(e.message.contains("Previous load attempt failed"),
				"Cached error should mention previous failure")
		}
	}

	// ── Helper methods ────────────────────────────────────────────

	/**
	 * Creates a valid JAR containing java.util.HashMap as Main-Class.
	 * HashMap is always on the classpath and has a no-arg constructor.
	 */
	private def Path createValidTestJar(Path dir, String name) {
		val jarPath = dir.resolve(name)
		createJarWithManifest(jarPath, "java.util.HashMap")
		return jarPath
	}

	private def void createJarWithManifest(Path jarPath, String mainClass) {
		val manifest = new Manifest()
		manifest.mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
		if (mainClass !== null) {
			manifest.mainAttributes.put(Attributes.Name.MAIN_CLASS, mainClass)
		}

		val fos = new FileOutputStream(jarPath.toFile)
		try {
			val jos = new JarOutputStream(fos, manifest)
			try {
				jos.putNextEntry(new JarEntry("META-INF/dummy.txt"))
				jos.write("placeholder".bytes)
				jos.closeEntry()
			} finally {
				jos.close()
			}
		} finally {
			fos.close()
		}
	}

	private def void createJarWithoutManifest(Path jarPath) {
		val fos = new FileOutputStream(jarPath.toFile)
		try {
			val jos = new JarOutputStream(fos)
			try {
				jos.putNextEntry(new JarEntry("dummy.txt"))
				jos.write("placeholder".bytes)
				jos.closeEntry()
			} finally {
				jos.close()
			}
		} finally {
			fos.close()
		}
	}
}
