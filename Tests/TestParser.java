package Tests;

import org.junit.jupiter.api.Test;
import src.AST.ExprAST;
import src.Parser.Parser;
import src.lexer.Lexer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class TestParser {

    private Parser createParser(String input) throws IOException {
        Lexer lex = new Lexer(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        return new Parser(lex);
    }

    @Test
    void testSimpleNumber() throws IOException {
        Parser parser = createParser("42");
        ExprAST ast = parser.ParseExpression();

        assertNotNull(ast);
        assertInstanceOf(ExprAST.NumberExprAST.class, ast);
    }

    @Test
    void testBinaryPrecedence() throws IOException {
        Parser parser = createParser("5 + 10 * 2");
        ExprAST ast = parser.ParseExpression();

        assertNotNull(ast);
        assertInstanceOf(ExprAST.BinaryExprAST.class, ast);
    }

    @Test
    void testParenthesis() throws IOException {
        Parser parser = createParser("x -> (5 + 10) * 2");
        ExprAST ast = parser.ParseExpression();

        assertNotNull(ast);
        assertInstanceOf(ExprAST.VariableExprAST.class, ast);
    }

    @Test
    void testFunctionCall() throws IOException {
        Parser parser = createParser("foo(x | y)");
        ExprAST ast = parser.ParseExpression();

        assertNotNull(ast);
        assertInstanceOf(ExprAST.CallExprAST.class, ast);
    }

    @Test
    void testLeftParsing() throws IOException {
        Parser parser = createParser("5 - 3 - 1");
        ExprAST ast = parser.ParseExpression();

        assertNotNull(ast);
    }

    /*@Test
    void testFunctionCallWithoutArgs() throws IOException {
        Parser parser = createParser("foo()");
        ExprAST ast = parser.ParseExpression();
        assertNotNull(ast);
        assertInstanceOf(ExprAST.CallExprAST.class, ast);
    }*/

    @Test
    void testFunctionCallWithExpression() throws IOException {
        Parser parser = createParser("foo(x + 5 | 42)");
        ExprAST ast = parser.ParseExpression();
        assertNotNull(ast);
    }

    @Test
    void testErrorUnclosedParenthesis() throws IOException {
        Parser parser = createParser("(5 + x");
        ExprAST ast = parser.ParseExpression();
        assertNull(ast);
    }

    @Test
    void testOrphanOperation() throws IOException {
        Parser parser = createParser("5 +");
        ExprAST ast = parser.ParseExpression();
        assertNull(ast);
    }

    @Test
    void testUnexpectedToken() throws IOException {
        Parser parser = createParser("+ 5");
        ExprAST ast = parser.ParseExpression();
        assertNull(ast);
    }


}
