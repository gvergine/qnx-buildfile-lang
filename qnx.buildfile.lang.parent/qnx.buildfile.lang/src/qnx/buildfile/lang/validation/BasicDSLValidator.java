package qnx.buildfile.lang.validation;

import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;

import qnx.buildfile.lang.attributes.AttributeKeywords;
import qnx.buildfile.lang.buildfileDSL.BooleanAttribute;
import qnx.buildfile.lang.buildfileDSL.BuildfileDSLPackage;
import qnx.buildfile.lang.buildfileDSL.ValuedAttribute;


public class BasicDSLValidator extends BaseDSLValidator
{
	
	@Override
	public void register(EValidatorRegistrar registrar) {
		// Prevent duplicate registration — this validator is invoked
		// via @ComposedChecks on BuildfileDSLValidator, not directly.
	}
	
	@Check
	public void checkAttributes(BooleanAttribute booleanAttribute) {
		if (!AttributeKeywords.ALL_BOOLEAN_ATTRIBUTE_KEYWORDS
				.contains(booleanAttribute.getName()))
		{
			error("Unknown BooleanAttribute \"" + booleanAttribute.getName(),
					BuildfileDSLPackage.Literals.ATTRIBUTE__NAME,
					"invalidName");
		}

	}

	@Check
	public void checkAttributes(ValuedAttribute valuedAttribute) {
		if (!AttributeKeywords.ALL_VALUED_ATTRIBUTE_KEYWORDS
				.contains(valuedAttribute.getName()))
		{
			error("Unknown ValuedAttribute \"" + valuedAttribute.getName() + "\"",
					BuildfileDSLPackage.Literals.ATTRIBUTE__NAME,
					"invalidName");
		}
		else
		{
			AttributeValueChecker.check(valuedAttribute,this);
		}

	}

}
