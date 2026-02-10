package qnx.buildfile.lang.ide;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.ide.editor.syntaxcoloring.IHighlightedPositionAcceptor;
import org.eclipse.xtext.ide.editor.syntaxcoloring.ISemanticHighlightingCalculator;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;

import qnx.buildfile.lang.buildfileDSL.AttributeSection;
import qnx.buildfile.lang.buildfileDSL.BooleanAttribute;
import qnx.buildfile.lang.buildfileDSL.ContentBlock;
import qnx.buildfile.lang.buildfileDSL.DeploymentStatement;
import qnx.buildfile.lang.buildfileDSL.ValuedAttribute;
import qnx.buildfile.lang.buildfileDSL.BuildfileDSLPackage;

/**
 * Semantic highlighting for BuildfileDSL.
 * <p>
 * Provides consistent token types that map to:
 * <ul>
 *   <li>Eclipse syntax coloring (via the UI module)</li>
 *   <li>LSP semantic tokens (used by VSCode)</li>
 * </ul>
 */
public class BuildfileDSLSemanticHighlightingCalculator implements ISemanticHighlightingCalculator {

    /** Attribute names inside [...] — e.g. uid, gid, perms, optional */
    public static final String ATTRIBUTE_NAME = "keyword";

    /** Attribute values — e.g. 0555, u, c */
    public static final String ATTRIBUTE_VALUE = "number";

    /** The + or - prefix on boolean attributes */
    public static final String BOOLEAN_MODIFIER = "keyword";

    /** The brackets [ ] themselves */
    public static final String BRACKET = "operator";

    /** The = sign in attributes and deployment statements */
    public static final String OPERATOR = "operator";

    /** Deployment paths — the source and target file paths */
    public static final String PATH = "string";

    /** Content blocks { ... } */
    public static final String CONTENT_BLOCK = "string";

    @Override
    public void provideHighlightingFor(XtextResource resource, IHighlightedPositionAcceptor acceptor,
            CancelIndicator cancelIndicator) {
        if (resource == null || resource.getParseResult() == null) {
            return;
        }

        ICompositeNode rootNode = resource.getParseResult().getRootNode();

        for (ILeafNode leaf : rootNode.getLeafNodes()) {
            if (cancelIndicator.isCanceled()) {
                return;
            }

            EObject semanticElement = leaf.getSemanticElement();
            if (semanticElement == null) {
                continue;
            }
            String text = leaf.getText();

            // Brackets [ ]
            if (semanticElement instanceof AttributeSection) {
                if ("[".equals(text) || "]".equals(text)) {
                    acceptor.addPosition(leaf.getOffset(), leaf.getLength(), BRACKET);
                }
            }

            // Boolean attribute: + or - prefix and the attribute name
            if (semanticElement instanceof BooleanAttribute) {
                if ("+".equals(text) || "-".equals(text)) {
                    acceptor.addPosition(leaf.getOffset(), leaf.getLength(), BOOLEAN_MODIFIER);
                } else if (!leaf.isHidden()) {
                    acceptor.addPosition(leaf.getOffset(), leaf.getLength(), ATTRIBUTE_NAME);
                }
            }

            // Valued attribute: name = value
            if (semanticElement instanceof ValuedAttribute) {
                if ("=".equals(text)) {
                    acceptor.addPosition(leaf.getOffset(), leaf.getLength(), OPERATOR);
                } else if (!leaf.isHidden()) {
                    ValuedAttribute attr = (ValuedAttribute) semanticElement;
                    INode nameNode = getFirstNode(attr, BuildfileDSLPackage.Literals.ATTRIBUTE__NAME);
                    if (nameNode != null && leaf.getOffset() == nameNode.getOffset()) {
                        acceptor.addPosition(leaf.getOffset(), leaf.getLength(), ATTRIBUTE_NAME);
                    } else {
                        acceptor.addPosition(leaf.getOffset(), leaf.getLength(), ATTRIBUTE_VALUE);
                    }
                }
            }

            // Deployment statement: path = content, and the = sign
            if (semanticElement instanceof DeploymentStatement) {
                if ("=".equals(text)) {
                    acceptor.addPosition(leaf.getOffset(), leaf.getLength(), OPERATOR);
                }
            }

            // Content block { ... }
            if (semanticElement instanceof ContentBlock && !leaf.isHidden()) {
                acceptor.addPosition(leaf.getOffset(), leaf.getLength(), CONTENT_BLOCK);
            }
        }

        // Highlight deployment paths using node model
        for (INode node : rootNode.getAsTreeIterable()) {
            if (cancelIndicator.isCanceled()) {
                return;
            }
            EObject element = node.getSemanticElement();
            if (element instanceof DeploymentStatement) {
                DeploymentStatement ds = (DeploymentStatement) element;
                if (ds.getPath() != null) {
                    INode pathNode = getFirstNode(ds, BuildfileDSLPackage.Literals.DEPLOYMENT_STATEMENT__PATH);
                    if (pathNode != null) {
                        acceptor.addPosition(pathNode.getOffset(), pathNode.getLength(), PATH);
                    }
                }
            }
        }
    }

    private INode getFirstNode(EObject element, EStructuralFeature feature) {
        var nodes = NodeModelUtils.findNodesForFeature(element, feature);
        return nodes.isEmpty() ? null : nodes.get(0);
    }
}
