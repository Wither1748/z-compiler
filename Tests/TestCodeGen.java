package Tests;

import org.junit.jupiter.api.Test;
import src.AST.ExprAST;
import src.Codegen.IRBuilder;
import src.Parser.Parser;
import src.lexer.Lexer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        final ExprAST.FunctionAST mainFunction = parser.ParseDefinition();
        final IRBuilder builder = new IRBuilder();
        return mainFunction.Codegen(builder);
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
}
