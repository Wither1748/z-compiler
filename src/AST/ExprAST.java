package src.AST;

import src.Codegen.IRBuilder;
import src.Codegen.SymbolTable;
import src.Parser.Types;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class to incorporate all expressions and various code generations methods
 */
public abstract class ExprAST {
    /**
     * Symbol table to store the symbols
     */
    protected static SymbolTable symbolTable = new SymbolTable();
    protected static java.util.Map<String, PrototypeAST> functionSignatures = new java.util.HashMap<>();

    ExprAST() {}

    /**
     * Code generation method
     * @param builder IRBuilder to generate code
     * @return generated code
     */
    public abstract String Codegen(final IRBuilder builder);

    /**
     * Error logging method
     * @param err error message
     * @return null
     */
    public static ExprAST LogError(final String err) {
        System.err.printf("Error: %s%n", err);
        return null;
    }

    /**
     * Error logging method for variables
     * @param err error message
     * @return null
     */
    public static VariableExprAST LogErrorV(final String err){
        System.err.printf("Error: %s%n", err);
        return null;
    }

    /**
     * Error logging method for prototypes
     * @param err error message
     * @return null
     */
    public static PrototypeAST LogErrorP(final String err) {
        System.err.printf("Error: %s%n", err);
        return null;
    }

    /**
     * Error logging method for constants
     * @param err error message
     * @return null
     */
    public static ConstantAST LogErrorC(final String err) {
        System.err.printf("Error: %s%n", err);
        return null;
    }

    /**
     * Error logging method for for loops
     * @param err error message
     * @return null
     */
    public static ForAST LogErrorF(final String err) {
        System.err.printf("Error: %s%n", err);
        return null;
    }

    /**
     * Number expression class, gets called in case of a NUMBER token (es. 10)
     */
    public static class NumberExprAST extends ExprAST {
        /**
         * value of the number
         */
        private final double val;

        /**
         * Constructor
         * @param Val value of the number
         */
        public NumberExprAST(final double Val) {
            val = Val;
        }

        /**
         * Returns the value of the number, either int or double (kind of dumb)
         * @return value of the number
         */
        public final double getVal() { return val; }
        public final int getIntVal() { return (int) val; }

        @Override
        /**
         * Returns the value of the number as a string
         * @param builder IRBuilder to generate code
         * @return value of the number
         */
        public String Codegen(final IRBuilder builder) {
            if (val == (long) val) {
                return String.valueOf((long) val);
            }
            return String.valueOf(val);
        }
    }

    /**
     * Variable expression class, gets called in case of an ID token (es. x)
     */
    public static class VariableExprAST extends ExprAST {
        // !!!! This is kind of messed up
        /**
         * name is the name of the variable
         * type is the Ztype of the variable (es. int32)
         * value is the value of the variable using ExprAST
         * info is the symbol table entry of the variable
         */
        private final String name;
        private final String type;
        private ExprAST value;
        private SymbolTable.SymbolInfo info;

        /**
         * gets name, type and value (es. int32 var -> Expr) to define a new symbol table entry and initialize the variable
         * @param name name of the var
         * @param type Ztype of the var
         * @param value ExprAST value of the var
         */
        public VariableExprAST(final String name, final String type, final ExprAST value) {
            this.name = name;
            this.type = type;
            this.value = value;
            this.info = new SymbolTable.SymbolInfo(name, Types.getLlvmType(type), "%" + name, false);
        }

        /**
         * Gets name and type of a var without declaration (es. int32 var), also checks if the var is already defined
         * @param name name of the var
         * @param type Ztype of the var
         */
        public VariableExprAST(final String name, final String type) {
            this.name = name;
            this.type = type;
            this.value = new NumberExprAST(Double.parseDouble(Types.getDefaultValue(type)));
            this.info = new SymbolTable.SymbolInfo(name, Types.getLlvmType(type), "%" + name, false);
        }

        /**
         * used only for compatibility issues, don't use it
         * @param idName name of the var
         */
        public VariableExprAST(final String idName) {
            this.name = idName;
            this.type = "flt64";
            this.value = null;
            this.info = null;
        }

        /**
         * Getters methods for a var: name, type and value
         * @return name, value and type
         */
        public final String getName() { return name; }
        public final String getType() { return type; }
        public final ExprAST getValue() { return value; }

