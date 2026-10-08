package src.AST;

import src.Codegen.IRBuilder;
import src.Codegen.SymbolTable;
import src.Parser.ZType;

/**
 * A declaration: {@code <type> <name> -> <value>} or {@code cn <type> <name> -> <value>}.
 *
 * <p>These were two nodes that were the same node. {@code VariableExprAST}'s
 * declaration branch and {@code ConstantAST} each defined the symbol, allocated
 * it, generated the initialiser and stored it — about thirty-five duplicated
 * lines, differing only in a flag and in the wording of an error. A constant is a
 * declaration that may not be assigned again, so that is the only difference.
 *
 * <p>A declaration produces no value. It used to return its own address, which is
 * how a body whose last statement was {@code int32 x -> 5} came to return a
 * pointer to {@code x}: LLVM saw {@code fptosi double %x to i32} and said no.
 */
public final class DeclaratorExprAST extends ExprAST {

    private final ZType type;
    private final String name;
    private final ExprAST initialiser;
    private final boolean constant;

    public DeclaratorExprAST(final ZType type, final String name,
                             final ExprAST initialiser, final boolean constant) {
        this.type = type;
        this.name = name;
        this.initialiser = initialiser;
        this.constant = constant;
    }

    /** @return the declared type */
    public ZType declaredType() {
        return type;
    }

    /** @return the declared name */
    public String name() {
        return name;
    }

    @Override
    public ZType type(final IRBuilder builder) {
        return type;
    }

    @Override
    public Value Codegen(final IRBuilder builder) {
        if (builder.symbols().lookupCurrentScope(name) != null) {
            System.err.println("Error: Shadowing o ridichiarazione locale: " + name);
            return null;
        }

        String text;
        if (initialiser == null) {
            text = type.initial();
        } else {
            final Value value = initialiser.Codegen(builder);
            if (value == null) {
                // The initialiser failed. Declaring the variable anyway would
                // leave a slot nothing ever writes, and every later read of it
                // would be whatever the stack happened to hold.
                return null;
            }
            // On the initialiser, not on this: a literal re-prints itself
            // in the target type, and that override lives on the literal.
            final Value fitted = initialiser.coerce(builder, value, type);
            if (fitted == null) return null;
            text = fitted.text();
        }

        final String slot = builder.emitAlloca(name, type.llvm());
        builder.emitStore(text, type.llvm(), slot);
        builder.symbols().define(name, new SymbolTable.SymbolInfo(
                name, type.llvm(), slot, constant));
        return null;
    }
}
