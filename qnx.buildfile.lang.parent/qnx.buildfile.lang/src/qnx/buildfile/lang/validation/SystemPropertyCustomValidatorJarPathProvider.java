package qnx.buildfile.lang.validation;

/**
 * Implementation that reads the custom validator JAR path from a JVM system property.
 * <p>
 * Used by the LSP server, where the VSCode extension passes the path via
 * {@code -DcustomValidatorJar=/path/to/jar} when launching the Java process.
 */
public class SystemPropertyCustomValidatorJarPathProvider implements CustomValidatorJarPathProvider {

    /** System property name set by the VSCode extension at launch time. */
    public static final String SYSTEM_PROPERTY = "customValidatorJar";

    @Override
    public String getJarPath() {
        String path = System.getProperty(SYSTEM_PROPERTY);
        if (path == null || path.isBlank()) {
            return null;
        }
        return path;
    }
}