        @Override
        /**
         * Returns the value of the variable as a string
         * @param builder IRBuilder to generate code
         * @return value of the variable
         */
        public final String Codegen(final IRBuilder builder) {
            if (this.info == null) {
                // use of a variable
                final SymbolTable.SymbolInfo existing = symbolTable.lookup(name);
                if (existing == null) {
                    ExprAST.LogErrorV("Variabile non dichiarata: " + name);
                    return null;
                }
                return builder.emitLoad(existing.getLlvmType(), existing.getPointerReg());
            } else {
                // new variable
                final boolean result = symbolTable.define(name, this.info);
                if (!result) {
                    ExprAST.LogErrorV("Shadowing o ridichiarazione della variabile nello scope: " + name);
                    return null;
                }

                final String llvmType = info.getLlvmType();
                final String ptr = builder.emitAlloca(name, llvmType);

                if (value != null) {
                    String valReg = value.Codegen(builder);
                    if (valReg != null) {
                        builder.emitStore(valReg, llvmType, ptr);
                    }
                }
                return ptr;
            }
        }
    }

    /**
     * Binary expression class, gets called in case of a BINARY token (es. +, -, *, /)
     */
    public static class BinaryExprAST extends ExprAST {
        /**
         * Op is the operator of the binary expression
         * Left is the left operand
         * Right is the right operand
         */
        private final char op;
        private final ExprAST left;
        private final ExprAST right;
        public final char getOp() { return op; }
        public final ExprAST getLeft() { return left; }
        public final ExprAST getRight() { return right; }

        /**
         * Constructor for a binary operation
         * @param Op binary operation (es. +)
         * @param Left left operand
         * @param Right right operand
         */
        public BinaryExprAST(final char Op, final ExprAST Left, final ExprAST Right) {
            op = Op;
            left = Left;
            right = Right;
        }

        @Override
        /**
         * Generates LLVM code for a binary expression
         * @param builder IRbuilder to generate code
         * @return result of the binary operation
         */
        public final String Codegen(final IRBuilder builder) {
            final String leftVal = left.Codegen(builder);
            final String rightVal = right.Codegen(builder);

            if (leftVal == null || rightVal == null) return null;

            String leftType = builder.getRegisterType(leftVal);
            if (leftType == null) {
                leftType = leftVal.contains(".") ? "double" : "i32";
            }

            boolean isFloat = leftType.equals("double") || leftType.equals("float");
            final String resultReg = builder.nextRegister();

            switch (op) {
                case '+':
                    builder.appendLine(resultReg + " = " + (isFloat ? "fadd " : "add ") + leftType + " " + leftVal + ", " + rightVal);
                    break;
                case '-':
                    builder.appendLine(resultReg + " = " + (isFloat ? "fsub " : "sub ") + leftType + " " + leftVal + ", " + rightVal);
                    break;
                case '*':
                    builder.appendLine(resultReg + " = " + (isFloat ? "fmul " : "mul ") + leftType + " " + leftVal + ", " + rightVal);
                    break;
                case '/':
                    builder.appendLine(resultReg + " = " + (isFloat ? "fdiv " : "sdiv ") + leftType + " " + leftVal + ", " + rightVal);
                    break;
                case '<':
                    String cmpReg = builder.nextRegister();
                    if (isFloat) {
                        builder.appendLine(cmpReg + " = fcmp olt " + leftType + " " + leftVal + ", " + rightVal);
                        builder.appendLine(resultReg + " = uitofp i1 " + cmpReg + " to " + leftType);
                    } else {
                        builder.appendLine(cmpReg + " = icmp slt " + leftType + " " + leftVal + ", " + rightVal);
                        builder.appendLine(resultReg + " = zext i1 " + cmpReg + " to " + leftType);
                    }
                    break;
                case '>':
                    cmpReg = builder.nextRegister();
                    if (isFloat) {
                        builder.appendLine(cmpReg + " = fcmp ogt " + leftType + " " + leftVal + ", " + rightVal);
                        builder.appendLine(resultReg + " = uitofp i1 " + cmpReg + " to " + leftType);
                    } else {
                        builder.appendLine(cmpReg + " = icmp sgt " + leftType + " " + leftVal + ", " + rightVal);
                        builder.appendLine(resultReg + " = zext i1 " + cmpReg + " to " + leftType);
                    }
                    break;
                default:
                    System.err.println("Operazione non supportata: " + op);
                    return null;
            }

            builder.setRegisterType(resultReg, leftType);
            return resultReg;
        }
    }

