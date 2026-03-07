package elsd.ast;

import elsd.ast.ASTNode.*;
import elsd.generated.ELSDParser;
import elsd.generated.ELSDParserBaseVisitor;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// converts the ANTLR parse tree (CST) into a clean AST
// extends the generated base visitor so each labeled alternative gets its own method
public class ASTBuilder extends ELSDParserBaseVisitor<ASTNode> {

    // sets line and column on a node from a token
    private void setLocation(ASTNode node, Token token) {
        node.line = token.getLine();
        node.col  = token.getCharPositionInLine();
    }

    // extracts list of identifier names from idList rule
    private List<String> extractIdList(ELSDParser.IdListContext ctx) {
        return ctx.ID().stream()
                .map(TerminalNode::getText)
                .collect(Collectors.toList());
    }

    // extracts list of expressions from exprList rule
    private List<Expression> extractExprList(ELSDParser.ExprListContext ctx) {
        return ctx.expression().stream()
                .map(e -> (Expression) visit(e))
                .collect(Collectors.toList());
    }

    // gets field text or null if not present
    private String fieldText(ELSDParser.FieldContext ctx) {
        return ctx == null ? null : ctx.getText();
    }

    // extracts generation number or null
    private Integer extractGenTag(ELSDParser.GenTagContext ctx) {
        return ctx == null ? null : Integer.parseInt(ctx.NUMBER().getText());
    }

    // builds an Event node from an event context
    private Event buildEvent(ELSDParser.EventContext ctx) {
        Event ev = new Event();
        setLocation(ev, ctx.getStart());
        if (ctx instanceof ELSDParser.EventPhenotypeContext) {
            ev.kind = "phenotype";
            ev.id = ((ELSDParser.EventPhenotypeContext) ctx).ID().getText();
        } else if (ctx instanceof ELSDParser.EventGenotypeContext) {
            ev.kind = "genotype";
            ev.id = ((ELSDParser.EventGenotypeContext) ctx).ID().getText();
        } else if (ctx instanceof ELSDParser.EventCarriesContext) {
            ELSDParser.EventCarriesContext c = (ELSDParser.EventCarriesContext) ctx;
            ev.kind = "carries";
            ev.id = c.ID().getText();
            ev.alleles = c.alleleList().allele().stream()
                    .map(a -> a.ID().getText())
                    .collect(Collectors.toList());
        }
        return ev;
    }

    // builds list of events from eventList context
    private List<Event> buildEventList(ELSDParser.EventListContext ctx) {
        return ctx.event().stream()
                .map(this::buildEvent)
                .collect(Collectors.toList());
    }

    // program - the root rule
    @Override
    public ASTNode visitProgram(ELSDParser.ProgramContext ctx) {
        Program prog = new Program();
        setLocation(prog, ctx.getStart());
        for (ELSDParser.StatementContext sc : ctx.statementList().statement()) {
            prog.statements.add(visit(sc));
        }
        return prog;
    }

    // statement - dispatches to the right sub-rule

    @Override
    public ASTNode visitStatement(ELSDParser.StatementContext ctx) {
        if (ctx.declaration()  != null) return visit(ctx.declaration());
        if (ctx.assignment()   != null) return visit(ctx.assignment());
        if (ctx.flowStructure()!= null) return visit(ctx.flowStructure());
        if (ctx.computation()  != null) return visit(ctx.computation());
        if (ctx.io()           != null) return visit(ctx.io());
        throw new RuntimeException("Unknown statement at line " + ctx.getStart().getLine());
    }

    // ═════════════════════════════════════════════════════════════════
    //  R4. declaration
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitDeclaration(ELSDParser.DeclarationContext ctx) {
        Declaration decl = new Declaration();
        setLocation(decl, ctx.getStart());
        decl.type = ctx.type().getText();
        decl.ids  = extractIdList(ctx.idList());
        if (ctx.expression() != null) {
            decl.initValue = (Expression) visit(ctx.expression());
        }
        return decl;
    }

    // ═════════════════════════════════════════════════════════════════
    //  R14. assignments
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitAssignExpr(ELSDParser.AssignExprContext ctx) {
        AssignExpr node = new AssignExpr();
        setLocation(node, ctx.getStart());
        node.field = fieldText(ctx.field());
        node.id    = ctx.ID().getText();
        node.value = (Expression) visit(ctx.expression());
        return node;
    }

