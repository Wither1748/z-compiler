package Tests;

import src.lexer.Lexer;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class TestLexer {

    @Test
    void testProcDeclarationTokens() throws IOException {
        // for now (int64 fails because it gets treated as one string
        String code = "proc name ( int64 name | int64 name) { \n ret 281 \n }";

        final Lexer lex = new Lexer(new ByteArrayInputStream(code.getBytes()));

        assertEquals(Lexer.Tokens.FUNC.value, lex.GetTok());
        assertEquals(Lexer.Tokens.IDENTIFIER.value, lex.GetTok());
        assertEquals(Lexer.Tokens.OPEN_PAR.value, lex.GetTok());
        assertEquals(Lexer.Tokens.INT64.value, lex.GetTok());
        assertEquals(Lexer.Tokens.IDENTIFIER.value, lex.GetTok());
        assertEquals(Lexer.Tokens.PARAMS.value, lex.GetTok());
        assertEquals(Lexer.Tokens.INT64.value, lex.GetTok());
        assertEquals(Lexer.Tokens.IDENTIFIER.value, lex.GetTok());
        assertEquals(Lexer.Tokens.CLOSE_PAR.value, lex.GetTok());
        assertEquals(Lexer.Tokens.OPEN_FUNC.value, lex.GetTok());
        assertEquals(Lexer.Tokens.RET.value, lex.GetTok());
        assertEquals(Lexer.Tokens.NUMBER.value, lex.GetTok());
        assertEquals(Lexer.Tokens.CLOSE_FUNC.value, lex.GetTok());
    }

    @Test
    void testConditionalTokens() throws IOException {
        // for now (int64 fails because it gets treated as one string
        final String code = "if name > 64 { \n\n } eif name < 32 { \n \n } else { \n \n }";

        final Lexer lex = new Lexer(new ByteArrayInputStream(code.getBytes()));

        assertEquals(Lexer.Tokens.IF.value, lex.GetTok());
        assertEquals(Lexer.Tokens.IDENTIFIER.value, lex.GetTok());
        assertEquals(Lexer.Tokens.MORE.value, lex.GetTok());
        assertEquals(Lexer.Tokens.NUMBER.value, lex.GetTok());
        assertEquals(Lexer.Tokens.OPEN_FUNC.value, lex.GetTok());
        assertEquals(Lexer.Tokens.CLOSE_FUNC.value, lex.GetTok());
        assertEquals(Lexer.Tokens.EIF.value, lex.GetTok());
        assertEquals(Lexer.Tokens.IDENTIFIER.value, lex.GetTok());
        assertEquals(Lexer.Tokens.LESS.value, lex.GetTok());
        assertEquals(Lexer.Tokens.NUMBER.value, lex.GetTok());
        assertEquals(Lexer.Tokens.OPEN_FUNC.value, lex.GetTok());
        assertEquals(Lexer.Tokens.CLOSE_FUNC.value, lex.GetTok());
        assertEquals(Lexer.Tokens.ELSE.value, lex.GetTok());
        assertEquals(Lexer.Tokens.OPEN_FUNC.value, lex.GetTok());
        assertEquals(Lexer.Tokens.CLOSE_FUNC.value, lex.GetTok());
    }

    @Test
    void testAssignmentOperator() throws IOException {
        final String code = "->";

        final Lexer lex = new Lexer(new ByteArrayInputStream(code.getBytes()));

        assertEquals(Lexer.Tokens.ASSIGN.value, lex.GetTok());
    }

    @Test
    void testSlashComments() throws IOException {
        final String code = "// this is a comment\nret";

        final Lexer lex = new Lexer(new ByteArrayInputStream(code.getBytes()));

        assertEquals(Lexer.Tokens.RET.value, lex.GetTok());
    }

    @Test
    void testConstantToken() throws IOException {
        final String code = "cn int64 PI -> 314";
        final Lexer lex = new Lexer(new ByteArrayInputStream(code.getBytes()));

        assertEquals(Lexer.Tokens.CN.value, lex.GetTok());
        assertEquals(Lexer.Tokens.INT64.value, lex.GetTok());
        assertEquals(Lexer.Tokens.IDENTIFIER.value, lex.GetTok());
        assertEquals("PI", lex.IdentifierStr);
        assertEquals(Lexer.Tokens.ASSIGN.value, lex.GetTok());
        assertEquals(Lexer.Tokens.NUMBER.value, lex.GetTok());
        assertEquals(314.0, lex.NumVal);
    }

    @Test
    void testVariableAssignment() throws IOException {
        final String code = "int32 var -> 30";
        final Lexer lex = new Lexer(new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8)));

        assertEquals(Lexer.Tokens.INT32.value, lex.GetTok());
        assertEquals(Lexer.Tokens.IDENTIFIER.value, lex.GetTok());
        assertEquals(Lexer.Tokens.ASSIGN.value, lex.GetTok());
        assertEquals(Lexer.Tokens.NUMBER.value, lex.GetTok());
    }
    // assignments for now don't work '->' are considered two tokens
}