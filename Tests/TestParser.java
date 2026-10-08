package Tests;

import org.junit.jupiter.api.Test;
import src.AST.ExprAST;
import src.Parser.Parser;
import src.lexer.Lexer;
import src.AST.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class TestParser {

    private final Parser createParser(final String input) throws IOException {
        final Lexer lex = new Lexer(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        return new Parser(lex);
    }
    @Test
    void testSimpleNumber() throws IOException {
        final Parser parser = createParser("42");
        final ExprAST ast = parser.ParseExpression();
        assertNotNull(ast);
        assertInstanceOf(Number.class, ast);
    }

    @Test
    void testBinaryPrecedence() throws IOException {
        final Parser parser = createParser("5 + 10 * 2");
        final ExprAST ast = parser.ParseExpression();
        assertNotNull(ast);
        assertInstanceOf(BinaryExprAST.class, ast);
    }

    @Test
    void testParenthesis() throws IOException {
        final Parser parser = createParser("(5 + 10) * 2");
        final ExprAST ast = parser.ParseExpression();
        assertNotNull(ast);
        assertInstanceOf(BinaryExprAST.class, ast);
    }

    @Test
    void testFunctionCall() throws IOException {
        final Parser parser = createParser("foo(x | y)");
        final ExprAST ast = parser.ParseExpression();
        assertNotNull(ast);
        assertInstanceOf(CallExprAST.class, ast);
    }

    @Test
    void testLeftParsing() throws IOException {
        final Parser parser = createParser("5 - 3 - 1");
        final ExprAST ast = parser.ParseExpression();
        assertNotNull(ast);
    }

    @Test
    void testFunctionCallWithoutArgs() throws IOException {
        final Parser parser = createParser("foo()");
        final ExprAST ast = parser.ParseExpression();
        assertNotNull(ast);
        assertInstanceOf(CallExprAST.class, ast);
    }

    @Test
    void testFunctionCallWithExpression() throws IOException {
        final Parser parser = createParser("foo(x + 5 | 42)");
        final ExprAST ast = parser.ParseExpression();
        assertNotNull(ast);
    }

    @Test
    void testErrorUnclosedParenthesis() throws IOException {
        final Parser parser = createParser("(5 + x");
        final ExprAST ast = parser.ParseExpression();
        assertNull(ast);
    }

    @Test
    void testOrphanOperation() throws IOException {
        final Parser parser = createParser("5 +");
        final ExprAST ast = parser.ParseExpression();
        assertNull(ast);
    }

    @Test
    void testUnexpectedToken() throws IOException {
        final Parser parser = createParser("+ 5");
        final ExprAST ast = parser.ParseExpression();
        assertNull(ast);
    }

    @Test
    void testSimpleConstant() throws IOException {
        // cn int64 PI -> 314
        final Parser parser = createParser("cn int64 PI -> 314");
        final DeclaratorExprAST c = parser.ParseConstant();

        assertNotNull(c);
    }

    @Test
    void testConstantWithEquals() throws IOException {
        // 也支持 = 赋值
        final Parser parser = createParser("cn int32 X = 42");
        final DeclaratorExprAST c = parser.ParseConstant();

        assertNotNull(c);
    }

    @Test
    void testConstantWithExpressionValue() throws IOException {
        // 值可以是表达式
        final Parser parser = createParser("cn int64 X -> 2 + 3");
        final DeclaratorExprAST c = parser.ParseConstant();

        assertNotNull(c);
    }

    @Test
    void testConstantWithVariableValue() throws IOException {
        // 值可以是变量引用
        final Parser parser = createParser("cn int64 Y -> X");
        final DeclaratorExprAST c = parser.ParseConstant();

        assertNotNull(c);
    }

    @Test
    void testConstantFloatType() throws IOException {
        final Parser parser = createParser("cn flt64 PI -> 3.14");
        final DeclaratorExprAST c = parser.ParseConstant();

        assertNotNull(c);
    }

    @Test
    void testConstantStringType() throws IOException {
        final Parser parser = createParser("cn str NAME -> 42");
        final DeclaratorExprAST c = parser.ParseConstant();

        assertNotNull(c);
    }

    @Test
    void testConstantMissingType() throws IOException {
        // cn PI -> 314  缺少类型
        final Parser parser = createParser("cn PI -> 314");
        final DeclaratorExprAST c = parser.ParseConstant();
        assertNull(c);
    }

    @Test
    void testConstantMissingName() throws IOException {
        // cn int64 -> 314  缺少名字
        final Parser parser = createParser("cn int64 -> 314");
        final DeclaratorExprAST c = parser.ParseConstant();
        assertNull(c);
    }

    @Test
    void testConstantMissingAssign() throws IOException {
        // cn int64 PI 314  缺少 -> 或 =
        final Parser parser = createParser("cn int64 PI 314");
        final DeclaratorExprAST c = parser.ParseConstant();
        assertNull(c);
    }

    @Test
    void testMainLoopWithConstants() throws IOException {
        // MainLoop 能连续处理多个常量
        final String code = """
                cn int64 A -> 1
                cn int64 B -> 2
                cn flt64 C -> 3.14
                """;

        final Parser parser = createParser(code);
        assertDoesNotThrow(parser::MainLoop);
    }

    @Test
    void testMainLoopConstantAndFunction() throws IOException {
        // 常量和函数混在一起
        final String code = "cn int64 PI -> 314\nproc foo(int64 x | int64 y) { ret x }\n";

        final Parser parser = createParser(code);
        assertDoesNotThrow(parser::MainLoop);
    }

    @Test
    void testVariableDeclaration() throws IOException {
        final String code = "int32 var";
        final Parser parser = createParser(code);
        final DeclaratorExprAST v = parser.ParseVariable();
        assertInstanceOf(DeclaratorExprAST.class, v);
    }

    @Test
    void testVariableWithAssignment() throws IOException {
        final String code = "flt32 var = 2";
        final Parser parser = createParser(code);
        final DeclaratorExprAST v = parser.ParseVariable();
        assertInstanceOf(DeclaratorExprAST.class, v);
    }
}