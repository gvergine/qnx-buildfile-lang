package qnx.buildfile.lang.utils;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Loads JAR files at runtime, instantiates the class specified in the Main-Class
 * manifest attribute using its default constructor, and caches the results.
 * 
 * Supports automatic reloading when a JAR file's timestamp changes, while avoiding
 * unnecessary reload attempts when the file hasn't changed.
 */
public class JarLoader {

    private final Class<?> parentLoaderClass;
    private final Map<Path, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * Simple holder for cached JAR state.
     */
    private static final class CacheEntry {
        private final long lastModifiedTime;
        private final Object instance;
        private final Exception error;
        private final URLClassLoader classLoader;

        CacheEntry(long lastModifiedTime, Object instance, Exception error, URLClassLoader classLoader) {
            this.lastModifiedTime = lastModifiedTime;
            this.instance = instance;
            this.error = error;
            this.classLoader = classLoader;
        }

        long lastModifiedTime() { return lastModifiedTime; }
        Object instance() { return instance; }
        Exception error() { return error; }
        URLClassLoader classLoader() { return classLoader; }
    }

    public JarLoader(Class<?> parentLoaderClass) {
        this.parentLoaderClass = parentLoaderClass;
    }

    /**
     * Loads a JAR and returns an instance of its Main-Class.
     * 
     * Behavior:
     * - If never loaded: loads the JAR and creates an instance
     * - If loaded and timestamp changed: reloads the JAR and creates a new instance
     * - If loaded and timestamp unchanged: returns the cached instance
     * - If previous load failed and timestamp unchanged: throws the cached exception
     * 
     * @param jarPath path to the JAR file
     * @return an instance of the Main-Class from the JAR
     * @throws JarLoadException if the JAR cannot be loaded or the class cannot be instantiated
     */
    public synchronized Object loadJar(Path jarPath) throws JarLoadException {
        Path normalizedPath = jarPath.toAbsolutePath().normalize();
        long currentModified = getLastModifiedTime(normalizedPath);
        CacheEntry entry = cache.get(normalizedPath);
        
        if (entry != null && entry.lastModifiedTime() == currentModified) {
            if (entry.error() != null) {
                throw new JarLoadException("Previous load attempt failed and JAR has not changed: " + jarPath, entry.error());
            }
            if (entry.instance() != null) {
                return entry.instance();
            }
        }
        
        // Close old classloader if reloading
        if (entry != null && entry.classLoader() != null) {
            closeClassLoader(entry.classLoader());
        }
        
        try {
            CacheEntry newEntry = loadAndInstantiate(normalizedPath, currentModified);
            cache.put(normalizedPath, newEntry);
            return newEntry.instance();
        } catch (Exception e) {
            cache.put(normalizedPath, new CacheEntry(currentModified, null, e, null));
            throw new JarLoadException("Failed to load JAR and instantiate Main-Class: " + jarPath, e);
        }
    }

    private long getLastModifiedTime(Path jarPath) {
        try {
            return Files.getLastModifiedTime(jarPath).toMillis();
        } catch (IOException e) {
            return -1;
        }
    }

    private CacheEntry loadAndInstantiate(Path jarPath, long modifiedTime) throws Exception {
        if (!Files.exists(jarPath)) {
            throw new IOException("JAR file does not exist: " + jarPath);
        }

        String mainClassName = getMainClassName(jarPath);
        if (mainClassName == null || mainClassName.isBlank()) {
            throw new IllegalStateException("No Main-Class attribute found in JAR manifest: " + jarPath);
        }

        URL jarUrl = jarPath.toUri().toURL();
        URLClassLoader classLoader = new URLClassLoader(
            new URL[]{jarUrl},
            parentLoaderClass.getClassLoader()
        );

        try {
            Class<?> mainClass = classLoader.loadClass(mainClassName);
            Object instance = mainClass.getDeclaredConstructor().newInstance();
            return new CacheEntry(modifiedTime, instance, null, classLoader);
        } catch (Exception e) {
            closeClassLoader(classLoader);
            throw e;
        }
    }

    private String getMainClassName(Path jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                throw new IllegalStateException("JAR file has no manifest: " + jarPath);
            }
            return manifest.getMainAttributes().getValue("Main-Class");
        }
    }

    private void closeClassLoader(URLClassLoader classLoader) {
        try {
            classLoader.close();
        } catch (IOException e) {
            // Ignore close errors
        }
    }

    /**
     * Exception thrown when JAR loading or class instantiation fails.
     */
    public static class JarLoadException extends Exception {
        
        public JarLoadException(String message) {
            super(message);
        }

        public JarLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