    @Override
    public ASTNode visitAssignComputation(ELSDParser.AssignComputationContext ctx) {
        // Treat as AssignExpr — the computation result is stored in the id
        AssignExpr node = new AssignExpr();
        setLocation(node, ctx.getStart());
        node.field = fieldText(ctx.field());
        node.id    = ctx.ID().getText();
        // wrap the computation result; visit returns the computation ASTNode
        node.value = null;  // computation is a statement, not an expression
        return node;
    }

    @Override
    public ASTNode visitAssignMulti(ELSDParser.AssignMultiContext ctx) {
        AssignMulti node = new AssignMulti();
        setLocation(node, ctx.getStart());
        node.field  = fieldText(ctx.field());
        node.ids    = extractIdList(ctx.idList());
        node.values = extractExprList(ctx.exprList());
        return node;
    }

    @Override
    public ASTNode visitAssignDominance(ELSDParser.AssignDominanceContext ctx) {
        AssignDominance node = new AssignDominance();
        setLocation(node, ctx.getStart());
        node.dominant  = ctx.ID(0).getText();
        node.recessive = ctx.ID(1).getText();
        return node;
    }

    // ═════════════════════════════════════════════════════════════════
    //  R17. conditions
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitCondCompare(ELSDParser.CondCompareContext ctx) {
        CompareCondition cond = new CompareCondition();
        setLocation(cond, ctx.getStart());
        cond.left  = (Expression) visit(ctx.expression(0));
        cond.op    = ctx.operator().getText();
        cond.right = (Expression) visit(ctx.expression(1));
        return cond;
    }

    @Override
    public ASTNode visitCondAnd(ELSDParser.CondAndContext ctx) {
        LogicalCondition cond = new LogicalCondition();
        setLocation(cond, ctx.getStart());
        cond.op    = "and";
        cond.left  = (Condition) visit(ctx.condition(0));
        cond.right = (Condition) visit(ctx.condition(1));
        return cond;
    }

    @Override
    public ASTNode visitCondOr(ELSDParser.CondOrContext ctx) {
        LogicalCondition cond = new LogicalCondition();
        setLocation(cond, ctx.getStart());
        cond.op    = "or";
        cond.left  = (Condition) visit(ctx.condition(0));
        cond.right = (Condition) visit(ctx.condition(1));
        return cond;
    }

    @Override
    public ASTNode visitCondNot(ELSDParser.CondNotContext ctx) {
        NotCondition cond = new NotCondition();
        setLocation(cond, ctx.getStart());
        cond.operand = (Condition) visit(ctx.condition());
        return cond;
    }

    @Override
    public ASTNode visitCondParen(ELSDParser.CondParenContext ctx) {
        return visit(ctx.condition());
    }

    // ═════════════════════════════════════════════════════════════════
    //  R18–R20. expressions
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitExprNumber(ELSDParser.ExprNumberContext ctx) {
        NumberLiteral lit = new NumberLiteral();
        setLocation(lit, ctx.getStart());
        lit.value = ctx.NUMBER().getText();
        return lit;
    }

    @Override
    public ASTNode visitExprString(ELSDParser.ExprStringContext ctx) {
        StringLiteral lit = new StringLiteral();
        setLocation(lit, ctx.getStart());
        String raw = ctx.STRING_LITERAL().getText();
        lit.value = raw.substring(1, raw.length() - 1); // strip quotes
        return lit;
    }

    @Override
    public ASTNode visitExprTrue(ELSDParser.ExprTrueContext ctx) {
        BooleanLiteral lit = new BooleanLiteral();
        setLocation(lit, ctx.getStart());
        lit.value = true;
        return lit;
    }

    @Override
    public ASTNode visitExprFalse(ELSDParser.ExprFalseContext ctx) {
        BooleanLiteral lit = new BooleanLiteral();
        setLocation(lit, ctx.getStart());
        lit.value = false;
        return lit;
    }

    @Override
    public ASTNode visitExprId(ELSDParser.ExprIdContext ctx) {
        Identifier id = new Identifier();
        setLocation(id, ctx.getStart());
        id.name = ctx.ID().getText();
        return id;
    }

    @Override
    public ASTNode visitExprUnaryMinus(ELSDParser.ExprUnaryMinusContext ctx) {
        UnaryExpr expr = new UnaryExpr();
        setLocation(expr, ctx.getStart());
        expr.op = "-";
        expr.operand = (Expression) visit(ctx.expression());
        return expr;
    }

