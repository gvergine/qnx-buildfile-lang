package qnx.buildfile.lang.validation;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.AbstractDeclarativeValidator;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.ComposedChecks;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import com.google.inject.Inject;

import qnx.buildfile.lang.buildfileDSL.Model;

@ComposedChecks(validators = {BasicDSLValidator.class})
public class BuildfileDSLValidator extends BaseDSLValidator
{
    @Inject
    private CustomValidatorJarPathProvider jarPathProvider;

    private Object extendedValidator;
    private List<Method> extendedValidatorCheckMethods;
    private boolean extendedValidatorLoaded = false;
    /** Track which path was loaded so we reload if the preference changes */
    private String loadedJarPath = null;
    
    @Check
    public void loadExtendedValidator(Model model) {
        String jarPath = (jarPathProvider != null) ? jarPathProvider.getJarPath() : null;

        // If the path changed (or was cleared), reset state
        if (!isEqualPath(jarPath, loadedJarPath)) {
            extendedValidator = null;
            extendedValidatorCheckMethods = null;
            extendedValidatorLoaded = false;
            loadedJarPath = null;
        }

        // Nothing configured
        if (jarPath == null || jarPath.isBlank()) {
            return;
        }

        // We call loadValidatorFromJar each time because JarLoader checks the timestamp
        try {
            extendedValidator = loadValidatorFromJar(new File(jarPath));
            
            // Find all @Check annotated methods
            extendedValidatorCheckMethods = new ArrayList<>();
            for (Method method : extendedValidator.getClass().getMethods()) {
                if (method.isAnnotationPresent(Check.class)) {
                    extendedValidatorCheckMethods.add(method);
                }
            }
            
            // Initialize if it extends AbstractDeclarativeValidator
            initializeExtendedValidator();
            
            extendedValidatorLoaded = true;
            loadedJarPath = jarPath;
            
        } catch (ClassNotFoundException e) {
            System.err.println("ExtendedValidator class not found in JAR: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Failed to load ExtendedValidator: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static boolean isEqualPath(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * Initialize the extended validator with necessary Xtext infrastructure
     */
    private void initializeExtendedValidator() throws Exception {
        if (extendedValidator instanceof AbstractDeclarativeValidator) {
            AbstractDeclarativeValidator validator = (AbstractDeclarativeValidator) extendedValidator;
            
            try {
                Field messageAcceptorField = AbstractDeclarativeValidator.class.getDeclaredField("messageAcceptor");
                messageAcceptorField.setAccessible(true);
                
                Field ourMessageAcceptorField = AbstractDeclarativeValidator.class.getDeclaredField("messageAcceptor");
                ourMessageAcceptorField.setAccessible(true);
                ValidationMessageAcceptor ourMessageAcceptor = (ValidationMessageAcceptor) ourMessageAcceptorField.get(this);
                
                messageAcceptorField.set(validator, ourMessageAcceptor);
            } catch (NoSuchFieldException e) {
                System.err.println("Could not access messageAcceptor field: " + e.getMessage());
            }
            
            try {
                Field chainField = AbstractDeclarativeValidator.class.getDeclaredField("chain");
                chainField.setAccessible(true);
                
                Field ourChainField = AbstractDeclarativeValidator.class.getDeclaredField("chain");
                ourChainField.setAccessible(true);
                Object ourChain = ourChainField.get(this);
                
                chainField.set(validator, ourChain);
            } catch (NoSuchFieldException e) {
                // Chain field might not exist in all versions, ignore
            }
        }
    }
    
    @Check
    public void delegateToExtendedValidator(EObject object) {
        if (extendedValidatorLoaded && extendedValidator != null) {
            invokeExtendedValidatorChecks(object);
        }
    }
    
    private void invokeExtendedValidatorChecks(EObject object) {
        for (Method method : extendedValidatorCheckMethods) {
            try {
                Class<?>[] paramTypes = method.getParameterTypes();
                
                if (paramTypes.length == 1 && paramTypes[0].isInstance(object)) {
                    setValidatorContext(extendedValidator);
                    method.invoke(extendedValidator, object);
                }
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                System.err.println("Error in ExtendedValidator check method " 
                    + method.getName() + ": " + cause.getMessage());
                cause.printStackTrace();
            } catch (Exception e) {
                System.err.println("Failed to invoke ExtendedValidator check method " 
                    + method.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void setValidatorContext(Object validator) {
        if (validator instanceof AbstractDeclarativeValidator) {
            try {
                Method setMessageAcceptorMethod = AbstractDeclarativeValidator.class
                    .getDeclaredMethod("setMessageAcceptor", ValidationMessageAcceptor.class);
                setMessageAcceptorMethod.setAccessible(true);
                
                Field ourMessageAcceptorField = AbstractDeclarativeValidator.class
                    .getDeclaredField("messageAcceptor");
                ourMessageAcceptorField.setAccessible(true);
                ValidationMessageAcceptor ourMessageAcceptor = 
                    (ValidationMessageAcceptor) ourMessageAcceptorField.get(this);
                
                setMessageAcceptorMethod.invoke(validator, ourMessageAcceptor);
            } catch (Exception e) {
                System.err.println("Warning: Could not set validation context: " + e.getMessage());
            }
        }
    }
    
}
