package src.AST;

import src.Codegen.IRBuilder;
import src.Codegen.SymbolTable;
import src.Parser.Types;

import java.util.ArrayList;
import java.util.List;

public abstract class ExprAST {
    protected static SymbolTable symbolTable = new SymbolTable();

    ExprAST() {}

    public abstract String Codegen(IRBuilder builder);

    public static ExprAST LogError(final String err) {
        System.err.printf("Error: %s%n", err);
        return null;
    }

    public static VariableExprAST LogErrorV(final String err){
        System.err.printf("Error: %s%n", err);
        return null;
    }

    public static PrototypeAST LogErrorP(final String err) {
        System.err.printf("Error: %s%n", err);
        return null;
    }

    public static ConstantAST LogErrorC(final String err) {
        System.err.printf("Error: %s%n", err);
        return null;
    }

    public static class NumberExprAST extends ExprAST {
        private final double val;

        public NumberExprAST(final double Val) {
            val = Val;
        }

        public double getVal() { return val; }
        public int getIntVal() { return (int) val; }

        @Override
        public String Codegen(IRBuilder builder) {
            return String.valueOf(val);
        }
    }

    public static class VariableExprAST extends ExprAST {
        private final String name;
        private final String type;
        private ExprAST value;
        private SymbolTable.SymbolInfo info;

        public VariableExprAST(final String name, final String type, final ExprAST value) {
            this.name = name;
            this.type = type;
            this.value = value;
            this.info = new SymbolTable.SymbolInfo(name, Types.getLlvmType(type), "%" + name, false);
            symbolTable.define(name, this.info);
        }

        public VariableExprAST(final String name, final String type) {
            this.name = name;
            this.type = type;
            this.value = new NumberExprAST(Double.parseDouble(Types.getDefaultValue(type)));
            this.info = new SymbolTable.SymbolInfo(name, Types.getLlvmType(type), "%" + name, false);
            final boolean result = symbolTable.define(name, this.info);
            if (!result) {
                ExprAST.LogErrorV("Shadowing or redeclaration of variable in current scope: " + name);
            }
        }

        public VariableExprAST(final String idName) {
            this.name = idName;
            this.type = "flt64";
            this.value = null;
        }

        public final String getName() { return name; }
        public final String getType() { return type; }
        public final ExprAST getValue() { return value; }

        @Override
        public String Codegen(IRBuilder builder) {
            SymbolTable.SymbolInfo existing = symbolTable.lookup(name);
            if (existing != null) {
                // reference to an existing variable
                return builder.emitLoad(existing.getLlvmType(), existing.getPointerReg());
            }

            // definining a new var
            String llvmType = info != null ? info.getLlvmType() : "double";
            String ptr = builder.emitAlloca(name, llvmType);
            if (value != null) {
                String valReg = value.Codegen(builder);
                if (valReg != null) {
                    builder.emitStore(valReg, llvmType, ptr);
                }
            }
            return ptr;
        }
    }

    public static class BinaryExprAST extends ExprAST {
        private final char op;
        private final ExprAST left;
        private final ExprAST right;

        public BinaryExprAST(final char Op, final ExprAST Left, final ExprAST Right) {
            op = Op;
            left = Left;
            right = Right;
        }

        @Override
        public String Codegen(IRBuilder builder) {
            String leftVal = left.Codegen(builder);
            String rightVal = right.Codegen(builder);

            if (leftVal == null || rightVal == null) return null;

            String resultReg = builder.nextRegister();
            switch (op) {
                case '+':
                    builder.appendLine(resultReg + " = fadd double " + leftVal + ", " + rightVal);
                    break;
                case '-':
                    builder.appendLine(resultReg + " = fsub double " + leftVal + ", " + rightVal);
                    break;
                case '*':
                    builder.appendLine(resultReg + " = fmul double " + leftVal + ", " + rightVal);
                    break;
                case '/':
                    builder.appendLine(resultReg + " = fdiv double " + leftVal + ", " + rightVal);
                    break;
                case '<':
                    String cmpReg = builder.nextRegister();
                    builder.appendLine(cmpReg + " = fcmp olt double " + leftVal + ", " + rightVal);
                    builder.appendLine(resultReg + " = uitofp i1 " + cmpReg + " to double");
                    break;
                default:
                    System.err.println("Unsupported operation: " + op);
                    return null;
            }
            return resultReg;
        }
    }