    /**
     * Call expression class, gets called in case of a CALL token (es. proc)
     */
    public static class CallExprAST extends ExprAST {
        /**
         * callee is the name of the function to be called
         * args is a list of arguments to be passed to the function
         */
        private final String callee;
        private final List<ExprAST> args;

        /**
         * Constructor for a call expression
         * @param callee name of the function to be called
         * @param args list of arguments
         */
        public CallExprAST(final String callee, final List<ExprAST> args) {
            this.callee = callee;
            this.args = args;
        }

        @Override
        /**
         * Generates LLVM code for a call expression
         * @param builder IRbuilder to generate code
         * @return result of the call expression
         */
        public final String Codegen(final IRBuilder builder) {
            final List<String> argRegs = new ArrayList<>();
            final PrototypeAST sig = functionSignatures.get(callee);

            for (int i = 0; i < args.size(); i++) {
                final String argReg = args.get(i).Codegen(builder);
                if (argReg == null) return null;

                String argType = "double";
                if (sig != null && i < sig.getParams().size()) {
                    argType = Types.getLlvmType(sig.getParams().get(i).type);
                }
                argRegs.add(argType + " " + argReg);
            }

            final String resultReg = builder.nextRegister();
            final String retType = callee.equals("main") ? "i32" : "double";
            builder.appendLine(resultReg + " = call " + retType + " @" + callee
                    + "(" + String.join(", ", argRegs) + ")");
            return resultReg;
        }
    }

    public static class Param {
        public final String type;
        public final String name;

        public Param(final String type, final String name) {
            this.type = type;
            this.name = name;
        }
    }

    /**
     * Prototype class, gets called in case of a PROTO token (es. proc)
     */
    public static class PrototypeAST extends ExprAST {
        /**
         * name is the name of the function
         * params is a list of parameters (type + name)
         */
        private final String name;
        private final List<Param> params;

        /**
         * Constructor for a prototype
         * @param name name of the function
         * @param params list of parameters
         */
        public PrototypeAST(final String name, final List<Param> params) {
            this.name = name;
            this.params = params;
        }

        /**
         * getter method for a prototype name
         * @return name of the function
         */
        public final String getName() { return name; }

        /**
         * getter method for the prototype parameters
         * @return list of parameters
         */
        public final List<Param> getParams() { return params; }

        @Override
        /**
         * Generates code for a Prototype
         * @param builder IRbuilder to generate code
         * @return empty string
         */
        public final String Codegen(final IRBuilder builder) {
            return "";
        }
    }

    /**
     * Return class, gets called in case of a RET token (es. return)
     */
    public static class RetAST extends ExprAST {
        /**
         * statement is the right part after ret
         */
        private final ExprAST statement;

        /**
         * Constructor for a ret assignement
         * @param statement right part after the ret keyword
         */
        public RetAST(final ExprAST statement) {
            this.statement = statement;
        }

        /**
         * getter method for a statement
         * @return statement
         */
        public final ExprAST getStatement() { return statement; }

        @Override
        /**
         * Codegen for a ret expression
         * @param builder IRbuilder to generate code
         * @return result of the ret expression
         */
        public final String Codegen(final IRBuilder builder) {
            if (statement != null) {
                return statement.Codegen(builder);
            }
            return null;
        }
    }

    /**
     * Block class, gets called in case of a BLOCK token (es. { })
     */
    public static class BlockAST extends ExprAST {
        /**
         * statements is the list of statements
         */
        private final List<ExprAST> statements;

        /**
         * Constructor for a block
         * @param statements list of statements
         */
        public BlockAST(final List<ExprAST> statements) {
            this.statements = statements;
        }

        /**
         * getter method for statements
         * @return the statements
         */
        public final List<ExprAST> getStatements() { return statements; }

        @Override
        /**
         * Generates the code of a block expression
         * @param builder IRbuilder to generate the code
         * @return the code
         */
        public final String Codegen(final IRBuilder builder) {
            String lastVal = null;
            for (ExprAST stmt : statements) {
                lastVal = stmt.Codegen(builder);
            }
            return lastVal;
        }
    }

