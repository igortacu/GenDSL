package elsd.ast;

import java.util.List;
import java.util.ArrayList;

// base class for all AST nodes
public abstract class ASTNode {

    // source location for error messages
    public int line;
    public int col;

    // visitor pattern accept method
    public abstract <T> T accept(ASTVisitor<T> visitor);

    // top level

    // root node, holds all statements
    public static class Program extends ASTNode {
        public final List<ASTNode> statements = new ArrayList<>();
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitProgram(this); }
    }

    // declarations

    // variable declaration like: gene myGene = value
    public static class Declaration extends ASTNode {
        public String type;
        public List<String> ids;
        public Expression initValue;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitDeclaration(this); }
    }

    // assignments

    // simple assignment: field id = expr
    public static class AssignExpr extends ASTNode {
        public String field;
        public String id;
        public Expression value;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitAssignExpr(this); }
    }

    // multi assignment: field id1 id2 = expr1 expr2
    public static class AssignMulti extends ASTNode {
        public String field;
        public List<String> ids;
        public List<Expression> values;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitAssignMulti(this); }
    }

    // dominance assignment: A dominant over a
    public static class AssignDominance extends ASTNode {
        public String dominant;
        public String recessive;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitAssignDominance(this); }
    }

    // expressions

    // base class for all expressions
    public static abstract class Expression extends ASTNode {}

    // number like 42 or 3.14
    public static class NumberLiteral extends Expression {
        public String value;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitNumberLiteral(this); }
    }

    // string like "hello"
    public static class StringLiteral extends Expression {
        public String value;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitStringLiteral(this); }
    }

    // true or false
    public static class BooleanLiteral extends Expression {
        public boolean value;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitBooleanLiteral(this); }
    }

    // variable reference
    public static class Identifier extends Expression {
        public String name;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitIdentifier(this); }
    }

    // unary op like -x or not x
    public static class UnaryExpr extends Expression {
        public String op;
        public Expression operand;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitUnaryExpr(this); }
    }

    // binary op like x + y or x * y
    public static class BinaryExpr extends Expression {
        public String op;
        public Expression left;
        public Expression right;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitBinaryExpr(this); }
    }

    // event used as expression
    public static class EventExpr extends Expression {
        public Event event;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitEventExpr(this); }
    }

    // conditions

    // base class for conditions
    public static abstract class Condition extends ASTNode {}

    // comparison like x > 5
    public static class CompareCondition extends Condition {
        public Expression left;
        public String op;
        public Expression right;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitCompareCondition(this); }
    }

    // logical and/or
    public static class LogicalCondition extends Condition {
        public String op;
        public Condition left;
        public Condition right;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitLogicalCondition(this); }
    }

    // not condition
    public static class NotCondition extends Condition {
        public Condition operand;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitNotCondition(this); }
    }

    // flow structures

    // if / elif / else
    public static class IfStatement extends ASTNode {
        public final List<ConditionBlock> branches = new ArrayList<>();
        public List<ASTNode> elseBody;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitIfStatement(this); }
    }

    // one branch in an if/elif chain
    public static class ConditionBlock {
        public Condition condition;
        public List<ASTNode> body;
    }

    // ternary: condition ? stmt1 : stmt2
    public static class TernaryStatement extends ASTNode {
        public Condition condition;
        public ASTNode thenBranch;
        public ASTNode elseBranch;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitTernaryStatement(this); }
    }

    // while loop
    public static class WhileStatement extends ASTNode {
        public Condition condition;
        public List<ASTNode> body;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitWhileStatement(this); }
    }

    // for loop
    public static class ForStatement extends ASTNode {
        public String variable;
        public List<Expression> iterable;
        public List<ASTNode> body;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitForStatement(this); }
    }

    // computations

    // find field of gene in generation
    public static class FindExpr extends ASTNode {
        public String field;
        public String id;
        public Integer generation;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitFindExpr(this); }
    }

    // cross two parents to get offspring
    public static class CrossExpr extends ASTNode {
        public String parent1;
        public String parent2;
        public String offspring;
        public List<Expression> ratios;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitCrossExpr(this); }
    }

    // predict traits for genes
    public static class PredExpr extends ASTNode {
        public List<String> ids;
        public Integer generation;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitPredExpr(this); }
    }

    // estimate a value with optional confidence
    public static class EstimateExpr extends ASTNode {
        public String id;
        public String value;
        public String confidence;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitEstimateExpr(this); }
    }

    // infer parents or field from data
    public static class InferExpr extends ASTNode {
        public boolean inferParents;
        public String field;
        public String sourceId;
        public List<String> additionalIds;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitInferExpr(this); }
    }

    // probability of events, optionally conditional
    public static class ProbExpr extends ASTNode {
        public List<Event> events;
        public List<Event> givenEvents;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitProbExpr(this); }
    }

    // linkage between genes
    public static class LinkExpr extends ASTNode {
        public List<String> ids;
        public String recombination;
        public String distance;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitLinkExpr(this); }
    }

    // sex-linked trait
    public static class SexExpr extends ASTNode {
        public String id;
        public String field;
        public Expression value;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitSexExpr(this); }
    }

    // blood group analysis
    public static class BloodExpr extends ASTNode {
        public List<String> ids;
        public String system;
        public List<Expression> phenotypes;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitBloodExpr(this); }
    }

    // events

    // an event like phenotype(X) or carries(X, alleles)
    public static class Event extends ASTNode {
        public String kind;
        public String id;
        public List<String> alleles;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitEvent(this); }
    }

    // io

    // print statement
    public static class PrintStatement extends ASTNode {
        public String field;
        public String targetId;
        public boolean printAll;
        public List<Expression> expressions;
        public List<Event> events;
        @Override public <T> T accept(ASTVisitor<T> v) { return v.visitPrintStatement(this); }
    }
}
