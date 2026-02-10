package qnx.buildfile.lang.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfiguration;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfigurationAcceptor;
import org.eclipse.xtext.ui.editor.utils.TextStyle;

/**
 * Defines the highlighting styles for BuildfileDSL in Eclipse.
 * <p>
 * The style IDs must match the token types used in
 * {@link qnx.buildfile.lang.ide.BuildfileDSLSemanticHighlightingCalculator}.
 */
public class BuildfileDSLHighlightingConfiguration implements IHighlightingConfiguration {

    // These IDs match the constants in BuildfileDSLSemanticHighlightingCalculator
    public static final String KEYWORD_ID = "keyword";
    public static final String NUMBER_ID = "number";
    public static final String STRING_ID = "string";
    public static final String OPERATOR_ID = "operator";

    @Override
    public void configure(IHighlightingConfigurationAcceptor acceptor) {
        acceptor.acceptDefaultHighlighting(KEYWORD_ID, "Attribute Name / Keyword", keywordStyle());
        acceptor.acceptDefaultHighlighting(NUMBER_ID, "Attribute Value", numberStyle());
        acceptor.acceptDefaultHighlighting(STRING_ID, "Path / Content Block", stringStyle());
        acceptor.acceptDefaultHighlighting(OPERATOR_ID, "Brackets / Operators", operatorStyle());
    }

    private TextStyle keywordStyle() {
        TextStyle style = defaultStyle();
        style.setColor(new RGB(0, 0, 192));       // dark blue
        style.setStyle(SWT.BOLD);
        return style;
    }

    private TextStyle numberStyle() {
        TextStyle style = defaultStyle();
        style.setColor(new RGB(125, 0, 160));      // purple
        return style;
    }

    private TextStyle stringStyle() {
        TextStyle style = defaultStyle();
        style.setColor(new RGB(128, 0, 0));        // dark red / brown
        return style;
    }

    private TextStyle operatorStyle() {
        TextStyle style = defaultStyle();
        style.setColor(new RGB(140, 140, 0));      // dark yellow
        style.setStyle(SWT.BOLD);
        return style;
    }

    private TextStyle defaultStyle() {
        return new TextStyle();
    }
}