    /**
     * For loop class: for (type name -> init | cond | op) { body }
     */
    public static class ForAST extends ExprAST {
        /**
         * varType is the type of the loop variable (e.g. "int32")
         * varName is the name of the loop variable (e.g. "i")
         * init is the initial value expression
         * cond is the loop condition expression
         * update is the update expression
         * body is the loop body
         */
        private final String varType;
        private final String varName;
        private final ExprAST init;
        private final ExprAST cond;
        private final ExprAST update;
        private final ExprAST body;

        /**
         * Constructor for a for loop
         * @param varType type of the loop variable
         * @param varName name of the loop variable
         * @param init initial value expression
         * @param cond loop condition expression
         * @param update update expression
         * @param body loop body
         */
        public ForAST(final String varType, final String varName,
                      final ExprAST init, final ExprAST cond,
                      final ExprAST update, final ExprAST body) {
            this.varType = varType;
            this.varName = varName;
            this.init = init;
            this.cond = cond;
            this.update = update;
            this.body = body;
        }

        /**
         * getter methods for a for loop
         * @return fields
         */
        public final String getVarType() { return varType; }
        public final String getVarName() { return varName; }
        public final ExprAST getInit() { return init; }
        public final ExprAST getCond() { return cond; }
        public final ExprAST getUpdate() { return update; }
        public final ExprAST getBody() { return body; }

        @Override
        public final String Codegen(final IRBuilder builder) {
            final String llvmType = Types.getLlvmType(varType);

            // 1. Allocate and initialize the loop variable
            final String varPtr = builder.emitAlloca(varName, llvmType);
            symbolTable.define(varName, new SymbolTable.SymbolInfo(varName, llvmType, varPtr, false));

            final String initReg = init.Codegen(builder);
            if (initReg == null) return null;
            builder.emitStore(initReg, llvmType, varPtr);

            // 2. Create labels
            final String condLabel = builder.nextLabel("for.cond");
            final String bodyLabel = builder.nextLabel("for.body");
            final String incLabel  = builder.nextLabel("for.inc");
            final String endLabel  = builder.nextLabel("for.end");

            // 3. Jump to condition check
            builder.emitBr(condLabel);
            builder.emitLabel(condLabel);

            // 4. Evaluate condition as i1 and branch
            final String condReg = emitCondI1(builder);
            if (condReg == null) return null;
            builder.emitCondBr(condReg, bodyLabel, endLabel);

            // 5. Loop body
            builder.emitLabel(bodyLabel);
            body.Codegen(builder);
            builder.emitBr(incLabel);

            // 6. Increment step
            builder.emitLabel(incLabel);
            final String updateVal = update.Codegen(builder);
            if (updateVal == null) return null;
            builder.emitStore(updateVal, llvmType, varPtr);
            builder.emitBr(condLabel);

            // 7. Exit label
            builder.emitLabel(endLabel);

            // For is a statement, not an expression. Return "0" as a placeholder
            // so that a function whose last statement is a for loop can still
            // generate a valid `ret i32 0` for main.
            return "0";
        }

        /**
         * Evaluates the for-loop condition and returns the i1 register.
         * Handles `<` and `>` directly to avoid the i1 -> i32 zext that
         * BinaryExprAST emits for comparisons.
         */
        private String emitCondI1(final IRBuilder builder) {
            if (cond instanceof BinaryExprAST) {
                final BinaryExprAST bin = (BinaryExprAST) cond;
                final char op = bin.getOp();
                if (op == '<' || op == '>') {
                    final String leftVal = bin.getLeft().Codegen(builder);
                    final String rightVal = bin.getRight().Codegen(builder);
                    if (leftVal == null || rightVal == null) return null;

                    String leftType = builder.getRegisterType(leftVal);
                    if (leftType == null) {
                        leftType = leftVal.contains(".") ? "double" : "i32";
                    }
                    final boolean isFloat = leftType.equals("double") || leftType.equals("float");
                    final String cmpReg = builder.nextRegister();
                    if (isFloat) {
                        final String pred = op == '<' ? "olt" : "ogt";
                        builder.appendLine(cmpReg + " = fcmp " + pred + " " + leftType + " " + leftVal + ", " + rightVal);
                    } else {
                        final String pred = op == '<' ? "slt" : "sgt";
                        builder.appendLine(cmpReg + " = icmp " + pred + " " + leftType + " " + leftVal + ", " + rightVal);
                    }
                    return cmpReg;
                }
            }
            return null;
        }
    }

