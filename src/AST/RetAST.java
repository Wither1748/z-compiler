package src.AST;

import src.Codegen.IRBuilder;
import src.Parser.ZType;

public final class RetAST extends ExprAST {

    private final ExprAST value;

    public RetAST(final ExprAST value) {
        this.value = value;
    }

    @Override
    public ZType type(final IRBuilder builder) {
        return ZType.VOID;
    }

    @Override
    public Value Codegen(final IRBuilder builder) {
        if (value == null) return null;
        final Value produced = value.Codegen(builder);
        if (produced == null) return null;
        builder.emitReturn(produced);
        return null;
    }
}
