package Tests;

import org.junit.jupiter.api.Test;
import src.Codegen.IRBuilder;
import src.Parser.Parser;
import src.lexer.Lexer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

// some tests may not work, ok
public class TestCodeGen {

    private void createFile(final String code, final String name){

        try (FileWriter file = new FileWriter(name)) {
            file.write(code);
        } catch (IOException e) {
            System.out.println("an error occurred");
            e.printStackTrace();
        }
    }

    private final String readFile(final String name) { // maybe later on
        final File file = new File(name);
        final StringBuilder data = new StringBuilder();

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                data.append(scanner.nextLine());
            }
        } catch (IOException e) {
            System.out.println("Error");
            e.printStackTrace();
        }

        return String.valueOf(data);
    }

    private final String codeGen(final String code) throws IOException {
        final InputStream input = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        final Lexer lexer = new Lexer(input);
        final Parser parser = new Parser(lexer);
        final src.AST.FunctionAST mainFunction = parser.ParseDefinition();
        final IRBuilder builder = new IRBuilder();
        final src.AST.Value result = mainFunction.Codegen(builder);
        return result == null ? null : result.text();
    }

    @Test
    public void testSimpleFunction() throws IOException {
        final String code = "proc test() {\n 3+4 \n }";

        final String llvmIR = codeGen(code);
        final String expected = "define private dso_local double @test() {\nentry:\n %1 = add i32 3, 4\n ret double %1\n}\n";
        assertNotNull(llvmIR);
        assertEquals(expected, llvmIR);
    }

    @Test
    public void testFunctionWithReturnStatement() throws IOException {
        final String code = "proc main() {\n ret 40 + 32 \n }";
        final String llvmIR = codeGen(code);
        final String expected = "define dso_local i32 @main() {\nentry:\n %1 = add i32 40, 32\n ret i32 %1\n}\n";
        assertNotNull(llvmIR);
        assertEquals(expected, llvmIR);
    }

    @Test
    public void testFunctionWithReturnNonMain() throws IOException {
        final String code = "proc foo() {\n ret 10 * 2 \n }";
        final String llvmIR = codeGen(code);
        final String expected = "define private dso_local double @foo() {\nentry:\n %1 = mul i32 10, 2\n ret double %1\n}\n";
        assertNotNull(llvmIR);
        assertEquals(expected, llvmIR);
    }

    @Test
    public void testVarsWithoutAssignment() throws IOException {
        final String code = "proc main() {\n int32 var \n ret 0 \n }";
        final String llvmIR = codeGen(code);
        final String expected = "define dso_local i32 @main() {\nentry:\n %var = alloca i32\n store i32 0, ptr %var\n ret i32 0\n}\n";
        assertNotNull(llvmIR);
        assertEquals(expected, llvmIR);
    }

    @Test
    public void testVarsWithAssignment() throws IOException {
        final String code = "proc main() {\n int32 var -> 3 \n ret 0 \n }";
        final String llvmIR = codeGen(code);
        final String expected = "define dso_local i32 @main() {\nentry:\n %var = alloca i32\n store i32 3, ptr %var\n ret i32 0\n}\n";
        assertNotNull(llvmIR);
        assertEquals(expected, llvmIR);
    }

    @Test
    public void testVarsWithAssignmentWithRet() throws IOException {
        final String code = "proc main() {\n int32 var -> 3 \n ret var \n }";
        final String llvmIR = codeGen(code);
        final String expected = "define dso_local i32 @main() {\nentry:\n %var = alloca i32\n store i32 3, ptr %var\n %1 = load i32, ptr %var\n ret i32 %1\n}\n";
        assertNotNull(llvmIR);
        assertEquals(expected, llvmIR);
    }

    @Test
    public void testMoreVars() throws IOException {
        final String code = "proc main() {\n int32 var\n int32 var2\n ret 0 \n }";
        final String llvmIR = codeGen(code);
        final String expected = "define dso_local i32 @main() {\nentry:\n %var = alloca i32\n store i32 0, ptr %var\n %var2 = alloca i32\n store i32 0, ptr %var2\n ret i32 0\n}\n";
        assertNotNull(llvmIR);
        assertEquals(expected, llvmIR);
    }

    @Test
    public void testVarsWithDifferentIntegers() throws IOException {
        final String code = "proc main() {\n int32 var -> 3\n int64 var2 -> 4\n ret 0\n }";
        final String llvmIR = codeGen(code);
        final String expected = "define dso_local i32 @main() {\nentry:\n %var = alloca i32\n store i32 3, ptr %var\n %var2 = alloca i64\n store i64 4, ptr %var2\n ret i32 0\n}\n";
        assertNotNull(llvmIR);
        assertEquals(expected, llvmIR);
    }

    @Test
    public void testVarsWithDifferentTypesWithRet() throws IOException {
        final String code = "proc main() {\n int32 var -> 3\n flt32 var2 -> 4\n flt64 var3 -> 4\n ret var3\n }";
        final String llvmIR = codeGen(code);
        final String expected = "define dso_local i32 @main() {\nentry:\n %var = alloca i32\n store i32 3, ptr %var\n %var2 = alloca float\n store float 4, ptr %var2\n %var3 = alloca double\n store double 4, ptr %var3\n %1 = load double, ptr %var3\n %2 = fptosi double %1 to i32\n ret i32 %2\n}\n";
        assertNotNull(llvmIR);
        assertEquals(expected, llvmIR);
    }

    @Test
    public void testConstantDefinitionWithDifferentTypes() throws IOException {
        final String code = "proc main() {\n cn int32 const -> 3\n cn flt32 floatConst -> 1.0\n ret 0}";
        final String llvmIR = codeGen(code);
        final String expected = "define dso_local i32 @main() {\nentry:\n %const = alloca i32\n store i32 3, ptr %const\n %floatConst = alloca float\n store float 1, ptr %floatConst\n ret i32 0\n}\n";
        assertNotNull(llvmIR);
        assertEquals(expected, llvmIR);
    }

    @Test
    public void testConstantDefinitionWithRet() throws IOException {
        final String code = "proc main() {\n cn int32 const -> 3\n cn flt32 floatConst -> 1.0\n ret const}";
        final String llvmIR = codeGen(code);
        final String expected = "define dso_local i32 @main() {\nentry:\n %const = alloca i32\n store i32 3, ptr %const\n %floatConst = alloca float\n store float 1, ptr %floatConst\n %1 = load i32, ptr %const\n ret i32 %1\n}\n";
        assertNotNull(llvmIR);
        assertEquals(expected, llvmIR);
    }

    @Test
    public void testFunctionCallInsideMain() throws IOException {
        final String code = "proc main() {\n foo() \n ret 0 \n }";
        final String llvmIR = codeGen(code);
        assertNotNull(llvmIR);
        assertTrue(llvmIR.contains("call double @foo()"));
    }

    @Test
    public void testFunctionWithParameters() throws IOException {
        final String code = "proc foo(int32 x | int32 y) {\n ret 0 \n }";
        final String llvmIR = codeGen(code);
        assertNotNull(llvmIR);
        assertTrue(llvmIR.contains("@foo(i32 %x, i32 %y)"));
    }

    @Test
    public void testCallWithParameters() throws IOException {
        final String code = "proc foo(int32 x | int32 y) {\n ret 0 \n }\n" + "proc main() {\n foo(1 | 2) \n ret 0 \n }";

        final InputStream input =
                new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        final Lexer lexer = new Lexer(input);
        final Parser parser = new Parser(lexer);

        final src.AST.FunctionAST foo = parser.ParseDefinition();
        foo.Codegen(new IRBuilder());

        final src.AST.FunctionAST main = parser.ParseDefinition();
        final String llvmIR = String.valueOf(main.Codegen(new IRBuilder()));

        assertNotNull(llvmIR);
        assertTrue(llvmIR.contains("call double @foo(i32 "));
    }

    @Test
    public void testSimpleForLoop() throws IOException {
        final String code = "proc main() {\n"
                + "  for (int32 i -> 0 | i < 10 | i + 1) {\n"
                + "    int32 x -> 5\n"
                + "  }\n"
                + "  ret 0\n"
                + "}";

        final String llvmIR = codeGen(code);
        assertNotNull(llvmIR);
        assertTrue(llvmIR.contains("for.cond"));
        assertTrue(llvmIR.contains("for.body"));
        assertTrue(llvmIR.contains("for.inc"));
        assertTrue(llvmIR.contains("for.end"));
        assertTrue(llvmIR.contains("icmp slt i32"));
        assertTrue(llvmIR.contains("br i1"));
    }
}
