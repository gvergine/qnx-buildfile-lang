package qnx.buildfile.lang.formatting2;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.formatting2.AbstractFormatter2;
import org.eclipse.xtext.formatting2.IFormattableDocument;
import org.eclipse.xtext.formatting2.IHiddenRegionFormatter;
import org.eclipse.xtext.formatting2.regionaccess.ISemanticRegion;
import org.eclipse.xtext.resource.XtextResource;

import com.google.inject.Inject;

import qnx.buildfile.lang.buildfileDSL.Attribute;
import qnx.buildfile.lang.buildfileDSL.AttributeSection;
import qnx.buildfile.lang.buildfileDSL.AttributeStatement;
import qnx.buildfile.lang.buildfileDSL.BooleanAttribute;
import qnx.buildfile.lang.buildfileDSL.DeploymentStatement;
import qnx.buildfile.lang.buildfileDSL.Model;
import qnx.buildfile.lang.buildfileDSL.Statement;
import qnx.buildfile.lang.buildfileDSL.ValuedAttribute;
import qnx.buildfile.lang.services.BuildfileDSLGrammarAccess;

/**
 * Formatter for BuildfileDSL.
 * <p>
 * Formatting rules:
 * <ul>
 *   <li>No leading whitespace before statements</li>
 *   <li>No space after {@code [} and before {@code ]}</li>
 *   <li>Single space between attributes inside {@code [...]}</li>
 *   <li>No spaces around {@code =} in valued attributes ({@code uid=0} not {@code uid = 0})</li>
 *   <li>No space between {@code +}/{@code -} and boolean attribute name</li>
 *   <li>Single space between {@code ]} and the deployment path</li>
 *   <li>No spaces around {@code =} in deployment assignments</li>
 * </ul>
 */
public class BuildfileDSLFormatter extends AbstractFormatter2 {

	@Inject
	private BuildfileDSLGrammarAccess grammar;
	@Override
	public void format(Object obj, IFormattableDocument doc) {

	    if (obj instanceof XtextResource) {
	        XtextResource resource = (XtextResource) obj;

	        for (EObject eObject : resource.getContents()) {
	            format(eObject, doc);
	        }
	        return;
	    }

	    if (obj instanceof Model) {
	        formatModel((Model) obj, doc);
	        return;
	    }

	    if (obj instanceof AttributeStatement) {
	        formatAttributeStatement((AttributeStatement) obj, doc);
	        return;
	    }

	    if (obj instanceof DeploymentStatement) {
	        formatDeploymentStatement((DeploymentStatement) obj, doc);
	        return;
	    }
	}


	private void formatModel(Model model, IFormattableDocument doc) {
		for (Statement statement : model.getStatements()) {
			format(statement, doc);
		}
	}

	private void formatAttributeStatement(AttributeStatement attributeStatement, IFormattableDocument doc) {

		if (attributeStatement.getAttributesection() != null) {
			// Remove leading whitespace before the statement
			ISemanticRegion openingBracket = textRegionExtensions.regionFor(attributeStatement.getAttributesection())
					.keyword(grammar.getAttributeSectionAccess().getLeftSquareBracketKeyword_0());
			if (openingBracket != null) {
				doc.prepend(openingBracket, (IHiddenRegionFormatter f) -> f.noSpace());
			}

			formatAttributeSection(attributeStatement.getAttributesection(), doc);
		}
	}

