package elsd;

import elsd.ast.ASTBuilder;
import elsd.ast.ASTNode;
import elsd.ast.ASTPrinter;
import elsd.ast.ASTTreeViewer;
import elsd.generated.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.gui.TreeViewer;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ELSD – Entry-Level Scientific DSL
 *
 * Main driver that:
 *   1. Reads a .elsd source file
 *   2. Lexes it (token stream)
 *   3. Parses it (parse tree)
 *   4. Prints the token stream and the parse tree (LISP-style)
 *
 * Usage:
 *   java -cp ".:antlr-4.13.2-complete.jar" elsd.Main <file.elsd>
 *   java -cp ".:antlr-4.13.2-complete.jar" elsd.Main --tokens <file.elsd>
 *   java -cp ".:antlr-4.13.2-complete.jar" elsd.Main --ast    <file.elsd>
 *   java -cp ".:antlr-4.13.2-complete.jar" elsd.Main --gui    <file.elsd>
 */
public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java elsd.Main [--tokens] [--ast] [--ast-gui] [--gui] <file.elsd>");
            System.err.println();
            System.err.println("Options:");
            System.err.println("  --tokens   Print the token stream");
            System.err.println("  --ast      Build and print the Abstract Syntax Tree");
            System.err.println("  --ast-gui  Open the AST graphical viewer");
            System.err.println("  --gui      Open the ANTLR parse-tree GUI (requires display)");
            System.exit(1);
        }

        boolean showTokens = false;
        boolean showAst = false;
        boolean showAstGui = false;
        boolean showGui = false;
        String filePath = null;

        for (String arg : args) {
            switch (arg) {
                case "--tokens":  showTokens = true;  break;
                case "--ast":     showAst = true;     break;
                case "--ast-gui": showAstGui = true;  break;
                case "--gui":     showGui = true;     break;
                default:          filePath = arg;     break;
            }
        }

        if (filePath == null) {
            System.err.println("Error: no input file specified.");
            System.exit(1);
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            System.err.println("Error: file not found – " + filePath);
            System.exit(1);
        }

        try {
            // ── 1. Create the character stream ──────────────────────────
            CharStream input = CharStreams.fromPath(path);

            // ── 2. Lex ─────────────────────────────────────────────────
            ELSDLexer lexer = new ELSDLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            tokens.fill();

            if (showTokens) {
                printTokens(tokens, lexer);
            }

            // ── 3. Parse ───────────────────────────────────────────────
            ELSDParser parser = new ELSDParser(tokens);

            // Attach an error listener that reports problems clearly
            parser.removeErrorListeners();
            parser.addErrorListener(new DiagnosticErrorListener());

            ELSDParser.ProgramContext tree = parser.program();

            // ── 4. Results ─────────────────────────────────────────────
            int errorCount = parser.getNumberOfSyntaxErrors();
            System.out.println();
            System.out.println("═══════════════════════════════════════════");
            System.out.println("  ELSD Parse Result");
            System.out.println("═══════════════════════════════════════════");
            System.out.println("  File   : " + filePath);
            System.out.println("  Tokens : " + tokens.getTokens().size());
            System.out.println("  Errors : " + errorCount);
            System.out.println("═══════════════════════════════════════════");
            System.out.println();

            if (errorCount == 0) {
                System.out.println("✓ Parse successful – no syntax errors.");
            } else {
                System.out.println("✗ Parse completed with " + errorCount + " syntax error(s).");
            }

            // Print the LISP-style parse tree
            System.out.println();
            System.out.println("── Parse Tree (LISP) ──────────────────────");
            String lispTree = tree.toStringTree(parser);
            System.out.println(prettyPrintTree(lispTree));

            // Build and print the AST
            if (showAst) {
                System.out.println();
                System.out.println("── Abstract Syntax Tree ───────────────────");
                ASTBuilder builder = new ASTBuilder();
                ASTNode.Program ast = (ASTNode.Program) builder.visit(tree);
                ASTPrinter printer = new ASTPrinter();
                System.out.println(printer.print(ast));
            }

            // Open graphical AST viewer
            if (showAstGui) {
                System.out.println("Opening AST graphical viewer...");
                ASTBuilder builder = new ASTBuilder();
                ASTNode.Program ast = (ASTNode.Program) builder.visit(tree);
                ASTTreeViewer.show(ast);
            }

            // Optionally open the GUI inspector
            if (showGui) {
                System.out.println();
                System.out.println("Opening parse-tree GUI...");
                JFrame frame = new JFrame("ELSD Parse Tree");
                TreeViewer viewer = new TreeViewer(
                        Arrays.asList(parser.getRuleNames()), tree);
                viewer.setScale(1.2);
                JScrollPane scrollPane = new JScrollPane(viewer);
                frame.add(scrollPane);
                frame.setSize(1200, 800);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }
    }

    // ── Helper: print token stream ──────────────────────────────────────
    private static void printTokens(CommonTokenStream tokens, ELSDLexer lexer) {
        System.out.println();
        System.out.println("── Token Stream ───────────────────────────");
        System.out.printf("%-6s %-20s %-25s %s%n", "INDEX", "TOKEN TYPE", "TEXT", "LINE:COL");
        System.out.println("─".repeat(80));

        for (Token tok : tokens.getTokens()) {
            if (tok.getType() == Token.EOF) {
                System.out.printf("%-6d %-20s %-25s %d:%d%n",
                        tok.getTokenIndex(), "EOF", "<EOF>",
                        tok.getLine(), tok.getCharPositionInLine());
                break;
            }
            String typeName = lexer.getVocabulary().getSymbolicName(tok.getType());
            if (typeName == null) typeName = String.valueOf(tok.getType());

            String text = tok.getText().replace("\n", "\\n").replace("\r", "\\r");
            if (text.length() > 24) text = text.substring(0, 21) + "...";

            System.out.printf("%-6d %-20s %-25s %d:%d%n",
                    tok.getTokenIndex(), typeName, "'" + text + "'",
                    tok.getLine(), tok.getCharPositionInLine());
        }
        System.out.println();
    }

    // ── Helper: indent LISP-style tree output ───────────────────────────
    private static String prettyPrintTree(String lispTree) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        boolean afterSpace = false;

        for (int i = 0; i < lispTree.length(); i++) {
            char c = lispTree.charAt(i);
            switch (c) {
                case '(':
                    indent++;
                    sb.append('\n');
                    sb.append("  ".repeat(indent));
                    sb.append(c);
                    afterSpace = false;
                    break;
                case ')':
                    indent--;
                    sb.append(c);
                    afterSpace = false;
                    break;
                case ' ':
                    if (!afterSpace) {
                        sb.append(' ');
                    }
                    afterSpace = true;
                    break;
                default:
                    sb.append(c);
                    afterSpace = false;
                    break;
            }
        }
        return sb.toString();
    }

    // ── Custom error listener ───────────────────────────────────────────
    private static class DiagnosticErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line, int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            System.err.printf("  ERROR  line %d:%d  –  %s%n", line, charPositionInLine, msg);
        }
    }
}
