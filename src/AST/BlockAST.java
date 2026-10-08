package src.AST;

import src.Codegen.IRBuilder;
import src.Parser.ZType;

import java.util.List;

/**
*
 */
public final class BlockAST extends ExprAST {

    private final List<ExprAST> statements;

    public BlockAST(final List<ExprAST> statements) {
        this.statements = List.copyOf(statements);
    }

    /** @return the statements, in order */
    public List<ExprAST> statements() {
        return statements;
    }

    @Override
    public ZType type(final IRBuilder builder) {
        return ZType.VOID;
    }

    @Override
    public Value Codegen(final IRBuilder builder) {
        for (final ExprAST statement : statements) {
            if (builder.isTerminated()) {
                System.err.println("Error: this statement is unreachable, " + "the block has already returned");
                return null;
            }
            statement.Codegen(builder);
        }
        return null;
    }
}