    @Override
    public ASTNode visitExprNot(ELSDParser.ExprNotContext ctx) {
        UnaryExpr expr = new UnaryExpr();
        setLocation(expr, ctx.getStart());
        expr.op = "not";
        expr.operand = (Expression) visit(ctx.expression());
        return expr;
    }

    @Override
    public ASTNode visitExprMulDiv(ELSDParser.ExprMulDivContext ctx) {
        BinaryExpr expr = new BinaryExpr();
        setLocation(expr, ctx.getStart());
        expr.op    = ctx.getChild(1).getText(); // "*" or "/"
        expr.left  = (Expression) visit(ctx.expression(0));
        expr.right = (Expression) visit(ctx.expression(1));
        return expr;
    }

    @Override
    public ASTNode visitExprAddSub(ELSDParser.ExprAddSubContext ctx) {
        BinaryExpr expr = new BinaryExpr();
        setLocation(expr, ctx.getStart());
        expr.op    = ctx.getChild(1).getText(); // "+" or "-"
        expr.left  = (Expression) visit(ctx.expression(0));
        expr.right = (Expression) visit(ctx.expression(1));
        return expr;
    }

    @Override
    public ASTNode visitExprParen(ELSDParser.ExprParenContext ctx) {
        return visit(ctx.expression());  // unwrap parentheses
    }

    @Override
    public ASTNode visitExprEvent(ELSDParser.ExprEventContext ctx) {
        EventExpr expr = new EventExpr();
        setLocation(expr, ctx.getStart());
        expr.event = buildEvent(ctx.event());
        return expr;
    }

    // ═════════════════════════════════════════════════════════════════
    //  R16. flow structures
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitIfStatement(ELSDParser.IfStatementContext ctx) {
        IfStatement node = new IfStatement();
        setLocation(node, ctx.getStart());

        // First "if" branch
        int condIdx = 0;
        ConditionBlock mainBranch = new ConditionBlock();
        mainBranch.condition = (Condition) visit(ctx.condition(condIdx));
        mainBranch.body = buildStatementList(ctx.statementList(condIdx));
        node.branches.add(mainBranch);
        condIdx++;

        // "elif" branches
        int stmtListIdx = 1;
        for (int i = 0; i < ctx.ELIF().size(); i++) {
            ConditionBlock elif = new ConditionBlock();
            elif.condition = (Condition) visit(ctx.condition(condIdx));
            elif.body = buildStatementList(ctx.statementList(stmtListIdx));
            node.branches.add(elif);
            condIdx++;
            stmtListIdx++;
        }

        // "else" branch
        if (ctx.ELSE() != null) {
            node.elseBody = buildStatementList(ctx.statementList(stmtListIdx));
        }

        return node;
    }

    @Override
    public ASTNode visitTernaryStatement(ELSDParser.TernaryStatementContext ctx) {
        TernaryStatement node = new TernaryStatement();
        setLocation(node, ctx.getStart());
        node.condition  = (Condition) visit(ctx.condition());
        node.thenBranch = visit(ctx.statement(0));
        node.elseBranch = visit(ctx.statement(1));
        return node;
    }

    @Override
    public ASTNode visitWhileStatement(ELSDParser.WhileStatementContext ctx) {
        WhileStatement node = new WhileStatement();
        setLocation(node, ctx.getStart());
        node.condition = (Condition) visit(ctx.condition());
        node.body = buildStatementList(ctx.statementList());
        return node;
    }

    @Override
    public ASTNode visitForStatement(ELSDParser.ForStatementContext ctx) {
        ForStatement node = new ForStatement();
        setLocation(node, ctx.getStart());
        node.variable = ctx.ID().getText();
        node.iterable = extractExprList(ctx.exprList());
        node.body = buildStatementList(ctx.statementList());
        return node;
    }

    /** Helper to convert a statementList context into a list of AST nodes. */
    private List<ASTNode> buildStatementList(ELSDParser.StatementListContext ctx) {
        List<ASTNode> stmts = new ArrayList<>();
        for (ELSDParser.StatementContext sc : ctx.statement()) {
            stmts.add(visit(sc));
        }
        return stmts;
    }

