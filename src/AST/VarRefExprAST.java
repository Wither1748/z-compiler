package src.AST;

import src.Codegen.IRBuilder;
import src.Codegen.SymbolTable;
import src.Parser.ZType;

public final class VarRefExprAST extends ExprAST {

    private final String name;

    public VarRefExprAST(final String name) {
        this.name = name;
    }

    /** @return the name being read */
    public String name() {
        return name;
    }

    @Override
    public ZType type(final IRBuilder builder) {
        final SymbolTable.SymbolInfo info = builder.symbols().lookup(name);
        return info == null ? ZType.INT32 : ZType.fromZName(zNameOf(info));
    }

    @Override
    public Value Codegen(final IRBuilder builder) {
        final SymbolTable.SymbolInfo info = builder.symbols().lookup(name);
        if (info == null) {
            System.err.println("Error: Variabile non dichiarata: " + name);
            return null;
        }
        final ZType type = ZType.fromZName(zNameOf(info));
        return new Value(builder.emitLoad(type.llvm(), info.getPointerReg()), type);
    }

    private static String zNameOf(final SymbolTable.SymbolInfo info) {
        for (final ZType candidate : ZType.values()) {
            if (candidate.llvm().equals(info.getLlvmType())) return candidate.zName();
        }
        return "int32";
    }
}
