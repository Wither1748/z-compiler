package src.AST;

import src.Codegen.IRBuilder;
import src.Parser.ZType;

public final class UnaryExprAST extends ExprAST {

    private final ExprAST operand;

    public UnaryExprAST(final ExprAST operand) {
        this.operand = operand;
    }

    /** @return the expression being negated */
    public ExprAST operand() {
        return operand;
    }

    @Override
    public ZType type(final IRBuilder b) {
        return operand.type(b);
    }

    @Override
    public Value Codegen(final IRBuilder b) {
        final Value value = operand.Codegen(b);
        if (value == null) return null;
        final String register = b.nextRegister();
        b.appendLine(register + " = " + (value.type().isFloat() ? "fneg " : "sub ") + " " + value.type().llvm() + " 0, " + value.text());
        b.setRegisterType(register, value.type().llvm());
        return new Value(register, value.type());
    }
}