    // ═════════════════════════════════════════════════════════════════
    //  R26. computations (dispatch)
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitComputation(ELSDParser.ComputationContext ctx) {
        if (ctx.findExpr()  != null) return visit(ctx.findExpr());
        if (ctx.crossExpr() != null) return visit(ctx.crossExpr());
        if (ctx.predExpr()  != null) return visit(ctx.predExpr());
        if (ctx.estExpr()   != null) return visit(ctx.estExpr());
        if (ctx.inferExpr() != null) return visit(ctx.inferExpr());
        if (ctx.probExpr()  != null) return visit(ctx.probExpr());
        if (ctx.linkExpr()  != null) return visit(ctx.linkExpr());
        if (ctx.sexExpr()   != null) return visit(ctx.sexExpr());
        if (ctx.bloodExpr() != null) return visit(ctx.bloodExpr());
        throw new RuntimeException("Unknown computation at line " + ctx.getStart().getLine());
    }

    // ═════════════════════════════════════════════════════════════════
    //  R27. find
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitFindExpr(ELSDParser.FindExprContext ctx) {
        FindExpr node = new FindExpr();
        setLocation(node, ctx.getStart());
        node.field = fieldText(ctx.field());
        node.id    = ctx.ID().getText();
        node.generation = extractGenTag(ctx.genTag());
        return node;
    }

    // ═════════════════════════════════════════════════════════════════
    //  R28. cross
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitCrossExpr(ELSDParser.CrossExprContext ctx) {
        CrossExpr node = new CrossExpr();
        setLocation(node, ctx.getStart());
        node.parent1   = ctx.ID(0).getText();
        node.parent2   = ctx.ID(1).getText();
        node.offspring = ctx.ID(2).getText();
        if (ctx.exprList() != null) {
            node.ratios = extractExprList(ctx.exprList());
        }
        return node;
    }

    // ═════════════════════════════════════════════════════════════════
    //  R29. pred
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitPredExpr(ELSDParser.PredExprContext ctx) {
        PredExpr node = new PredExpr();
        setLocation(node, ctx.getStart());
        node.ids = extractIdList(ctx.idList());
        node.generation = extractGenTag(ctx.genTag());
        return node;
    }

    // ═════════════════════════════════════════════════════════════════
    //  R30. estimate
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitEstExpr(ELSDParser.EstExprContext ctx) {
        EstimateExpr node = new EstimateExpr();
        setLocation(node, ctx.getStart());
        node.id    = ctx.ID().getText();
        node.value = ctx.NUMBER(0).getText();
        if (ctx.CONFIDENCE() != null) {
            node.confidence = ctx.NUMBER(1).getText();
        }
        return node;
    }

    // ═════════════════════════════════════════════════════════════════
    //  R31. infer
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitInferParents(ELSDParser.InferParentsContext ctx) {
        InferExpr node = new InferExpr();
        setLocation(node, ctx.getStart());
        node.inferParents = true;
        node.sourceId = ctx.ID().getText();
        node.additionalIds = ctx.idList() != null ? extractIdList(ctx.idList()) : new ArrayList<>();
        return node;
    }

    @Override
    public ASTNode visitInferField(ELSDParser.InferFieldContext ctx) {
        InferExpr node = new InferExpr();
        setLocation(node, ctx.getStart());
        node.inferParents = false;
        node.field    = fieldText(ctx.field());
        node.sourceId = ctx.ID().getText();
        node.additionalIds = ctx.idList() != null ? extractIdList(ctx.idList()) : new ArrayList<>();
        return node;
    }

    // ═════════════════════════════════════════════════════════════════
    //  R32. probability
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitProbSimple(ELSDParser.ProbSimpleContext ctx) {
        ProbExpr node = new ProbExpr();
        setLocation(node, ctx.getStart());
        node.events = new ArrayList<>();
        node.events.add(buildEvent(ctx.event()));
        return node;
    }

    @Override
    public ASTNode visitProbConditionalSingle(ELSDParser.ProbConditionalSingleContext ctx) {
        ProbExpr node = new ProbExpr();
        setLocation(node, ctx.getStart());
        node.events = new ArrayList<>();
        node.events.add(buildEvent(ctx.event(0)));
        node.givenEvents = new ArrayList<>();
        node.givenEvents.add(buildEvent(ctx.event(1)));
        return node;
    }

    @Override
    public ASTNode visitProbConditionalMulti(ELSDParser.ProbConditionalMultiContext ctx) {
        ProbExpr node = new ProbExpr();
        setLocation(node, ctx.getStart());
        node.events = new ArrayList<>();
        node.events.add(buildEvent(ctx.event()));
        node.givenEvents = buildEventList(ctx.eventList());
        return node;
    }