    /**
     * Function class, gets called in case of a FUNCTION token (es. proc)
     */
    public static class FunctionAST extends ExprAST {
        /**
         * proto is the prototype
         * body is the body of the function
         */
        private final PrototypeAST proto;
        private final ExprAST body;

        /**
         * Constructor for a function
         * @param proto the prototype
         * @param body the body of the function
         */
        public FunctionAST(final PrototypeAST proto, final ExprAST body) {
            this.proto = proto;
            this.body = body;
        }

        @Override
        /**
         * Generates the code of a function
         * @param builder IRbuilder to generate the code
         * @return the code
         */
        public final String Codegen(final IRBuilder builder) {
            builder.reset();
            symbolTable.enterScope();

            final StringBuilder functionIR = new StringBuilder();
            final boolean isMain = proto.getName().equals("main");
            final String retType = isMain ? "i32" : "double";
            final String linkage = isMain ? "dso_local" : "private dso_local";

            // Build parameter list: "i32 %x, i32 %y"
            final StringBuilder paramList = new StringBuilder();
            final List<PrototypeAST.Param> params = proto.getParams();
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) paramList.append(", ");
                final PrototypeAST.Param p = params.get(i);
                paramList.append(Types.getLlvmType(p.type)).append(" %").append(p.name);
            }

            functionIR.append("define ").append(linkage).append(" ").append(retType)
                    .append(" @").append(proto.getName())
                    .append("(").append(paramList).append(") {\n");
            functionIR.append("entry:\n");

            // Register signature so calls can look it up.
            functionSignatures.put(proto.getName(), proto);

            // Allocate each parameter and store the incoming value.
            for (PrototypeAST.Param p : params) {
                final String llvmT = Types.getLlvmType(p.type);
                final String addrName = p.name + ".addr";
                final String ptr = builder.emitAlloca(addrName, llvmT);
                builder.emitStore("%" + p.name, llvmT, ptr);
                symbolTable.define(p.name, new SymbolTable.SymbolInfo(
                        p.name, llvmT, ptr, false));
            }

            final String retVal = body.Codegen(builder);

            if (retVal != null) {
                functionIR.append(builder.getIR());
                if (isMain) {
                    // Check if retVal is an integer literal (including decimal representations like 0.0)
                    if (retVal.matches("-?\\d+\\.0+")) {
                        functionIR.append(" ret i32 ").append(retVal.substring(0, retVal.indexOf('.'))).append("\n");
                    } else if (retVal.matches("-?\\d+")) {
                        functionIR.append(" ret i32 ").append(retVal).append("\n");
                    } else {
                        // Check if the register is already i32 type
                        final String regType = builder.getRegisterType(retVal);
                        if (regType != null && regType.equals("i32")) {
                            functionIR.append(" ret i32 ").append(retVal).append("\n");
                        } else {
                            final String intReg = builder.nextRegister();
                            functionIR.append(" ").append(intReg).append(" = fptosi double ").append(retVal).append(" to i32\n");
                            functionIR.append(" ret i32 ").append(intReg).append("\n");
                        }
                    }
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

    /**
     * Constant class, gets called in case of a CONSTANT token (es. const)
     */
    public static class ConstantAST extends ExprAST {
        /**
         * type is the type of the constant
         * name is the name of the constant
         * value is the ExprAST value
         */
        private final String type;
        private final String name;
        private final ExprAST value;

        /**
         * Construct a constant node (es. cn int32 const -> value)
         * @param type Ztype of the constant
         * @param name name of the constant
         * @param value value of the constant
         */
        public ConstantAST(final String type, final String name, final ExprAST value) {
            this.type = type;
            this.name = name;
            this.value = value;
        }

        /**
         * getter methods for a constant
         * @return type, name and value
         */
        public final String getType() { return type; }
        public final String getName() { return name; }
        public final ExprAST getValue() { return value; }

        @Override
        /**
         * Generates the code for a constant
         * @param builder IRbuilder to generate the code
         * @return the code for a constant
         */
        public final String Codegen(final IRBuilder builder) {
            String llvmType = Types.getLlvmType(type);
            SymbolTable.SymbolInfo info = new SymbolTable.SymbolInfo(name, llvmType, "%" + name, true);

            if (!symbolTable.define(name, info)) {
                ExprAST.LogErrorC("Constant redefinition: " + name);
                return null;
            }

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
}