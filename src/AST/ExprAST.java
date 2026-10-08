package src.AST;

import src.Codegen.IRBuilder;
import src.Parser.ZType;

public abstract class ExprAST {

    /**
     * What this node produces.
     *
     */
    public abstract ZType type(IRBuilder builder);

    /**
     * Emits the code for this node.
     *
     * @param builder the module being built
     * @return the value produced, or {@code null} if generation failed — in which
     *         case an error has already been reported
     */
    public abstract Value Codegen(final IRBuilder builder);

    /**
     * Converts a generated value to {@code target}, emitting the conversion.
     *
     * @return the value in the requested type, or the original when it already is
     */
    public Value coerce(final IRBuilder builder, final Value value, final ZType target) {
        if (value == null || target == null) return value;
        final ZType from = value.type();
        if (from == target) return value;

        final String result = builder.nextRegister();
        if (from.isFloat() && target.isFloat()) {
            // float is narrower than double; double is widened to it otherwise.
            builder.appendLine(result + " = " + (target.bits() < from.bits() ? "fptrunc " : "fpext ") + from.llvm() + " " + value.text() + " to " + target.llvm());
        } else if (from.isFloat() && target.isIntegral()) {
            builder.appendLine(result + " = fptosi " + from.llvm() + " " + value.text() + " to " + target.llvm());
        } else if (from.isIntegral() && target.isFloat()) {
            builder.appendLine(result + " = sitofp " + from.llvm() + " " + value.text() + " to " + target.llvm());
        } else if (from.isIntegral() && target.isIntegral()) {
            builder.appendLine(result + " = " + (from.bits() > target.bits() ? "trunc " : "sext ") + from.llvm() + " " + value.text() + " to " + target.llvm());
        } else {
            return value;
        }
        return new Value(result, target);
    }
}
