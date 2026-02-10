package qnx.buildfile.lang.validation;

import com.google.inject.ImplementedBy;

/**
 * Provides the file path to a custom validator JAR.
 * <p>
 * In Eclipse, this is backed by the preference store.
 * In the LSP server, this can be configured via initialization options.
 * In the CLI, the path is passed directly (no Guice needed).
 * <p>
 * The default implementation returns {@code null} (no custom validator).
 */
@ImplementedBy(CustomValidatorJarPathProvider.Default.class)
public interface CustomValidatorJarPathProvider {

    /**
     * @return the absolute path to the custom validator JAR, or {@code null} if not configured.
     */
    String getJarPath();

    /**
     * Default implementation that returns no path.
     */
    class Default implements CustomValidatorJarPathProvider {
        @Override
        public String getJarPath() {
            return null;
        }
    }
}
