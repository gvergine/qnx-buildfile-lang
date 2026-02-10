package qnx.buildfile.lang.validation;

import com.google.inject.ImplementedBy;

/**
 * Provides the file path to a custom validator JAR.
 * <p>
 * The default implementation reads from the {@code customValidatorJar} system property,
 * which works for both the CLI ({@code System.setProperty} before setup) and the
 * LSP server ({@code -DcustomValidatorJar=...} JVM argument).
 * <p>
 * In Eclipse, this is overridden by the UI module to read from the preference store.
 */
@ImplementedBy(SystemPropertyCustomValidatorJarPathProvider.class)
public interface CustomValidatorJarPathProvider {

    /**
     * @return the absolute path to the custom validator JAR, or {@code null} if not configured.
     */
    String getJarPath();
}
