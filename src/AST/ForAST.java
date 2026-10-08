package src.AST;

import src.Codegen.IRBuilder;
import src.Codegen.SymbolTable;
import src.Parser.ZType;

/**
 * A for loop: {@code for (type name -> init | cond | op) { body }}.
 *
 * <p>The loop variable is declared in the current scope, initialised,
 * checked before each iteration, updated at the end of each iteration,
 * and stays visible after the loop.
 */
public final class ForAST extends ExprAST {

    private final ZType varType;
    private final String varName;
    private final ExprAST init;
    private final ExprAST cond;
    private final ExprAST update;
    private final ExprAST body;

    public ForAST(final ZType varType, final String varName,
                  final ExprAST init, final ExprAST cond,
                  final ExprAST update, final ExprAST body) {
        this.varType = varType;
        this.varName = varName;
        this.init = init;
        this.cond = cond;
        this.update = update;
        this.body = body;
    }

    /** @return the declared type of the loop variable */
    public ZType varType() { return varType; }

    /** @return the loop variable name */
    public String varName() { return varName; }

    /** @return the initialiser expression */
    public ExprAST init() { return init; }

    /** @return the loop condition */
    public ExprAST cond() { return cond; }

    /** @return the update expression */
    public ExprAST update() { return update; }

    /** @return the loop body */
    public ExprAST body() { return body; }

    @Override
    public ZType type(final IRBuilder builder) {
        return ZType.VOID;
    }

    @Override
    public Value Codegen(final IRBuilder builder) {
        // 1. Declare the loop variable in the current scope
        if (builder.symbols().lookupCurrentScope(varName) != null) {
            System.err.println("Error: Shadowing o ridichiarazione locale: " + varName);
            return null;
        }
        final String slot = builder.emitAlloca(varName, varType.llvm());
        builder.symbols().define(varName, new SymbolTable.SymbolInfo(
                varName, varType.llvm(), slot, false));

        // 2. Initialise the loop variable
        if (init != null) {
            final Value produced = init.Codegen(builder);
            if (produced == null) return null;
            final Value fitted = init.coerce(builder, produced, varType);
            if (fitted == null) return null;
            builder.emitStore(fitted.text(), varType.llvm(), slot);
        } else {
            builder.emitStore(varType.initial(), varType.llvm(), slot);
        }

        // 3. Create the four labels
        final String condLabel = builder.nextLabel("for.cond");
        final String bodyLabel = builder.nextLabel("for.body");
        final String incLabel  = builder.nextLabel("for.inc");
        final String endLabel  = builder.nextLabel("for.end");

        // 4. Jump into the condition
        builder.emitBr(condLabel);
        builder.emitLabel(condLabel);

        // 5. Evaluate the condition as i1 and branch
        final String condReg = emitConditionI1(builder, cond);
        if (condReg == null) return null;
        builder.emitCondBr(condReg, bodyLabel, endLabel);

        // 6. Loop body
        builder.emitLabel(bodyLabel);
        body.Codegen(builder);
        builder.emitBr(incLabel);

        // 7. Increment step
        builder.emitLabel(incLabel);
        final Value updateProduced = update.Codegen(builder);
        if (updateProduced == null) return null;
        final Value updateFitted = update.coerce(builder, updateProduced, varType);
        if (updateFitted == null) return null;
        builder.emitStore(updateFitted.text(), varType.llvm(), slot);
        builder.emitBr(condLabel);

        // 8. Exit
        builder.emitLabel(endLabel);
        return null;
    }

    /**
     * Evaluates {@code condition} and returns an i1 register suitable for
     * {@code br i1}.
     *
     * <p>For {@code <} and {@code >} we emit {@code icmp}/{@code fcmp} directly
     * so we get the raw i1, instead of going through the i1-to-i32
     * {@code zext} that {@link BinaryExprAST} emits for comparisons.
     * Any other expression is evaluated and compared against zero.
     */
    private String emitConditionI1(final IRBuilder builder, final ExprAST condition) {
        if (condition instanceof BinaryExprAST) {
            final BinaryExprAST bin = (BinaryExprAST) condition;
            final char op = bin.operator();
            if (op == '<' || op == '>') {
                final Value a = bin.left().Codegen(builder);
                if (a == null) return null;
                final Value b = bin.right().Codegen(builder);
                if (b == null) return null;
                final boolean ordered = a.type().isFloat();
                final String pred = ordered
                        ? (op == '<' ? "olt" : "ogt")
                        : (op == '<' ? "slt" : "sgt");
                final String cmp = builder.nextRegister();
                builder.appendLine(cmp + " = " + (ordered ? "fcmp " : "icmp ")
                        + pred + " " + a.type().llvm() + " " + a.text() + ", " + b.text());
                return cmp;
            }
        }

        // Fallback: evaluate and compare to zero
        final Value v = condition.Codegen(builder);
        if (v == null) return null;
        final String cmp = builder.nextRegister();
        if (v.type().isFloat()) {
            builder.appendLine(cmp + " = fcmp one " + v.type().llvm()
                    + " " + v.text() + ", 0.0");
        } else {
            builder.appendLine(cmp + " = icmp ne " + v.type().llvm()
                    + " " + v.text() + ", 0");
        }
        return cmp;
    }
}