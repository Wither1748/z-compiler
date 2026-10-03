package src.Parser;

import src.lexer.Lexer;
import src.AST.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Parser class to parse Z code using an AST
 */
public class Parser {
    /**
     * curTok stores the current Token from the Lexer
     * lex stores the Lexer
     */
    private int curTok;
    private Lexer lex = null;

    /**
     * BinOpPrecedence stores the precedence of binary operators
     */
    public static final HashMap<Integer, Integer> BinOpPrecedence = new HashMap<>();
    static {
        BinOpPrecedence.put(Lexer.Tokens.LESS.value, 10);
        BinOpPrecedence.put(Lexer.Tokens.MORE.value, 10);
        BinOpPrecedence.put(Lexer.Tokens.SAME.value, 10);
        BinOpPrecedence.put(Lexer.Tokens.ADD.value, 20);
        BinOpPrecedence.put(Lexer.Tokens.SUB.value, 20);
        BinOpPrecedence.put(Lexer.Tokens.MUL.value, 40);
        BinOpPrecedence.put(Lexer.Tokens.DIV.value, 40);
    }

    /**
     * functionCount stores the count of functions for future optimizations
     */
    public static HashMap<String, Integer> functionCount = new HashMap<>();

    /**
     * Constructor for Parser
     * @param lex the lexer object
     * @throws IOException
     */
    public Parser(final Lexer lex) throws IOException {
        this.lex = lex;
        this.curTok = lex.GetTok();
    }

    /**
     * MainLoop method to parse the input
     * @throws IOException
     */
    public void MainLoop() throws IOException {
        while (true) {
            final Lexer.Tokens token = Lexer.Tokens.fromValues(curTok);

            if (token == Lexer.Tokens.EOF) {
                return;
            } else if (curTok == ';') {
                Next();
            } else if (token == Lexer.Tokens.FUNC) {
                HandleDefinition();
            } else if (token == Lexer.Tokens.CN) {
                HandleConstant();
            } else if (isTypeToken(token)) {
                HandleVariable();
            } else {
                ExprAST.LogError("Unknown token in main loop: " + curTok);
                Next();
            }
        }
    }

    /**
     * Handle the definition of a function
     * @throws IOException
     */
    public void HandleDefinition() throws IOException {
        final ExprAST.FunctionAST e = ParseDefinition();
        if (e != null) {
            System.out.println("Parsed a function");
        } else {
            Next();
        }
    }

    /**
     * handles the definition and assignment of a constant
     * @throws IOException
     */
    public void HandleConstant() throws IOException {
        final ExprAST.ConstantAST c = ParseConstant();
        if (c != null) {
            System.out.println("Parsed a constant: " + c.getName());
        } else {
            Next();
        }
    }

    /**
     * Handles the definition e/o assignment of a variable
     * @throws IOException
     */
    public void HandleVariable() throws IOException {
        final ExprAST.VariableExprAST v = ParseVariable();
        if (v != null) {
            System.out.println("Parsed a variable: " + v.getName());
        } else {
            Next();
        }
    }

    /**
     * Parses a variable
     * @return an AST node or an error
     * @throws IOException
     */
    public final ExprAST.VariableExprAST ParseVariable() throws IOException {
        final Lexer.Tokens type = Lexer.Tokens.fromValues(curTok);
        Next();

        if (curTok != Lexer.Tokens.IDENTIFIER.value) {
            return ExprAST.LogErrorV("Expected name after type");
        }

        final String name = lex.IdentifierStr;
        Next();

        if (curTok != Lexer.Tokens.ASSIGN.value) {
            return new ExprAST.VariableExprAST(name, type.description);
        }

        Next();
        final ExprAST val = ParseExpression();
        return new ExprAST.VariableExprAST(name, type.description, val);
    }

