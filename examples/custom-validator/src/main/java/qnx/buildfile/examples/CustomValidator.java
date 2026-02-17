package qnx.buildfile.examples;

import org.eclipse.xtext.validation.Check;

import qnx.buildfile.lang.buildfileDSL.AttributeSection;
import qnx.buildfile.lang.buildfileDSL.BuildfileDSLPackage;
import qnx.buildfile.lang.buildfileDSL.DeploymentStatement;
import qnx.buildfile.lang.buildfileDSL.Path;
import qnx.buildfile.lang.buildfileDSL.ValuedAttribute;
import qnx.buildfile.lang.validation.BaseDSLValidator;

public class CustomValidator extends BaseDSLValidator
{
	/* This check raise a warning if a directory is copied from the host.
	 * This is considered an antipattern in some environments.
	 * 
	 * To check this, it is necessarty to check type=dir, but that is not enough.
	 * type=dir can be used also when creating an empty directory
	 * We also need to check that there is no source path
	 */
	@Check
	public void checkDirectories(DeploymentStatement deploymentStatement)
	{	
		if (deploymentStatement == null) return;
		if (deploymentStatement.getContent() == null || !(deploymentStatement.getContent() instanceof Path)) return;

		AttributeSection attributeSection = deploymentStatement.getAttributesection();
		if (attributeSection == null) return;

		attributeSection.getAttributes().forEach(attribute -> {
			if (attribute instanceof ValuedAttribute valuedAttribute)
			{
				if (valuedAttribute.getName().equals("type") && valuedAttribute.getValue().equals("dir"))
				{
					warning("Copying whole directories from host is considered harmful",
							deploymentStatement,  BuildfileDSLPackage.Literals.DEPLOYMENT_STATEMENT__PATH, "copiedPath");
				}
			}
		});

	}
}