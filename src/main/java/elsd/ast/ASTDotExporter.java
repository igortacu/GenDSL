package elsd.ast;

import elsd.ast.ASTNode.*;

// exports the AST as a DOT graph format for graphviz rendering
public class ASTDotExporter implements ASTVisitor<String> {

    private int nodeId = 0;
    private StringBuilder sb = new StringBuilder();

    // generates the full DOT graph string
    public String export(ASTNode root) {
        sb.append("digraph AST {\n");
        sb.append("  rankdir=TB;\n");
        sb.append("  node [shape=box, style=\"rounded,filled\", fontname=\"Helvetica\", fontsize=11];\n");
        sb.append("  edge [arrowsize=0.7];\n");
        sb.append("\n");
        root.accept(this);
        sb.append("}\n");
        return sb.toString();
    }

    // creates a new node and returns its id
    private String newNode(String label, String color) {
        String id = "n" + (nodeId++);
        String escaped = label.replace("\"", "\\\"");
        sb.append("  ").append(id)
          .append(" [label=\"").append(escaped).append("\"")
          .append(", fillcolor=\"").append(color).append("\"];\n");
        return id;
    }

    // draws an edge between two nodes
    private void edge(String from, String to) {
        sb.append("  ").append(from).append(" -> ").append(to).append(";\n");
    }

    // colors for different node categories
    private static final String COL_ROOT = "#4A90D9";
    private static final String COL_DECL = "#7BC67E";
    private static final String COL_ASSIGN = "#F5A623";
    private static final String COL_EXPR = "#F7DC6F";
    private static final String COL_COND = "#BB8FCE";
    private static final String COL_FLOW = "#85C1E9";
    private static final String COL_COMP = "#F1948A";
    private static final String COL_IO = "#AED6F1";
    private static final String COL_EVENT = "#D5DBDB";
    private static final String COL_LIT = "#FDEBD0";

    @Override
    public String visitProgram(Program node) {
        String id = newNode("Program", COL_ROOT);
        for (ASTNode stmt : node.statements) {
            String childId = stmt.accept(this);
            edge(id, childId);
        }
        return id;
    }

    @Override
    public String visitDeclaration(Declaration node) {
        String init = node.initValue != null ? " = ..." : "";
        String id = newNode("Declare\\n" + node.type + " " + node.ids + init, COL_DECL);
        if (node.initValue != null) {
            String childId = node.initValue.accept(this);
            edge(id, childId);
        }
        return id;
    }

    @Override
    public String visitAssignExpr(AssignExpr node) {
        String f = node.field != null ? node.field + " " : "";
        String id = newNode("Assign\\n" + f + node.id, COL_ASSIGN);
        if (node.value != null) {
            String childId = node.value.accept(this);
            edge(id, childId);
        }
        return id;
    }

    @Override
    public String visitAssignMulti(AssignMulti node) {
        String f = node.field != null ? node.field + " " : "";
        String id = newNode("AssignMulti\\n" + f + node.ids, COL_ASSIGN);
        for (Expression v : node.values) {
            String childId = v.accept(this);
            edge(id, childId);
        }
        return id;
    }

    @Override
    public String visitAssignDominance(AssignDominance node) {
        return newNode("Dominance\\n" + node.dominant + " > " + node.recessive, COL_ASSIGN);
    }

    @Override
    public String visitNumberLiteral(NumberLiteral node) {
        return newNode(node.value, COL_LIT);
    }

    @Override
    public String visitStringLiteral(StringLiteral node) {
        return newNode("'" + node.value + "'", COL_LIT);
    }

    @Override
    public String visitBooleanLiteral(BooleanLiteral node) {
        return newNode(String.valueOf(node.value), COL_LIT);
    }

    @Override
    public String visitIdentifier(Identifier node) {
        return newNode(node.name, COL_EXPR);
    }

    @Override
    public String visitUnaryExpr(UnaryExpr node) {
        String id = newNode("Unary " + node.op, COL_EXPR);
        edge(id, node.operand.accept(this));
        return id;
    }

    @Override
    public String visitBinaryExpr(BinaryExpr node) {
        String id = newNode(node.op, COL_EXPR);
        edge(id, node.left.accept(this));
        edge(id, node.right.accept(this));
        return id;
    }

    @Override
    public String visitEventExpr(EventExpr node) {
        return node.event.accept(this);
    }

    @Override
    public String visitCompareCondition(CompareCondition node) {
        String id = newNode(node.op, COL_COND);
        edge(id, node.left.accept(this));
        edge(id, node.right.accept(this));
        return id;
    }

    @Override
    public String visitLogicalCondition(LogicalCondition node) {
        String id = newNode(node.op, COL_COND);
        edge(id, node.left.accept(this));
        edge(id, node.right.accept(this));
        return id;
    }

    @Override
    public String visitNotCondition(NotCondition node) {
        String id = newNode("not", COL_COND);
        edge(id, node.operand.accept(this));
        return id;
    }

