package qnx.buildfile.lang.ide;

import org.eclipse.emf.ecore.EObject;

import qnx.buildfile.lang.buildfileDSL.Attribute;
import qnx.buildfile.lang.buildfileDSL.AttributeStatement;
import qnx.buildfile.lang.buildfileDSL.BooleanAttribute;
import qnx.buildfile.lang.buildfileDSL.ContentBlock;
import qnx.buildfile.lang.buildfileDSL.DeploymentStatement;
import qnx.buildfile.lang.buildfileDSL.Path;
import qnx.buildfile.lang.buildfileDSL.ValuedAttribute;

/**
 * Shared label/name computation logic for outline elements.
 * Used by both the Eclipse outline ({@code BuildfileDSLLabelProvider})
 * and indirectly by the LSP document symbol provider.
 * <p>
 * All methods are static utilities with no lsp4j or Eclipse UI dependencies.
 */
public final class BuildfileDSLOutlineLabels {

    private BuildfileDSLOutlineLabels() {}

    /**
     * Compute a display name for any buildfile AST element.
     */
    public static String getName(EObject element) {
        if (element instanceof AttributeStatement) {
            return getAttributeStatementLabel((AttributeStatement) element);
        }
        if (element instanceof DeploymentStatement) {
            return ((DeploymentStatement) element).getPath();
        }
        if (element instanceof BooleanAttribute) {
            BooleanAttribute ba = (BooleanAttribute) element;
            return (ba.isEnabled() ? "+" : "-") + ba.getName();
        }
        if (element instanceof ValuedAttribute) {
            ValuedAttribute va = (ValuedAttribute) element;
            return va.getName() + "=" + va.getValue();
        }
        return null;
    }

    /**
     * Compute a detail/description string (e.g. the assignment target).
     */
    public static String getDetail(EObject element) {
        if (element instanceof DeploymentStatement) {
            return getDeploymentDetail((DeploymentStatement) element);
        }
        return null;
    }

    public static String getAttributeStatementLabel(AttributeStatement stmt) {
        StringBuilder sb = new StringBuilder("[");
        var attrs = stmt.getAttributesection().getAttributes();
        for (int i = 0; i < attrs.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(formatAttribute(attrs.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String getDeploymentLabel(DeploymentStatement stmt) {
        StringBuilder sb = new StringBuilder(stmt.getPath());
        String detail = getDeploymentDetail(stmt);
        if (detail != null) {
            sb.append(" ").append(detail);
        }
        return sb.toString();
    }

    public static String formatAttribute(Attribute attr) {
        if (attr instanceof BooleanAttribute) {
            BooleanAttribute ba = (BooleanAttribute) attr;
            return (ba.isEnabled() ? "+" : "-") + ba.getName();
        } else if (attr instanceof ValuedAttribute) {
            ValuedAttribute va = (ValuedAttribute) attr;
            return va.getName() + "=" + va.getValue();
        }
        return "?";
    }

    private static String getDeploymentDetail(DeploymentStatement stmt) {
        if (!stmt.isAssignment() || stmt.getContent() == null) {
            return null;
        }
        if (stmt.getContent() instanceof Path) {
            return "= " + ((Path) stmt.getContent()).getValue();
        }
        if (stmt.getContent() instanceof ContentBlock) {
            return "= {...}";
        }
        return null;
    }
}
