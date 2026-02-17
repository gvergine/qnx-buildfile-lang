package qnx.buildfile.lang.ide;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.ide.server.symbol.DocumentSymbolMapper;

/**
 * Maps the buildfile AST to a flat list of named elements for the LSP
 * {@code textDocument/documentSymbol} request.
 * <p>
 * This extends Xtext's {@link DocumentSymbolMapper} to provide custom
 * name computation via {@link BuildfileDSLOutlineLabels}. The actual
 * conversion to {@code DocumentSymbol} (lsp4j type) is handled by
 * the Xtext framework — this class never imports lsp4j directly.
 */
public class BuildfileDSLDocumentSymbolNameProvider extends DocumentSymbolMapper.DocumentSymbolNameProvider {

    @Override
    public String getName(EObject object) {
        String name = BuildfileDSLOutlineLabels.getName(object);
        return name != null ? name : super.getName(object);
    }
}