    /**
     * Parses a definition and assignement of a constant
     * @return an AST node or an expression
     * @throws IOException
     */
    public final ExprAST.ConstantAST ParseConstant() throws IOException {
        Next();

        final Lexer.Tokens typeTok = Lexer.Tokens.fromValues(curTok);
        if (typeTok == null || !isTypeToken(typeTok)) {
            return ExprAST.LogErrorC("Expected type after 'cn'");
        }
        final String type = typeTok.description;
        Next();

        if (curTok != Lexer.Tokens.IDENTIFIER.value) {
            return ExprAST.LogErrorC("Expected constant name");
        }
        final String name = lex.IdentifierStr;
        Next();

        if (curTok != Lexer.Tokens.ASSIGN.value && curTok != Lexer.Tokens.SAME.value) {
            return ExprAST.LogErrorC("Expected '->' or '=' in constant declaration");
        }
        Next();

        final ExprAST value = ParseExpression();
        if (value == null) return null;

        return new ExprAST.ConstantAST(type, name, value);
    }

    /**
     * Checks if a token is a type token
     * @param t the token to check
     * @return true if the token is a type token, false otherwise
     */
    private final boolean isTypeToken(final Lexer.Tokens t) {
        return t == Lexer.Tokens.INT64
                || t == Lexer.Tokens.INT32
                || t == Lexer.Tokens.FLOAT32
                || t == Lexer.Tokens.FLOAT64
                || t == Lexer.Tokens.STRING
                || t == Lexer.Tokens.BOOL
                || t == Lexer.Tokens.UINT32
                || t == Lexer.Tokens.UINT64;
    }

    /**
     * Helper method to get the next token from the lexer
     * @return the next token
     * @throws IOException
     */
    private final int Next() throws IOException {
        curTok = lex.GetTok();
        return curTok;
    }

    /**
     * Parses a number expression
     * @param numVal double value
     * @return a number Expression
     * @throws IOException
     */
    public final ExprAST ParseNumberExpr(final double numVal) throws IOException {
        final ExprAST result = new ExprAST.NumberExprAST(numVal);
        Next();
        return result;
    }

    /**
     * Parses a parenthesis expression
     * @return an Expression node or an error
     * @throws IOException
     */
    public final ExprAST ParseParenExpr() throws IOException {
        Next();
        final ExprAST V = ParseExpression();
        if (V == null) return null;

        if (curTok != Lexer.Tokens.CLOSE_PAR.value) {
            return ExprAST.LogError("expected ')'");
        }
        Next();
        return V;
    }

    /**
     * Parses an identifier expression
     * @return an Expression node or an error
     * @throws IOException
     */
    public final ExprAST ParseIdentifierExpr() throws IOException {
        final String idName = lex.IdentifierStr;
        Next();

        if (curTok != Lexer.Tokens.OPEN_PAR.value) {
            return new ExprAST.VariableExprAST(idName);
        }

        Next();
        final List<ExprAST> args = new ArrayList<>();
        if (curTok != Lexer.Tokens.CLOSE_PAR.value) {
            while (true) {
                final ExprAST arg = ParseExpression();
                if (arg != null) {
                    args.add(arg);
                } else {
                    return null;
                }

                if (curTok == Lexer.Tokens.CLOSE_PAR.value) break;

                if (curTok != Lexer.Tokens.PARAMS.value) {
                    return ExprAST.LogError("Expected ')' or '|' in argument list");
                }
                Next();
            }
        }
        Next();
        return new ExprAST.CallExprAST(idName, args);
    }

    /**
     * Parses a primary expression (general one)
     * @return an Expression node or an error
     * @throws IOException
     */
    public final ExprAST ParsePrimary() throws IOException {
        if (curTok == Lexer.Tokens.OPEN_FUNC.value) {
            Next();
        }

        if (curTok == Lexer.Tokens.IDENTIFIER.value) {
            return ParseIdentifierExpr();
        } else if (curTok == Lexer.Tokens.NUMBER.value) {
            return ParseNumberExpr(lex.NumVal);
        } else if (curTok == Lexer.Tokens.OPEN_PAR.value) {
            return ParseParenExpr();
        } else {
            return ExprAST.LogError("unknown token when expecting an expression: " + curTok);
        }
    }

    /**
     * Returns the precedence of the current token
     * @return the precedence of the current token or -1 if it's not a binary operator
     */
    public final int GetTokPrecedence() {
        final Integer TokPrec = BinOpPrecedence.get(curTok);
        if (TokPrec == null || TokPrec <= 0) return -1;
        return TokPrec;
    }

