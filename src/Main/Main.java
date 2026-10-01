package src.Main;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import src.AST.ExprAST;
import src.Codegen.IRBuilder;
import src.Parser.Parser;
import src.lexer.Lexer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Callable;

@Command(name = "compiler", mixinStandardHelpOptions = true, version = "1", description = "Compiler z-code")
class CompilerCmd implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = "Source to file to be compiled")
    private File sourceFile;

    @CommandLine.Option(names = {"-o", "--out"}, description = "out directory")
    private String outPath;

    @Override
    public Integer call() throws Exception {
        String zCode = Files.readString(sourceFile.toPath());

        System.out.println("Compilation");

        try {
            InputStream input = new ByteArrayInputStream(zCode.getBytes(StandardCharsets.UTF_8));

            if (outPath == null) {
                outPath = "output.ll";
            }

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

            System.out.println(llvmIR);
            File irFile = new File(outPath);
            Files.writeString(irFile.toPath(), llvmIR);
            System.out.println("File IR built successfully: " + outPath);

            compileToBinary(irFile.getAbsolutePath(), outPath);

            return 0;

        } catch (IOException e) {
            e.printStackTrace();
            return 1;
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

public class Main {
    public static void main(String[] args) {
        new CommandLine(new CompilerCmd()).execute(args);
    }
}