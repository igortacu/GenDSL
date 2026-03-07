package elsd.ast;

import elsd.ast.ASTNode.*;

// visitor interface for walking the AST
// implement this for interpretation, code gen, analysis, etc
public interface ASTVisitor<T> {

    // top level
    T visitProgram(Program node);

    // declarations
    T visitDeclaration(Declaration node);

    // assignments
    T visitAssignExpr(AssignExpr node);
    T visitAssignMulti(AssignMulti node);
    T visitAssignDominance(AssignDominance node);

    // expressions
    T visitNumberLiteral(NumberLiteral node);
    T visitStringLiteral(StringLiteral node);
    T visitBooleanLiteral(BooleanLiteral node);
    T visitIdentifier(Identifier node);
    T visitUnaryExpr(UnaryExpr node);
    T visitBinaryExpr(BinaryExpr node);
    T visitEventExpr(EventExpr node);

    // conditions
    T visitCompareCondition(CompareCondition node);
    T visitLogicalCondition(LogicalCondition node);
    T visitNotCondition(NotCondition node);

    // flow structures
    T visitIfStatement(IfStatement node);
    T visitTernaryStatement(TernaryStatement node);
    T visitWhileStatement(WhileStatement node);
    T visitForStatement(ForStatement node);

    // computations
    T visitFindExpr(FindExpr node);
    T visitCrossExpr(CrossExpr node);
    T visitPredExpr(PredExpr node);
    T visitEstimateExpr(EstimateExpr node);
    T visitInferExpr(InferExpr node);
    T visitProbExpr(ProbExpr node);
    T visitLinkExpr(LinkExpr node);
    T visitSexExpr(SexExpr node);
    T visitBloodExpr(BloodExpr node);

    // events
    T visitEvent(Event node);

    // io
    T visitPrintStatement(PrintStatement node);
}
