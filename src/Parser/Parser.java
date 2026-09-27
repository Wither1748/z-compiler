package src.Parser;

import src.lexer.Lexer;
import src.AST.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// needs to be fixed
public class Parser {
    private int curTok;
    private Lexer lex = null;
    public static final HashMap<Integer, Integer> BinOpPrecedence = new HashMap<>();
    static { // TODO check this
        BinOpPrecedence.put(Lexer.Tokens.LESS.value, 10);
        BinOpPrecedence.put(Lexer.Tokens.MUL.value, 40);
        BinOpPrecedence.put(Lexer.Tokens.SUB.value, 20);
        BinOpPrecedence.put(Lexer.Tokens.DIV.value, 40);
        BinOpPrecedence.put(Lexer.Tokens.ADD.value, 20);
        BinOpPrecedence.put(Lexer.Tokens.SAME.value, 20);
        BinOpPrecedence.put(Lexer.Tokens.MORE.value, 10);
    }


    public Parser(Lexer lex) throws IOException {
        this.lex = lex;
        this.curTok = lex.GetTok();
    }


    public  void MainLoop() throws IOException {
        Next();
        while (true) {
            Lexer.Tokens token = Lexer.Tokens.fromValues(curTok);

            if (token == Lexer.Tokens.EOF) {
                return;
            }
            else if (curTok == ';') {
                Next();
            }
            else if (token == Lexer.Tokens.FUNC) {
                HandleDefinition();
            }
            else {
                ExprAST.LogError("Unknown error (default case)");
                Next();
            }

        }
    }

    public  void HandleDefinition() throws IOException {
        ExprAST.FunctionAST e = ParseDefinition();
        if (e != null ) {
            System.out.println("Parsed a function");
        }
        else {
            Next();
        }
    }

    private  int Next() throws IOException {
        curTok = lex.GetTok();
        return curTok;
    }

    public  ExprAST ParseNumberExpr(double numVal) throws IOException {
        ExprAST result = new ExprAST.NumberExprAST(numVal);

        Next();

        return result;
    }

    public  ExprAST ParseParenExpr() throws IOException {
        Next();

        ExprAST V = ParseExpression();

        if ( V == null ){
            return null;
        }

        if (curTok != ')') {
            return ExprAST.LogError("expected ')'");
        }
        Next();
        return V;
    }

    public  ExprAST ParseIdentifierExpr() throws IOException {
        String IdName = lex.IdentifierStr;

        Next();

        if (curTok != Lexer.Tokens.OPEN_PAR.value) {
            return new ExprAST.VariableExprAST(IdName);
        }

        Next();

        List<ExprAST> args = new ArrayList<>();
        if (curTok != Lexer.Tokens.OPEN_PAR.value) {
            while(true) {
                ExprAST arg = ParseExpression();
                if (arg != null) {
                    args.add(arg);
                }
                else {
                    return null;
                }

                if (curTok == Lexer.Tokens.CLOSE_PAR.value) {
                    break;
                }

                if (curTok != Lexer.Tokens.PARAMS.value) {
                    return ExprAST.LogError("Expected ')' or '|' in argument list");
                }

                Next();
            }
        }

        Next();

        return new ExprAST.CallExprAST(IdName, args);
    }

    public  ExprAST ParsePrimary() throws IOException {
        //Lexer.Tokens token = Lexer.Tokens.fromValues(curTok);

        if (curTok == Lexer.Tokens.OPEN_FUNC.value) {
            Next();
        }

        if (curTok == Lexer.Tokens.IDENTIFIER.value) {
            return ParseIdentifierExpr();
        }
        else if (curTok == Lexer.Tokens.NUMBER.value) {
            return ParseNumberExpr(lex.NumVal);
        }
        else if (curTok == Lexer.Tokens.OPEN_PAR.value) {
            return ParseParenExpr();
        }
        else {
            return ExprAST.LogError("unknown token when expecting an expression" + curTok);
        }
    }

    public  int GetTokPrecedence() {
        Integer TokPrec = BinOpPrecedence.get(curTok);
        if (TokPrec == null || TokPrec <= 0) return -1;
        return TokPrec;
    }

    public  ExprAST ParseExpression() throws IOException {
        ExprAST LHS = ParsePrimary();
        if (LHS == null) {
            return null;
        }

        return ParseBinOpRHS(0, LHS);
    }

    public  ExprAST ParseBinOpRHS(int ExprPrec, ExprAST LHS) throws IOException {

        while (true) {
            int TokPrec = GetTokPrecedence();

            if (TokPrec < ExprPrec) {
                return LHS;
            }

            int BinOp = curTok;
            Next();

            ExprAST RHS = ParsePrimary();
            if (RHS == null) {
                return null;
            }

            int NextPrec = GetTokPrecedence();
            if (TokPrec < NextPrec) {
                RHS = ParseBinOpRHS(TokPrec+1, RHS);
                if (RHS == null) {
                    return null;
                }
            }

            LHS = new ExprAST.BinaryExprAST(Lexer.Tokens.fromValues(BinOp).description.charAt(0), LHS, RHS);

        }

    }

    public  ExprAST.PrototypeAST ParsePrototype() throws IOException {
        if (curTok != Lexer.Tokens.IDENTIFIER.value) {
            return ExprAST.LogErrorP("Expected function name in prototype");
        }

        String FnName = lex.IdentifierStr;
        Next();

        List<String> ArgNames = new ArrayList<>(List.of());

        if (curTok != Lexer.Tokens.CLOSE_PAR.value) {
            while (Next() == Lexer.Tokens.IDENTIFIER.value) {
                ArgNames.add(lex.IdentifierStr);
            }
        }

        if (curTok != Lexer.Tokens.CLOSE_PAR.value) {
            return ExprAST.LogErrorP("Expected ')' in prototype");
        }

        Next();

        return new ExprAST.PrototypeAST(FnName, ArgNames);
    }

    public  ExprAST.FunctionAST ParseDefinition() throws IOException {
        Next();
        ExprAST.PrototypeAST Proto = ParsePrototype();
        if (Proto == null) {
            return null;
        }

        ExprAST E = ParseExpression();
        if (E != null) {
            return new ExprAST.FunctionAST(Proto, E);
        }

        return null;
    }

    // TODO add import parsing

}