    /**
     * Parses a Ret instruction
     * @return an RetAST node
     * @throws IOException
     */
    public final ExprAST ParseRet() throws IOException {
        Next();
        final ExprAST rightSide = ParseExpression();
        return new ExprAST.RetAST(Objects.requireNonNullElseGet(rightSide, () -> new ExprAST.NumberExprAST(0.0)));
    }

    /**
     * Parses a statement
     * @return , in reality it just calls other functions
     * @throws IOException
     */
    public final ExprAST ParseStatement() throws IOException {
        final Lexer.Tokens token = Lexer.Tokens.fromValues(curTok);
        if (token == Lexer.Tokens.RET) {
            return ParseRet();
        } else if (isTypeToken(token)) {
            return ParseVariable();
        } else if (token == Lexer.Tokens.CN) {
            return ParseConstant();
        } else {
            return ParseExpression();
        }
    }

    /**
     * Parses an expression (general use)
     * @return null or calls other functions
     * @throws IOException
     */
    public final ExprAST ParseExpression() throws IOException {
        final ExprAST LHS = ParsePrimary();
        if (LHS == null) return null;

        return ParseBinOpRHS(0, LHS);
    }

    /**
     * Parses a binary operation
     * @param ExprPrec precedence of the operator
     * @param LHS left side
     * @return null or a BinaryExprAST node
     * @throws IOException
     */
    public final ExprAST ParseBinOpRHS(final int ExprPrec, ExprAST LHS) throws IOException {
        while (true) {
            final int TokPrec = GetTokPrecedence();

            if (TokPrec < ExprPrec) {
                return LHS;
            }

            final int BinOp = curTok;
            Next();

            ExprAST RHS = ParsePrimary();
            if (RHS == null) return null;

            final int NextPrec = GetTokPrecedence();
            if (TokPrec < NextPrec) {
                RHS = ParseBinOpRHS(TokPrec + 1, RHS);
                if (RHS == null) return null;
            }

            LHS = new ExprAST.BinaryExprAST(Lexer.Tokens.fromValues(BinOp).description.charAt(0), LHS, RHS);
        }
    }

    /**
     * Parses a function prototype
     * @return an error or a PrototypeAST node
     * @throws IOException
     */
    public final  ExprAST.PrototypeAST ParsePrototype() throws IOException {
        if (curTok != Lexer.Tokens.IDENTIFIER.value) {
            return ExprAST.LogErrorP("Expected function name in prototype");
        }

        final String fnName = lex.IdentifierStr;
        Next();

        final List<String> argNames = new ArrayList<>();

        if (curTok != Lexer.Tokens.CLOSE_PAR.value) {
            while (Next() == Lexer.Tokens.IDENTIFIER.value) {
                argNames.add(lex.IdentifierStr);
            }
        }

        if (curTok != Lexer.Tokens.CLOSE_PAR.value) {
            return ExprAST.LogErrorP("Expected ')' in prototype");
        }

        Next();
        functionCount.put(fnName, 0);

        return new ExprAST.PrototypeAST(fnName, argNames);
    }

    /**
     * Parses a function definition
     * @return null or a FunctionAST node
     * @throws IOException
     */
    public final ExprAST.FunctionAST ParseDefinition() throws IOException {
        Next();
        final ExprAST.PrototypeAST proto = ParsePrototype();
        if (proto == null) return null;

        if (curTok == Lexer.Tokens.OPEN_FUNC.value) {
            Next();
        }

        final List<ExprAST> bodyStatements = new ArrayList<>();
        while (curTok != Lexer.Tokens.CLOSE_FUNC.value && curTok != Lexer.Tokens.EOF.value) {
            if (curTok == ';') {
                Next();
                continue;
            }
            final ExprAST stmt = ParseStatement();
            if (stmt != null) {
                bodyStatements.add(stmt);
            } else {
                break;
            }
        }

        if (curTok == Lexer.Tokens.CLOSE_FUNC.value) {
            Next();
        }

        if (bodyStatements.isEmpty()) return null;

        final ExprAST body = bodyStatements.size() == 1 ? bodyStatements.getFirst() : new ExprAST.BlockAST(bodyStatements);
        return new ExprAST.FunctionAST(proto, body);
    }
}