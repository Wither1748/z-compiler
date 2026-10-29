package src.AST;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import src.Codegen.IRBuilder;
import src.Codegen.SymbolTable;
import src.Parser.TypeRef;
import src.Parser.ZType;

public final class VarRefExprAST extends ExprAST {
    private static final Logger logger = LoggerFactory.getLogger(VarRefExprAST.class);

    private final String name;

    public VarRefExprAST(final String name) {
        this.name = name;
    }

    /** @return the name being read */
    public String name() {
        return name;
    }

    @Override
    public TypeRef type(final IRBuilder builder) {
        SymbolTable.SymbolInfo info = builder.symbols().lookup(name);
        if (info == null && !name.contains(".")) info = builder.symbols().lookup(builder.qualify(name));
        return info == null ? ZType.INT32 : info.getType();
    }

    @Override
    public Value codeGen(final IRBuilder builder) {
        SymbolTable.SymbolInfo info = builder.symbols().lookup(name);
        if (info == null && !name.contains(".")) {
            info = builder.symbols().lookup(builder.qualify(name));
        }
        if (info == null) {
            logger.error("Error: undeclared variable: {}", name);
            builder.markFailed();
            return null;
        }
        final TypeRef type = info.getType();
        return new Value(builder.emitLoad(type.llvm(), info.getPointerReg()), type);
    }
}