package src.Parser;

import src.lexer.Lexer;
import src.AST.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Parser {
    private int curTok;
    private Lexer lex = null;

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

    public static HashMap<String, Integer> functionCount = new HashMap<>();

    public Parser(Lexer lex) throws IOException {
        this.lex = lex;
        this.curTok = lex.GetTok();
    }

    public void MainLoop() throws IOException {
        while (true) {
            Lexer.Tokens token = Lexer.Tokens.fromValues(curTok);

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

    public void HandleDefinition() throws IOException {
        ExprAST.FunctionAST e = ParseDefinition();
        if (e != null) {
            System.out.println("Parsed a function");
        } else {
            Next();
        }
    }

    public void HandleConstant() throws IOException {
        ExprAST.ConstantAST c = ParseConstant();
        if (c != null) {
            System.out.println("Parsed a constant: " + c.getName());
        } else {
            Next();
        }
    }

    public void HandleVariable() throws IOException {
        ExprAST.VariableExprAST v = ParseVariable();
        if (v != null) {
            System.out.println("Parsed a variable: " + v.getName());
        } else {
            Next();
        }
    }

    public ExprAST.VariableExprAST ParseVariable() throws IOException {
        Lexer.Tokens type = Lexer.Tokens.fromValues(curTok);
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
        ExprAST val = ParseExpression();
        return new ExprAST.VariableExprAST(name, type.description, val);
    }

    public ExprAST.ConstantAST ParseConstant() throws IOException {
        Next();

        Lexer.Tokens typeTok = Lexer.Tokens.fromValues(curTok);
        if (typeTok == null || !isTypeToken(typeTok)) {
            return ExprAST.LogErrorC("Expected type after 'cn'");
        }
        String type = typeTok.description;
        Next();

        if (curTok != Lexer.Tokens.IDENTIFIER.value) {
            return ExprAST.LogErrorC("Expected constant name");
        }
        String name = lex.IdentifierStr;
        Next();

        if (curTok != Lexer.Tokens.ASSIGN.value && curTok != Lexer.Tokens.SAME.value) {
            return ExprAST.LogErrorC("Expected '->' or '=' in constant declaration");
        }
        Next();

        ExprAST value = ParseExpression();
        if (value == null) return null;

        return new ExprAST.ConstantAST(type, name, value);
    }

    private boolean isTypeToken(Lexer.Tokens t) {
        return t == Lexer.Tokens.INT64
                || t == Lexer.Tokens.INT32
                || t == Lexer.Tokens.FLOAT32
                || t == Lexer.Tokens.FLOAT64
                || t == Lexer.Tokens.STRING
                || t == Lexer.Tokens.BOOL
                || t == Lexer.Tokens.UINT32
                || t == Lexer.Tokens.UINT64;
    }

    private int Next() throws IOException {
        curTok = lex.GetTok();
        return curTok;
    }

    public ExprAST ParseNumberExpr(double numVal) throws IOException {
        ExprAST result = new ExprAST.NumberExprAST(numVal);
        Next();
        return result;
    }

    public ExprAST ParseParenExpr() throws IOException {
        Next();
        ExprAST V = ParseExpression();
        if (V == null) return null;

        if (curTok != Lexer.Tokens.CLOSE_PAR.value) {
            return ExprAST.LogError("expected ')'");
        }
        Next();
        return V;
    }

    public ExprAST ParseIdentifierExpr() throws IOException {
        String idName = lex.IdentifierStr;
        Next();

        if (curTok != Lexer.Tokens.OPEN_PAR.value) {
            return new ExprAST.VariableExprAST(idName);
        }

        Next();
        List<ExprAST> args = new ArrayList<>();
        if (curTok != Lexer.Tokens.CLOSE_PAR.value) {
            while (true) {
                ExprAST arg = ParseExpression();
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

    public ExprAST ParsePrimary() throws IOException {
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

    public int GetTokPrecedence() {
        Integer TokPrec = BinOpPrecedence.get(curTok);
        if (TokPrec == null || TokPrec <= 0) return -1;
        return TokPrec;
    }

    public ExprAST ParseRet() throws IOException {
        Next();
        ExprAST rightSide = ParseExpression();
        if (rightSide == null) {
            return new ExprAST.RetAST(new ExprAST.NumberExprAST(0.0));
        }
        return new ExprAST.RetAST(rightSide);
    }

    public ExprAST ParseStatement() throws IOException {
        Lexer.Tokens token = Lexer.Tokens.fromValues(curTok);
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

    public ExprAST ParseExpression() throws IOException {
        ExprAST LHS = ParsePrimary();
        if (LHS == null) return null;

        return ParseBinOpRHS(0, LHS);
    }

    public ExprAST ParseBinOpRHS(int ExprPrec, ExprAST LHS) throws IOException {
        while (true) {
            int TokPrec = GetTokPrecedence();

            if (TokPrec < ExprPrec) {
                return LHS;
            }

            int BinOp = curTok;
            Next();

            ExprAST RHS = ParsePrimary();
            if (RHS == null) return null;

            int NextPrec = GetTokPrecedence();
            if (TokPrec < NextPrec) {
                RHS = ParseBinOpRHS(TokPrec + 1, RHS);
                if (RHS == null) return null;
            }

            LHS = new ExprAST.BinaryExprAST(Lexer.Tokens.fromValues(BinOp).description.charAt(0), LHS, RHS);
        }
    }

    public ExprAST.PrototypeAST ParsePrototype() throws IOException {
        if (curTok != Lexer.Tokens.IDENTIFIER.value) {
            return ExprAST.LogErrorP("Expected function name in prototype");
        }

        String fnName = lex.IdentifierStr;
        Next();

        List<String> argNames = new ArrayList<>();

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

    public ExprAST.FunctionAST ParseDefinition() throws IOException {
        Next();
        ExprAST.PrototypeAST proto = ParsePrototype();
        if (proto == null) return null;

        if (curTok == Lexer.Tokens.OPEN_FUNC.value) {
            Next();
        }

        List<ExprAST> bodyStatements = new ArrayList<>();
        while (curTok != Lexer.Tokens.CLOSE_FUNC.value && curTok != Lexer.Tokens.EOF.value) {
            if (curTok == ';') {
                Next();
                continue;
            }
            ExprAST stmt = ParseStatement();
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

        ExprAST body = bodyStatements.size() == 1 ? bodyStatements.get(0) : new ExprAST.BlockAST(bodyStatements);
        return new ExprAST.FunctionAST(proto, body);
    }
}