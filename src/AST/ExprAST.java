package src.AST;

import src.Codegen.IRBuilder;
import src.Parser.*;

import java.util.List;
import java.util.Objects;

// this probably needs a type field, but we'll figure that out when we'll need a type checker
public abstract class ExprAST {


    ExprAST() {

    }

    public abstract String Codegen(IRBuilder builder);

    public static ExprAST LogError(final String err) {
        System.out.printf("Error: %s", err);
        return null;
    }

    public static VariableExprAST LogErrorV(final String err){
        System.out.printf("Error: %s", err);
        return null;
    }

    public static PrototypeAST LogErrorP(final String err) {
        System.out.printf("Error: %s",err);
        return null;
    }

    public static ConstantAST LogErrorC(final String err) {
        System.out.printf("Error: %s%n", err);
        return null;
    }

    public static class NumberExprAST extends ExprAST {
        private final double val;

        public NumberExprAST(final double Val) {
            val = Val;
        }

        @Override
        public String Codegen(IRBuilder builder) {
            return String.valueOf(val);
        }
    }

    public static class VariableExprAST extends ExprAST {
        private final String name;
        private final String type;
        private ExprAST value;
        private boolean flagForVars = false;
        private String Identifier;

        public VariableExprAST(final String Name, final String type, final ExprAST value) {
            name = Name;
            this.type = type;
            this.value = value;
            this.Identifier = "";
        }

        public VariableExprAST(final String name, final String type) {
            this.name = name;
            this.type = type;
            value = new NumberExprAST(0); // TODO add types different defaults
        }

        public VariableExprAST(final String idName) {
            name = idName;
            type = "double";
            value = new NumberExprAST(0);
        }

        public VariableExprAST(final String name, final String type, final String varName) {
            this.name = name;
            this.type = type;
            this.value = checkIfVarOrFunction(varName);
        }

        public NumberExprAST checkIfVarOrFunction(final String name) {
            final Integer ret = Parser.functionCount.get(name);
            Identifier = name;
            if (ret == null) { // it must be a variable
                flagForVars = true;
                return null;
            }

            flagForVars = false;
            return null;
        }

        public void setValue(final ExprAST value) {
            if (value instanceof ExprAST.CallExprAST || value instanceof ExprAST.NumberExprAST || value instanceof ExprAST.BinaryExprAST) {
                this.value = value;
            }
        }

        public final String getName() {
            return name;
        }

        public final String getType() {
            return type;
        }

        public final ExprAST getValue() {
            return value;
        }

        @Override
        public String Codegen(IRBuilder builder){
            builder.appendLine("%" + name + " = alloca double");
            if (!Objects.equals(Identifier, "") && !flagForVars) {
                builder.appendLine(builder.nextRegister() + " = call " + getType() + "@" + Identifier + "(");
            }
            return builder.toString();
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
            switch (op) { // TODO add types
                // nothing for now
                case '+':
                    builder.appendLine(resultReg + " = fadd double " + leftVal + ", " + rightVal);
                    break;

                case '-':
                    builder.appendLine(resultReg + " =  fsub double " + leftVal + ", " + rightVal);
                    break;
                case '*':
                    builder.appendLine(resultReg + " = fmul double " + leftVal + ", " + rightVal);
                    break;

                case '<':
                    String cmpReg = builder.nextRegister();
                    builder.appendLine(cmpReg + " = fcmp olt double " + leftVal + ", " + rightVal);
                    builder.appendLine(resultReg + " = uitofp i1 " + cmpReg + " to double");
                    break;
                default:
                    System.err.println("Unsopported operation: " + op);
                    return null;
            }

            return resultReg;

        }
    }

    public static class CallExprAST extends ExprAST {
        private final String Callee;
        private final List<ExprAST> args;

        public CallExprAST(final String callee, final List<ExprAST> Args) {
            Callee = callee;
            args = Args;
        }

        @Override
        public String Codegen(IRBuilder builder) {
            return ""; // nothing for now
        }
    }

    public static class PrototypeAST extends ExprAST{
        private final String name;
        private final List<String> args;

        public PrototypeAST(final String name, final List<String> args) {
            this.name = name;
            this.args = args;
        }

        public final String getName() { return name; }

        @Override
        public String Codegen(IRBuilder builder) {
            return ""; // nothing for now
        }
    }

    public static class RetAST extends ExprAST {
        private final ExprAST statement;

        public RetAST(final ExprAST statement) {
            this.statement = statement;
        }

        @Override
        public String Codegen(IRBuilder builder) {
            if (statement instanceof NumberExprAST) {
                builder.appendLine("ret i32 " + statement.Codegen(builder));
                return builder.getIR();
            }

            return ""; // nothing for now
        }
    }

    public static class FunctionAST extends ExprAST {
        private final PrototypeAST Proto;
        private final ExprAST body;

        public FunctionAST(final PrototypeAST Proto, final ExprAST body) {
            this.Proto = Proto;
            this.body = body;
        }

        @Override
        public String Codegen(IRBuilder builder) {

            builder.reset();

            StringBuilder functionIR = new StringBuilder();
            boolean isMain = Proto.getName().equals("main");
            // very funny right? norecurse nounwind alwaysinline after () not working also tailcc
            // i32 is for the main (fucking standards)
            String retType = isMain ? "i32" : "double";
            String linkage = isMain ? "dso_local" : "private dso_local";

            functionIR.append("define ").append(linkage).append(" ").append(retType).append(" @").append(Proto.getName()).append("() {\n");
            functionIR.append("entry:\n");

            String retVal = body.Codegen(builder);

            if (retVal != null) {
                functionIR.append(builder.getIR());
                if (isMain) {
                    String intReg = builder.nextRegister();
                    functionIR.append("  ").append(intReg).append(" = fptosi double ").append(retVal).append(" to i32\n");
                    retVal = intReg;
                }

                functionIR.append("  ret ").append(retType).append(" ").append(retVal).append("\n");
                functionIR.append("}\n");
                return functionIR.toString();
            }

            System.out.println("null retval");
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
            return ""; // nothing for now
        }
    }
}





