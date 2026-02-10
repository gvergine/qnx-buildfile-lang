package qnx.buildfile.lang.ui;

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import qnx.buildfile.lang.ui.internal.LangActivator;
import qnx.buildfile.lang.validation.BuildfileDSLPreferenceConstants;

/**
 * Preference page for BuildfileDSL settings.
 * <p>
 * Accessible under Window > Preferences > BuildfileDSL > Custom Validator.
 */
public class CustomValidatorPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public CustomValidatorPreferencePage() {
        super(GRID);
        setDescription("Configure external custom validator JAR for BuildfileDSL.\n"
                + "The JAR must contain a Main-Class manifest entry pointing to a class that extends BaseDSLValidator.\n"
                + "Changes take effect on the next validation run (Project > Clean to re-trigger).");
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(LangActivator.getInstance().getPreferenceStore());
    }

    @Override
    protected void createFieldEditors() {
        FileFieldEditor jarPathEditor = new FileFieldEditor(
                BuildfileDSLPreferenceConstants.CUSTOM_VALIDATOR_JAR_PATH,
                "Custom Validator JAR:",
                true,                          // enforce absolute path
                getFieldEditorParent());
        jarPathEditor.setFileExtensions(new String[] { "*.jar" });
        addField(jarPathEditor);
    }
}
