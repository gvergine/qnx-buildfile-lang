package qnx.buildfile.lang.ui;

import org.eclipse.jface.preference.IPreferenceStore;

import qnx.buildfile.lang.ui.internal.LangActivator;
import qnx.buildfile.lang.validation.BuildfileDSLPreferenceConstants;
import qnx.buildfile.lang.validation.CustomValidatorJarPathProvider;

/**
 * Eclipse-specific implementation that reads the custom validator JAR path
 * from the Eclipse preference store (set via the preference page).
 */
public class EclipseCustomValidatorJarPathProvider implements CustomValidatorJarPathProvider {

    @Override
    public String getJarPath() {
        LangActivator activator = LangActivator.getInstance();
        if (activator == null) {
            return null;
        }

        IPreferenceStore store = activator.getPreferenceStore();
        String path = store.getString(BuildfileDSLPreferenceConstants.CUSTOM_VALIDATOR_JAR_PATH);

        if (path == null || path.isBlank()) {
            return null;
        }
        return path;
    }
}
