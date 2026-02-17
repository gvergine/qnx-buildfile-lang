package qnx.buildfile.lang.ide;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.xtext.ide.server.codeActions.ICodeActionService2;

/**
 * LSP code action (quickfix) service for VSCode.
 * <p>
 * Implements {@link ICodeActionService2} to provide code actions for
 * validation diagnostics. Converts each suggestion into a
 * {@link CodeAction} with a {@link WorkspaceEdit}.
 * <p>
 * Handles:
 * <ul>
 *   <li>{@code invalidName} — suggests closest matching attribute keyword names
 *       using Levenshtein distance (via {@link BuildfileDSLQuickfixResolvers})</li>
 * </ul>
 */
public class BuildfileDSLCodeActionService implements ICodeActionService2 {

    @Override
    public List<Either<Command, CodeAction>> getCodeActions(Options options) {
        List<Either<Command, CodeAction>> actions = new ArrayList<>();

        if (options.getCodeActionParams() == null
                || options.getCodeActionParams().getContext() == null
                || options.getCodeActionParams().getContext().getDiagnostics() == null) {
            return actions;
        }

        String uri = options.getCodeActionParams().getTextDocument().getUri();

        for (Diagnostic diagnostic : options.getCodeActionParams().getContext().getDiagnostics()) {
            String code = diagnostic.getCode() != null ? diagnostic.getCode().getLeft() : null;

            if ("invalidName".equals(code)) {
                actions.addAll(createInvalidNameFixes(diagnostic, uri));
            }
        }

        return actions;
    }

    private List<Either<Command, CodeAction>> createInvalidNameFixes(Diagnostic diagnostic, String documentUri) {
        List<Either<Command, CodeAction>> actions = new ArrayList<>();

        String badName = BuildfileDSLQuickfixResolvers.extractBadNameFromMessage(diagnostic.getMessage());
        if (badName == null || badName.isEmpty()) {
            return actions;
        }

        var suggestions = BuildfileDSLQuickfixResolvers.suggestAttributeNames(badName);

        for (int i = 0; i < suggestions.size(); i++) {
            String replacement = suggestions.get(i).getKeyword();

            TextEdit textEdit = new TextEdit(diagnostic.getRange(), replacement);

            WorkspaceEdit workspaceEdit = new WorkspaceEdit();
            workspaceEdit.getChanges().put(documentUri, List.of(textEdit));

            CodeAction action = new CodeAction("Change to '" + replacement + "'");
            action.setKind(CodeActionKind.QuickFix);
            action.setDiagnostics(List.of(diagnostic));
            action.setEdit(workspaceEdit);
            action.setIsPreferred(i == 0);

            actions.add(Either.forRight(action));
        }

        return actions;
    }
}
