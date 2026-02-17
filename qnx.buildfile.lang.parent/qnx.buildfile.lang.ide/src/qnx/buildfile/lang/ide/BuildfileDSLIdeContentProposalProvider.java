package qnx.buildfile.lang.ide;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistEntry;
import org.eclipse.xtext.ide.editor.contentassist.IIdeContentProposalAcceptor;
import org.eclipse.xtext.ide.editor.contentassist.IdeContentProposalProvider;

import qnx.buildfile.lang.attributes.AttributeKeywords;
import qnx.buildfile.lang.buildfileDSL.ValuedAttribute;

/**
 * Content assist proposals for the LSP server (VSCode).
 * <p>
 * Mirrors the Eclipse {@code BuildfileDSLProposalProvider} by offering:
 * <ul>
 *   <li>Boolean attribute names after {@code +} or {@code -} inside {@code [...]}</li>
 *   <li>Valued attribute names (with trailing {@code =}) inside {@code [...]}</li>
 *   <li>Known values for specific attributes ({@code type}, {@code autoso}, {@code compress})</li>
 * </ul>
 */
public class BuildfileDSLIdeContentProposalProvider extends IdeContentProposalProvider {

    /** Known values for valued attributes that have a fixed set of options. */
    private static final Map<String, List<String>> KNOWN_VALUES = new LinkedHashMap<>();

    static {
        KNOWN_VALUES.put("type", Arrays.asList("link", "fifo", "file", "dir"));
        KNOWN_VALUES.put("autoso", Arrays.asList("none", "list", "add"));
        KNOWN_VALUES.put("compress", Arrays.asList("1", "2", "3"));
        KNOWN_VALUES.put("code", Arrays.asList("u", "c"));
        KNOWN_VALUES.put("data", Arrays.asList("u", "c"));
    }

    @Override
    protected void _createProposals(Assignment assignment, ContentAssistContext context,
            IIdeContentProposalAcceptor acceptor) {

        String feature = assignment.getFeature();
        String ruleName = GrammarUtil.containingRule(assignment).getName();

        if ("BooleanAttribute".equals(ruleName) && "name".equals(feature)) {
            proposeBooleanAttributeNames(context, acceptor);
            return;
        }

        if ("ValuedAttribute".equals(ruleName) && "name".equals(feature)) {
            proposeValuedAttributeNames(context, acceptor);
            return;
        }

        if ("ValuedAttribute".equals(ruleName) && "value".equals(feature)) {
            proposeAttributeValues(context, acceptor);
            return;
        }

        // Fall back to default for all other assignments (path, content, etc.)
        super._createProposals(assignment, context, acceptor);
    }

    private void proposeBooleanAttributeNames(ContentAssistContext context,
            IIdeContentProposalAcceptor acceptor) {
        for (String keyword : AttributeKeywords.ALL_BOOLEAN_ATTRIBUTE_KEYWORDS) {
            ContentAssistEntry entry = getProposalCreator().createProposal(
                    keyword, context, (e) -> {
                        e.setLabel(keyword);
                        e.setKind(ContentAssistEntry.KIND_KEYWORD);
                        e.setDescription("Boolean attribute");
                    });
            acceptor.accept(entry, getProposalPriorities().getDefaultPriority(entry));
        }
    }

    private void proposeValuedAttributeNames(ContentAssistContext context,
            IIdeContentProposalAcceptor acceptor) {
        for (String keyword : AttributeKeywords.ALL_VALUED_ATTRIBUTE_KEYWORDS) {
            String proposalText = keyword + "=";
            ContentAssistEntry entry = getProposalCreator().createProposal(
                    proposalText, context, (e) -> {
                        e.setLabel(keyword);
                        e.setKind(ContentAssistEntry.KIND_KEYWORD);
                        e.setDescription("Valued attribute");
                    });
            acceptor.accept(entry, getProposalPriorities().getDefaultPriority(entry));
        }
    }

    private void proposeAttributeValues(ContentAssistContext context,
            IIdeContentProposalAcceptor acceptor) {
        EObject model = context.getCurrentModel();
        if (model instanceof ValuedAttribute) {
            String attrName = ((ValuedAttribute) model).getName();
            if (attrName != null && KNOWN_VALUES.containsKey(attrName)) {
                for (String value : KNOWN_VALUES.get(attrName)) {
                    ContentAssistEntry entry = getProposalCreator().createProposal(
                            value, context, (e) -> {
                                e.setLabel(value);
                                e.setKind(ContentAssistEntry.KIND_VALUE);
                                e.setDescription(attrName + " value");
                            });
                    acceptor.accept(entry, getProposalPriorities().getDefaultPriority(entry));
                }
                return;
            }
        }
        // Fall back to default
        super._createProposals(
                (Assignment) context.getCurrentNode().getGrammarElement(),
                context, acceptor);
    }
}