    public static class CallExprAST extends ExprAST {
        private final String callee;
        private final List<ExprAST> args;

        public CallExprAST(final String callee, final List<ExprAST> args) {
            this.callee = callee;
            this.args = args;
        }

        @Override
        public String Codegen(IRBuilder builder) {
            List<String> argRegs = new ArrayList<>();
            for (ExprAST arg : args) {
                String argReg = arg.Codegen(builder);
                if (argReg == null) return null;
                argRegs.add("double " + argReg);
            }
            String resultReg = builder.nextRegister();
            builder.appendLine(resultReg + " = call double @" + callee + "(" + String.join(", ", argRegs) + ")");
            return resultReg;
        }
    }

    public static class PrototypeAST extends ExprAST {
        private final String name;
        private final List<String> args;

        public PrototypeAST(final String name, final List<String> args) {
            this.name = name;
            this.args = args;
        }

        public final String getName() { return name; }

        @Override
        public String Codegen(IRBuilder builder) {
            return "";
        }
    }

    public static class RetAST extends ExprAST {
        private final ExprAST statement;

        public RetAST(final ExprAST statement) {
            this.statement = statement;
        }

        public ExprAST getStatement() { return statement; }

        @Override
        public String Codegen(IRBuilder builder) {
            if (statement != null) {
                return statement.Codegen(builder);
            }
            return null;
        }
    }

    public static class BlockAST extends ExprAST {
        private final List<ExprAST> statements;

        public BlockAST(final List<ExprAST> statements) {
            this.statements = statements;
        }

        public List<ExprAST> getStatements() { return statements; }

        @Override
        public String Codegen(IRBuilder builder) {
            String lastVal = null;
            for (ExprAST stmt : statements) {
                lastVal = stmt.Codegen(builder);
            }
            return lastVal;
        }
    }

    public static class FunctionAST extends ExprAST {
        private final PrototypeAST proto;
        private final ExprAST body;

        public FunctionAST(final PrototypeAST proto, final ExprAST body) {
            this.proto = proto;
            this.body = body;
        }

        @Override
        public String Codegen(IRBuilder builder) {
            builder.reset();
            symbolTable.enterScope();

            StringBuilder functionIR = new StringBuilder();
            boolean isMain = proto.getName().equals("main");
            String retType = isMain ? "i32" : "double";
            String linkage = isMain ? "dso_local" : "private dso_local";

            functionIR.append("define ").append(linkage).append(" ").append(retType).append(" @").append(proto.getName()).append("() {\n");
            functionIR.append("entry:\n");

            String retVal = body.Codegen(builder);

            if (retVal != null) {
                functionIR.append(builder.getIR());
                if (isMain) {
                    String intReg = builder.nextRegister();
                    functionIR.append("  ").append(intReg).append(" = fptosi double ").append(retVal).append(" to i32\n");
                    functionIR.append("  ret i32 ").append(intReg).append("\n");
                } else {
                    functionIR.append(" ret double ").append(retVal).append("\n");
                }
                functionIR.append("}\n");
                symbolTable.exitScope();
                return functionIR.toString();
            }

            symbolTable.exitScope();
            System.err.println("Null retval in function " + proto.getName());
            return null;
        }
    }

    public static class ConstantAST extends ExprAST {
        private final String type;
        private final String name;
        private final ExprAST value;

        public ConstantAST(final String type, final String name, final ExprAST value) {
            this.type = type;
            this.name = name;
            this.value = value;
        }

        public String getType() { return type; }
        public String getName() { return name; }
        public ExprAST getValue() { return value; }

        @Override
        public String Codegen(IRBuilder builder) {
            if (value != null) {
                return value.Codegen(builder);
            }
            return "";
        }
    }
}