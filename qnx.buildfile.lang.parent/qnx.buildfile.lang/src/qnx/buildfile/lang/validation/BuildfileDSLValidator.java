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

import qnx.buildfile.lang.buildfileDSL.Model;

@ComposedChecks(validators = {BasicDSLValidator.class})
public class BuildfileDSLValidator extends BaseDSLValidator
{
    private Object extendedValidator;
    private List<Method> extendedValidatorCheckMethods;
    private boolean extendedValidatorLoaded = false;
    
    @Check
    public void loadExtendedValidator(Model model) {
        System.err.println("extendedValidatorLoadAttemped");

        try {
            // Load the ExtendedValidator class
        	extendedValidator = loadValidatorFromJar(new File("/home/giovanni/git/qnx-buildfile-lang/examples/custom-validator/target/custom-validator-0.0.1-SNAPSHOT.jar"));
            
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
            System.out.println("Successfully loaded ExtendedValidator with " 
                + extendedValidatorCheckMethods.size() + " check methods");
            
        } catch (ClassNotFoundException e) {
            System.err.println("ExtendedValidator class not found in JAR: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Failed to load ExtendedValidator: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Initialize the extended validator with necessary Xtext infrastructure
     */
    private void initializeExtendedValidator() throws Exception {
        if (extendedValidator instanceof AbstractDeclarativeValidator) {
            AbstractDeclarativeValidator validator = (AbstractDeclarativeValidator) extendedValidator;
            
            // Use reflection to access and set the messageAcceptor field
            try {
                Field messageAcceptorField = AbstractDeclarativeValidator.class.getDeclaredField("messageAcceptor");
                messageAcceptorField.setAccessible(true);
                
                // Get our own messageAcceptor
                Field ourMessageAcceptorField = AbstractDeclarativeValidator.class.getDeclaredField("messageAcceptor");
                ourMessageAcceptorField.setAccessible(true);
                ValidationMessageAcceptor ourMessageAcceptor = (ValidationMessageAcceptor) ourMessageAcceptorField.get(this);
                
                // Set it on the runtime validator
                messageAcceptorField.set(validator, ourMessageAcceptor);
            } catch (NoSuchFieldException e) {
                System.err.println("Could not access messageAcceptor field: " + e.getMessage());
            }
            
            // Also try to share the chain if available
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
    
//    @Override
//    public void register(EValidatorRegistrar registrar) {
//        // Prevent duplicate registration
//    }
    
    /**
     * Alternative approach: directly invoke checks during validation
     */
    @Check
    public void delegateToExtendedValidator(EObject object) {
        if (extendedValidatorLoaded && extendedValidator != null) {
            invokeExtendedValidatorChecks(object);
        }
    }
    
    /**
     * Invoke all @Check methods from the runtime-loaded validator
     */
    private void invokeExtendedValidatorChecks(EObject object) {
        for (Method method : extendedValidatorCheckMethods) {
            try {
                Class<?>[] paramTypes = method.getParameterTypes();
                
                // Check if this method applies to the current object type
                if (paramTypes.length == 1 && paramTypes[0].isInstance(object)) {
                    // Set the context before invoking
                    setValidatorContext(extendedValidator);
                    method.invoke(extendedValidator, object);
                }
            } catch (java.lang.reflect.InvocationTargetException e) {
                // Unwrap and log the actual exception from the validator
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
    
    /**
     * Set the validation context on the runtime validator so it can report errors
     */
    private void setValidatorContext(Object validator) {
        if (validator instanceof AbstractDeclarativeValidator) {
            try {
                // Access the setMessageAcceptor method if available
                Method setMessageAcceptorMethod = AbstractDeclarativeValidator.class
                    .getDeclaredMethod("setMessageAcceptor", ValidationMessageAcceptor.class);
                setMessageAcceptorMethod.setAccessible(true);
                
                // Get our message acceptor via reflection
                Field ourMessageAcceptorField = AbstractDeclarativeValidator.class
                    .getDeclaredField("messageAcceptor");
                ourMessageAcceptorField.setAccessible(true);
                ValidationMessageAcceptor ourMessageAcceptor = 
                    (ValidationMessageAcceptor) ourMessageAcceptorField.get(this);
                
                setMessageAcceptorMethod.invoke(validator, ourMessageAcceptor);
            } catch (Exception e) {
                // If we can't set the context, the validator won't be able to report errors
                System.err.println("Warning: Could not set validation context: " + e.getMessage());
            }
        }
    }
    
}
