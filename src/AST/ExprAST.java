package src.AST;

import src.Codegen.IRBuilder;
import java.util.List;

// this probably needs a type field, but we'll figure that out when we'll need a type checker
public abstract class ExprAST {


    ExprAST() {

    }

    public abstract  String Codegen(IRBuilder builder);

    public static ExprAST LogError(final String err) {
        System.out.printf("Error: %s", err);
        return null;
    }

    public static PrototypeAST LogErrorP(final String err) {
        System.out.printf("Error: %s",err);
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

        public VariableExprAST(final String Name) {
            name = Name;
        }

        @Override
        public String Codegen(IRBuilder builder){
            return ""; // nothing for now
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
                // nothing for now
            }

            return "";
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

    public static class FunctionAST extends ExprAST {
        private final PrototypeAST Proto;
        private final ExprAST body;

        public FunctionAST(final PrototypeAST Proto, final ExprAST body) {
            this.Proto = Proto;
            this.body = body;
        }

        @Override
        public String Codegen(IRBuilder builder) {
            return ""; // nothing for now
        }
    }
}





