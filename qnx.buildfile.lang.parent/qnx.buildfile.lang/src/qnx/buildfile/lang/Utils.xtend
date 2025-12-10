package qnx.buildfile.lang

import qnx.buildfile.lang.buildfileDSL.AttributeStatement
import qnx.buildfile.lang.buildfileDSL.Statement
import qnx.buildfile.lang.buildfileDSL.DeploymentStatement
import qnx.buildfile.lang.buildfileDSL.BooleanAttribute
import qnx.buildfile.lang.buildfileDSL.ValuedAttribute
import qnx.buildfile.lang.buildfileDSL.Attribute

class Utils
{
	def static AttributeStatement asAttributeStatement(Statement s)
	{
        return s as AttributeStatement;
    }
    
	def static DeploymentStatement asDeploymentStatement(Statement s)
	{
        return s as DeploymentStatement;
    }
    
	def static BooleanAttribute asBooleanAttribute(Attribute a)
	{
        return a as BooleanAttribute;
    }
    
	def static ValuedAttribute asValuedAttribute(Attribute a)
	{
        return a as ValuedAttribute;
    }
    
	def static String getName(Attribute a)
	{
		if (a instanceof BooleanAttribute)
		{
	        return a.asBooleanAttribute.name;
	    }
	    else if (a instanceof ValuedAttribute)
	    {
	    	return a.asValuedAttribute.name;
	    }
	    else
	    {
	    	throw new Exception("Unknown class " + a.class)
	    }
    }

    
    
}