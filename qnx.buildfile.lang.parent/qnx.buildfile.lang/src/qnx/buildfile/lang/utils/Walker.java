package qnx.buildfile.lang.utils;

import qnx.buildfile.lang.buildfileDSL.AttributeSection;
import qnx.buildfile.lang.buildfileDSL.AttributeStatement;
import qnx.buildfile.lang.buildfileDSL.BooleanAttribute;
import qnx.buildfile.lang.buildfileDSL.Content;
import qnx.buildfile.lang.buildfileDSL.ContentBlock;
import qnx.buildfile.lang.buildfileDSL.DeploymentStatement;
import qnx.buildfile.lang.buildfileDSL.Model;
import qnx.buildfile.lang.buildfileDSL.Path;
import qnx.buildfile.lang.buildfileDSL.ValuedAttribute;

public class Walker
{
	private void walkAttributeSection(AttributeSection attributeSection, IWalker iwalker)
	{
		if (attributeSection == null) return;
		
		iwalker.found(attributeSection);
		
		attributeSection.getAttributes().forEach(attribute -> {
			
			if (attribute instanceof BooleanAttribute)
			{
				BooleanAttribute booleanAttribute = (BooleanAttribute) attribute;
				iwalker.found(booleanAttribute);
			}
			else if (attribute instanceof ValuedAttribute)
			{
				ValuedAttribute valuedAttribute = (ValuedAttribute) attribute;
				iwalker.found(valuedAttribute);
			}
		});
	}
	
	public void walk(Model model, IWalker iwalker)
	{
		iwalker.found(model);
		
		model.getStatements().forEach(statement -> {
			
			AttributeSection attributeSection = statement.getAttributesection();
			
			if (statement instanceof AttributeStatement)
			{
				AttributeStatement attributeStatement = (AttributeStatement) statement;
				iwalker.found(attributeStatement);
								
				walkAttributeSection(attributeSection, iwalker);
			}
			else if (statement instanceof DeploymentStatement)
			{
				DeploymentStatement deploymentStatement = (DeploymentStatement) statement;
				iwalker.found(deploymentStatement);
				
				walkAttributeSection(attributeSection, iwalker);
				
				Content content = deploymentStatement.getContent();
				
				if (content instanceof ContentBlock)
				{
					ContentBlock contentBlock = (ContentBlock) content;
					iwalker.found(contentBlock);
				}
				else if (content instanceof Path)
				{
					Path path = (Path) content;
					iwalker.found(path);
				}
			}
		});
	}

	public interface IWalker
	{
		default void found(Model model) {};
		default void found(AttributeStatement attributeStatement) {};
		default void found(DeploymentStatement deploymentStatement) {};
		default void found(AttributeSection attributeSection) {};
		default void found(BooleanAttribute booleanAttribute) {};
		default void found(ValuedAttribute valuedAttribute) {};
		default void found(ContentBlock contentBlock) {};
		default void found(Path path) {};
	}
}
