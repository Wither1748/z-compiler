package src.lexer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Lexer {

    public enum Tokens {
        FUNC("proc", -1),
        INT64("int64", -2),
        NUMBER("unused", -3), // number returns
        RET("ret", -4),
        INT32("int32", -5),
        FLOAT32("flt32", -6),
        FLOAT64("flt64", -7),
        STRING("str", -8),
        BOOL("01", -9),
        UINT32("uint32", -10),
        UINT64("uint64", -11),
        MUL("*", -12),
        DIV("/", -13),
        ADD("+", -14),
        SUB("-", -15),
        ASSIGN("->", -30),
        IF("if", -16),
        EIF("eif", -17),
        ELSE("else", -18),
        FOR("for", -19),
        OPEN_PAR("(", -20),
        CLOSE_PAR(")", -21),
        OPEN_FUNC("{", -22),
        CLOSE_FUNC("}", -23),
        LESS("<", -24),
        MORE(">", -25),
        IMPORT("import", -26),
        ARRAY_OPEN("[", -27),
        ARRAY_CLOSE("]", -28),
        PARAMS("|", -29),
        IDENTIFIER("id", -34),
        EOF("eof", -31),
        SAME("=", -32),
        CN("cn", -35);

        public final String description;
        public final int value;

        Tokens(String description, int value) {
            this.description = description;
            this.value = value;
        }

        public static Tokens fromValues(int value) {
            return java.util.Arrays.stream(values()).filter(token -> token.value == value).findFirst().orElse(null);
        }
    }

    // status vars
    private int LastChar = ' ';
    public String IdentifierStr;
    public double NumVal;

    private final InputStream input;

    private static final Map<String, Tokens> keywordMap = new HashMap<>();

    static {
        for (Tokens t : Tokens.values()) {
            keywordMap.put(t.description, t);
        }
    }


    public Lexer(InputStream input) {
        this.input = input;
    }

    public int GetTok() throws IOException {

        // skips whitespaces
        while (Character.isWhitespace(LastChar)) {
            LastChar = input.read();
        }

        // EOF for some fucking reason in java is -1
        if (LastChar == -1) {
            return Tokens.EOF.value;
        }

        // assignment operator ->
        if (LastChar == '-') {
            LastChar = input.read();

            if (LastChar == '>') {
                LastChar = input.read();
                return Tokens.ASSIGN.value;
            }

            return Tokens.SUB.value;
        }

        // chars

        if (Character.isAlphabetic(LastChar)) {
                StringBuilder sb = new StringBuilder();

                do {
                    sb.append((char) LastChar);
                    LastChar = input.read();
                } while (Character.isLetterOrDigit(LastChar));

                IdentifierStr = sb.toString();

                // hashmap should be faster
                Tokens token = keywordMap.get(IdentifierStr);
                if (token != null && token != Tokens.IDENTIFIER && token != Tokens.NUMBER && token != Tokens.EOF) {
                    return token.value;
                }

                // if it's not a word it must be an identifier ig
                return Tokens.IDENTIFIER.value;
        }

            // numbers
            if (Character.isDigit(LastChar) || LastChar == '.') {
                StringBuilder NumStr = new StringBuilder();

                do {
                    NumStr.append((char) LastChar);
                    LastChar = input.read();
                } while (Character.isDigit(LastChar) || LastChar == '.');

                // parse float numbers
                NumVal = Double.parseDouble(NumStr.toString());
                return Tokens.NUMBER.value;
            }

        // Comments
        if (LastChar == '/') {
            LastChar = input.read();

            if (LastChar == '/') {
                do {
                    LastChar = input.read();
                } while (LastChar != -1 && LastChar != '\n' && LastChar != '\r');

                return GetTok();
            }

            return Tokens.DIV.value;
        }

        Tokens symbol = keywordMap.get(Character.toString((char) LastChar));

        if (symbol != null) {
            LastChar = input.read();
            return symbol.value;
        }

            // returns the ASCII if unrecognized
            int ThisChar = LastChar;

            // next one
            LastChar = input.read();

            return ThisChar;
        }

    }