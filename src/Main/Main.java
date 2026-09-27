package src.Main;

import src.lexer.Lexer;
import src.Parser.Parser;
import src.AST.ExprAST;
import src.Codegen.IRBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
//import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        // Files.readString(Path.of(args[0]));
        String zCode = "proc test() { 5 + 3 }"; // it should fail because the main is unreferenced

        System.out.println("Compilation");

        try {
            InputStream input = new ByteArrayInputStream(zCode.getBytes(StandardCharsets.UTF_8));

            Lexer lexer = new Lexer(input);
            Parser parser = new Parser(lexer);

            ExprAST.FunctionAST mainFunction = parser.ParseDefinition();

            if (mainFunction == null) {
                System.err.println("Syntax error");
                System.exit(1);
            }

            IRBuilder builder = new IRBuilder();
            String llvmIR = mainFunction.Codegen(builder);

            if (llvmIR == null) {
                System.err.println("Error building the IR");
                System.exit(1);
            }

            File irFile = new File("output.ll");
            Files.writeString(irFile.toPath(), llvmIR);
            System.out.println("File IR built successfully: output.ll");

            compileToBinary(irFile.getAbsolutePath(), "z.out");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void compileToBinary(String llFilePath, String outputBinaryName) {
        System.out.println("Clang launching...");

        ProcessBuilder processBuilder = createProcessBuilder(llFilePath, outputBinaryName);

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("✅ Executable created: ./" + outputBinaryName);
            } else {
                System.err.println("❌ Error during compilation. Exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Fail to launch Clang");
            e.printStackTrace();
        }
    }

    private static ProcessBuilder createProcessBuilder(String llFilePath, String outputBinaryName) {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "clang",
                "-O3",
                "-flto",
                "-march=native",
                "-fprofile-instr-generate", // next -fprofile-instr-use if PGO
                "-funroll-loops",
                //"-fprefetch-loop-arrays", for some reason it's unsupported
                "-fno-rtti",
                "-fno-exceptions",
                "-mtune=native",
                llFilePath,
                "-o",
                outputBinaryName
        );

        processBuilder.inheritIO();
        return processBuilder;
    }
}