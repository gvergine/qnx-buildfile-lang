package qnx.buildfile.lang.ide;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.impl.DefaultResourceDescriptionStrategy;
import org.eclipse.xtext.util.IAcceptor;

import qnx.buildfile.lang.buildfileDSL.AttributeStatement;
import qnx.buildfile.lang.buildfileDSL.BooleanAttribute;
import qnx.buildfile.lang.buildfileDSL.DeploymentStatement;
import qnx.buildfile.lang.buildfileDSL.ValuedAttribute;

/**
 * Controls which buildfile elements are exported to the resource description
 * (and thus appear in document symbols / outline for LSP).
 * <p>
 * Exports:
 * <ul>
 *   <li>{@link AttributeStatement} — named by their bracket content</li>
 *   <li>{@link DeploymentStatement} — named by their target path</li>
 *   <li>{@link BooleanAttribute} and {@link ValuedAttribute} — as children</li>
 * </ul>
 */
public class BuildfileDSLResourceDescriptionStrategy extends DefaultResourceDescriptionStrategy {

    @Override
    public boolean createEObjectDescriptions(EObject eObject, IAcceptor<IEObjectDescription> acceptor) {
        String name = BuildfileDSLOutlineLabels.getName(eObject);
        if (name != null && !name.isEmpty()) {
            acceptor.accept(EObjectDescription.create(QualifiedName.create(name), eObject));
        }
        // Return true to continue descending into children
        return true;
    }
}