	private void formatDeploymentStatement(DeploymentStatement deploymentStatement, IFormattableDocument doc) {
		if (deploymentStatement.getAttributesection() != null) {
			// Remove leading whitespace before the statement
			ISemanticRegion openingBracket = textRegionExtensions.regionFor(deploymentStatement.getAttributesection())
					.keyword(grammar.getAttributeSectionAccess().getLeftSquareBracketKeyword_0());
			if (openingBracket != null) {
				doc.prepend(openingBracket, (IHiddenRegionFormatter f) -> f.noSpace());
			}

			formatAttributeSection(deploymentStatement.getAttributesection(), doc);

			// Single space between ] and path
			ISemanticRegion closingBracket = textRegionExtensions.regionFor(deploymentStatement.getAttributesection())
					.keyword(grammar.getAttributeSectionAccess().getRightSquareBracketKeyword_2());
			if (closingBracket != null) {
				doc.append(closingBracket, (IHiddenRegionFormatter f) -> f.oneSpace());
			}
		} else if (deploymentStatement.getPath() != null) {
			// No attribute section — remove leading whitespace before the path
			ISemanticRegion pathRegion = textRegionExtensions.regionFor(deploymentStatement)
					.assignment(grammar.getDeploymentStatementAccess().getPathAssignment_1());
			if (pathRegion != null) {
				doc.prepend(pathRegion, (IHiddenRegionFormatter f) -> f.noSpace());
			}
		}

		// No spaces around = in deployment assignment
		if (deploymentStatement.isAssignment()) {
			ISemanticRegion equalsSign = textRegionExtensions.regionFor(deploymentStatement)
					.keyword(grammar.getDeploymentStatementAccess().getAssignmentEqualsSignKeyword_2_0_0());
			if (equalsSign != null) {
				doc.prepend(equalsSign, (IHiddenRegionFormatter f) -> f.noSpace());
				doc.append(equalsSign, (IHiddenRegionFormatter f) -> f.noSpace());
			}
		}
	}

	private void formatAttributeSection(AttributeSection section, IFormattableDocument doc) {
		// No space after [
		ISemanticRegion openingBracket = textRegionExtensions.regionFor(section)
				.keyword(grammar.getAttributeSectionAccess().getLeftSquareBracketKeyword_0());
		if (openingBracket != null) {
			doc.append(openingBracket, (IHiddenRegionFormatter f) -> f.noSpace());
		}

		// No space before ]
		ISemanticRegion closingBracket = textRegionExtensions.regionFor(section)
				.keyword(grammar.getAttributeSectionAccess().getRightSquareBracketKeyword_2());
		if (closingBracket != null) {
			doc.prepend(closingBracket, (IHiddenRegionFormatter f) -> f.noSpace());
		}

		// Format each attribute
		List<Attribute> attributes = section.getAttributes();
		for (int i = 0; i < attributes.size(); i++) {
			Attribute attr = attributes.get(i);

			// Single space before each attribute (except the first)
			if (i > 0) {
				List<ISemanticRegion> regions = new ArrayList<>();
				textRegionExtensions.allSemanticRegions((EObject) attr).forEach(regions::add);
				if (!regions.isEmpty()) {
					doc.prepend(regions.get(0), (IHiddenRegionFormatter f) -> f.oneSpace());
				}
			}

			if (attr instanceof ValuedAttribute) {
				formatValuedAttribute((ValuedAttribute) attr, doc);
			} else if (attr instanceof BooleanAttribute) {
				formatBooleanAttribute((BooleanAttribute) attr, doc);
			}
		}
	}

	private void formatValuedAttribute(ValuedAttribute attr, IFormattableDocument doc) {
		// No spaces around = in valued attributes: uid=0 not uid = 0
		ISemanticRegion equalsSign = textRegionExtensions.regionFor(attr)
				.keyword(grammar.getValuedAttributeAccess().getEqualsSignKeyword_1());
		if (equalsSign != null) {
			doc.prepend(equalsSign, (IHiddenRegionFormatter f) -> f.noSpace());
			doc.append(equalsSign, (IHiddenRegionFormatter f) -> f.noSpace());
		}
	}

	private void formatBooleanAttribute(BooleanAttribute attr, IFormattableDocument doc) {
		// No space between +/- and the attribute name
		ISemanticRegion nameRegion = textRegionExtensions.regionFor(attr)
				.assignment(grammar.getBooleanAttributeAccess().getNameAssignment_1());
		if (nameRegion != null) {
			doc.prepend(nameRegion, (IHiddenRegionFormatter f) -> f.noSpace());
		}
	}
}