    @Override
    public String visitIfStatement(IfStatement node) {
        String id = newNode("If", COL_FLOW);
        for (int i = 0; i < node.branches.size(); i++) {
            ConditionBlock b = node.branches.get(i);
            String kw = (i == 0) ? "if" : "elif";
            String branchId = newNode(kw, COL_FLOW);
            edge(id, branchId);
            edge(branchId, b.condition.accept(this));
            for (ASTNode stmt : b.body) {
                edge(branchId, stmt.accept(this));
            }
        }
        if (node.elseBody != null) {
            String elseId = newNode("else", COL_FLOW);
            edge(id, elseId);
            for (ASTNode stmt : node.elseBody) {
                edge(elseId, stmt.accept(this));
            }
        }
        return id;
    }

    @Override
    public String visitTernaryStatement(TernaryStatement node) {
        String id = newNode("Ternary", COL_FLOW);
        edge(id, node.condition.accept(this));
        edge(id, node.thenBranch.accept(this));
        edge(id, node.elseBranch.accept(this));
        return id;
    }

    @Override
    public String visitWhileStatement(WhileStatement node) {
        String id = newNode("While", COL_FLOW);
        edge(id, node.condition.accept(this));
        for (ASTNode stmt : node.body) {
            edge(id, stmt.accept(this));
        }
        return id;
    }

    @Override
    public String visitForStatement(ForStatement node) {
        String id = newNode("For " + node.variable, COL_FLOW);
        String iterId = newNode("iterable", COL_FLOW);
        edge(id, iterId);
        for (Expression e : node.iterable) {
            edge(iterId, e.accept(this));
        }
        for (ASTNode stmt : node.body) {
            edge(id, stmt.accept(this));
        }
        return id;
    }

    @Override
    public String visitFindExpr(FindExpr node) {
        String gen = node.generation != null ? "\\ngen=" + node.generation : "";
        return newNode("Find\\n" + node.field + " " + node.id + gen, COL_COMP);
    }

    @Override
    public String visitCrossExpr(CrossExpr node) {
        String id = newNode("Cross\\n" + node.parent1 + " x " + node.parent2
                + "\\n-> " + node.offspring, COL_COMP);
        if (node.ratios != null) {
            String ratioId = newNode("ratios", COL_COMP);
            edge(id, ratioId);
            for (Expression r : node.ratios) {
                edge(ratioId, r.accept(this));
            }
        }
        return id;
    }

    @Override
    public String visitPredExpr(PredExpr node) {
        String gen = node.generation != null ? "\\ngen=" + node.generation : "";
        return newNode("Predict\\n" + node.ids + gen, COL_COMP);
    }

    @Override
    public String visitEstimateExpr(EstimateExpr node) {
        String conf = node.confidence != null ? "\\nconf=" + node.confidence : "";
        return newNode("Estimate\\n" + node.id + " = " + node.value + conf, COL_COMP);
    }

    @Override
    public String visitInferExpr(InferExpr node) {
        if (node.inferParents) {
            return newNode("Infer parents\\nfrom " + node.sourceId, COL_COMP);
        }
        return newNode("Infer " + node.field + "\\nfrom " + node.sourceId, COL_COMP);
    }

    @Override
    public String visitProbExpr(ProbExpr node) {
        String id = newNode("Probability", COL_COMP);
        for (Event ev : node.events) {
            edge(id, ev.accept(this));
        }
        if (node.givenEvents != null) {
            String givenId = newNode("given", COL_COMP);
            edge(id, givenId);
            for (Event ev : node.givenEvents) {
                edge(givenId, ev.accept(this));
            }
        }
        return id;
    }

    @Override
    public String visitLinkExpr(LinkExpr node) {
        String dist = node.distance != null ? "\\ndist=" + node.distance : "";
        return newNode("Linkage\\n" + node.ids + "\\nrecomb=" + node.recombination + dist, COL_COMP);
    }

    @Override
    public String visitSexExpr(SexExpr node) {
        String extra = node.field != null ? "\\n" + node.field : "";
        return newNode("SexLinked\\n" + node.id + extra, COL_COMP);
    }

    @Override
    public String visitBloodExpr(BloodExpr node) {
        String id = newNode("BloodGroup\\n" + node.ids + " " + node.system, COL_COMP);
        if (node.phenotypes != null) {
            for (Expression e : node.phenotypes) {
                edge(id, e.accept(this));
            }
        }
        return id;
    }

    @Override
    public String visitEvent(Event node) {
        String label = node.kind + "(" + node.id;
        if (node.alleles != null) label += ", " + node.alleles;
        label += ")";
        return newNode(label, COL_EVENT);
    }

    @Override
    public String visitPrintStatement(PrintStatement node) {
        StringBuilder label = new StringBuilder("Print");
        if (node.targetId != null) label.append("\\n").append(node.targetId);
        if (node.field != null) label.append("\\n[").append(node.field).append("]");
        if (node.printAll) label.append(" ALL");
        String id = newNode(label.toString(), COL_IO);
        if (node.expressions != null) {
            for (Expression e : node.expressions) {
                edge(id, e.accept(this));
            }
        }
        if (node.events != null) {
            for (Event ev : node.events) {
                edge(id, ev.accept(this));
            }
        }
        return id;
    }
}