    @Override
    public ASTNode visitProbMulti(ELSDParser.ProbMultiContext ctx) {
        ProbExpr node = new ProbExpr();
        setLocation(node, ctx.getStart());
        node.events = buildEventList(ctx.eventList());
        return node;
    }

    // ═════════════════════════════════════════════════════════════════
    //  R33. linkage
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitLinkPair(ELSDParser.LinkPairContext ctx) {
        LinkExpr node = new LinkExpr();
        setLocation(node, ctx.getStart());
        node.ids = List.of(ctx.ID(0).getText(), ctx.ID(1).getText());
        node.recombination = ctx.NUMBER().getText();
        return node;
    }

    @Override
    public ASTNode visitLinkMulti(ELSDParser.LinkMultiContext ctx) {
        LinkExpr node = new LinkExpr();
        setLocation(node, ctx.getStart());
        node.ids = extractIdList(ctx.idList());
        node.recombination = ctx.NUMBER(0).getText();
        if (ctx.DISTANCE() != null) {
            node.distance = ctx.NUMBER(1).getText();
        }
        return node;
    }

    // ═════════════════════════════════════════════════════════════════
    //  R34. sex-linked
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitSexSimple(ELSDParser.SexSimpleContext ctx) {
        SexExpr node = new SexExpr();
        setLocation(node, ctx.getStart());
        node.id = ctx.ID().getText();
        return node;
    }

    @Override
    public ASTNode visitSexWithField(ELSDParser.SexWithFieldContext ctx) {
        SexExpr node = new SexExpr();
        setLocation(node, ctx.getStart());
        node.id    = ctx.ID().getText();
        node.field = fieldText(ctx.field());
        node.value = (Expression) visit(ctx.expression());
        return node;
    }

    // ═════════════════════════════════════════════════════════════════
    //  R35. blood group
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitBloodSingle(ELSDParser.BloodSingleContext ctx) {
        BloodExpr node = new BloodExpr();
        setLocation(node, ctx.getStart());
        node.ids    = List.of(ctx.ID().getText());
        node.system = ctx.bloodsys().getText();
        return node;
    }

    @Override
    public ASTNode visitBloodMulti(ELSDParser.BloodMultiContext ctx) {
        BloodExpr node = new BloodExpr();
        setLocation(node, ctx.getStart());
        node.ids    = extractIdList(ctx.idList());
        node.system = ctx.bloodsys().getText();
        if (ctx.exprList() != null) {
            node.phenotypes = extractExprList(ctx.exprList());
        }
        return node;
    }

    // ═════════════════════════════════════════════════════════════════
    //  R36. events (called via buildEvent helper, but also handle
    //       direct visits for event used as expression)
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitEventPhenotype(ELSDParser.EventPhenotypeContext ctx) {
        return buildEvent(ctx);
    }

    @Override
    public ASTNode visitEventGenotype(ELSDParser.EventGenotypeContext ctx) {
        return buildEvent(ctx);
    }

    @Override
    public ASTNode visitEventCarries(ELSDParser.EventCarriesContext ctx) {
        return buildEvent(ctx);
    }

    // ═════════════════════════════════════════════════════════════════
    //  R44. I/O
    // ═════════════════════════════════════════════════════════════════

    @Override
    public ASTNode visitPrintId(ELSDParser.PrintIdContext ctx) {
        PrintStatement node = new PrintStatement();
        setLocation(node, ctx.getStart());
        node.targetId = ctx.ID().getText();
        return node;
    }

    @Override
    public ASTNode visitPrintField(ELSDParser.PrintFieldContext ctx) {
        PrintStatement node = new PrintStatement();
        setLocation(node, ctx.getStart());
        node.field = fieldText(ctx.field());
        if (ctx.ID() != null) {
            node.targetId = ctx.ID().getText();
        }
        if (ctx.ALL() != null) {
            node.printAll = true;
            node.expressions = new ArrayList<>();
            node.expressions.add((Expression) visit(ctx.expression()));
        }
        return node;
    }

    @Override
    public ASTNode visitPrintExprList(ELSDParser.PrintExprListContext ctx) {
        PrintStatement node = new PrintStatement();
        setLocation(node, ctx.getStart());
        node.expressions = extractExprList(ctx.exprList());
        return node;
    }

    @Override
    public ASTNode visitPrintEventList(ELSDParser.PrintEventListContext ctx) {
        PrintStatement node = new PrintStatement();
        setLocation(node, ctx.getStart());
        node.events = buildEventList(ctx.eventList());
        return node;
    }
}
