package Tests;

import src.AST.ExprAST;
import src.Parser.Parser;
import src.lexer.Lexer;
import org.junit.jupiter.api.Test;
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
        Parser parser = createParser("(5 + 10) * 2");
        ExprAST ast = parser.ParseExpression();
        assertNotNull(ast);
        assertInstanceOf(ExprAST.BinaryExprAST.class, ast);
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

    @Test
    void testFunctionCallWithoutArgs() throws IOException {
        Parser parser = createParser("foo()");
        ExprAST ast = parser.ParseExpression();
        assertNotNull(ast);
        assertInstanceOf(ExprAST.CallExprAST.class, ast);
    }

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

    @Test
    void testSimpleConstant() throws IOException {
        // cn int64 PI -> 314
        Parser parser = createParser("cn int64 PI -> 314");
        ExprAST.ConstantAST c = parser.ParseConstant();

        assertNotNull(c);
        assertEquals("int64", c.getType());
        assertEquals("PI", c.getName());
        assertInstanceOf(ExprAST.NumberExprAST.class, c.getValue());
    }

    @Test
    void testConstantWithEquals() throws IOException {
        // 也支持 = 赋值
        Parser parser = createParser("cn int32 X = 42");
        ExprAST.ConstantAST c = parser.ParseConstant();

        assertNotNull(c);
        assertEquals("int32", c.getType());
        assertEquals("X", c.getName());
        assertInstanceOf(ExprAST.NumberExprAST.class, c.getValue());
    }

    @Test
    void testConstantWithExpressionValue() throws IOException {
        // 值可以是表达式
        Parser parser = createParser("cn int64 X -> 2 + 3");
        ExprAST.ConstantAST c = parser.ParseConstant();

        assertNotNull(c);
        assertEquals("int64", c.getType());
        assertEquals("X", c.getName());
        assertInstanceOf(ExprAST.BinaryExprAST.class, c.getValue());
    }

    @Test
    void testConstantWithVariableValue() throws IOException {
        // 值可以是变量引用
        Parser parser = createParser("cn int64 Y -> X");
        ExprAST.ConstantAST c = parser.ParseConstant();

        assertNotNull(c);
        assertEquals("int64", c.getType());
        assertEquals("Y", c.getName());
        assertInstanceOf(ExprAST.VariableExprAST.class, c.getValue());
    }

    @Test
    void testConstantFloatType() throws IOException {
        Parser parser = createParser("cn flt64 PI -> 3.14");
        ExprAST.ConstantAST c = parser.ParseConstant();

        assertNotNull(c);
        assertEquals("flt64", c.getType());
        assertEquals("PI", c.getName());
    }

    @Test
    void testConstantStringType() throws IOException {
        Parser parser = createParser("cn str NAME -> 42");
        ExprAST.ConstantAST c = parser.ParseConstant();

        assertNotNull(c);
        assertEquals("str", c.getType());
        assertEquals("NAME", c.getName());
    }

    @Test
    void testConstantMissingType() throws IOException {
        // cn PI -> 314  缺少类型
        Parser parser = createParser("cn PI -> 314");
        ExprAST.ConstantAST c = parser.ParseConstant();
        assertNull(c);
    }

    @Test
    void testConstantMissingName() throws IOException {
        // cn int64 -> 314  缺少名字
        Parser parser = createParser("cn int64 -> 314");
        ExprAST.ConstantAST c = parser.ParseConstant();
        assertNull(c);
    }

    @Test
    void testConstantMissingAssign() throws IOException {
        // cn int64 PI 314  缺少 -> 或 =
        Parser parser = createParser("cn int64 PI 314");
        ExprAST.ConstantAST c = parser.ParseConstant();
        assertNull(c);
    }

    @Test
    void testMainLoopWithConstants() throws IOException {
        // MainLoop 能连续处理多个常量
        String code = "cn int64 A -> 1\n"
                + "cn int64 B -> 2\n"
                + "cn flt64 C -> 3.14\n";

        Parser parser = createParser(code);
        assertDoesNotThrow(parser::MainLoop);
    }

    @Test
    void testMainLoopConstantAndFunction() throws IOException {
        // 常量和函数混在一起
        String code = "cn int64 PI -> 314\n"
                + "proc foo(int64 x | int64 y) { ret x }\n";

        Parser parser = createParser(code);
        assertDoesNotThrow(parser::MainLoop);
    }
}