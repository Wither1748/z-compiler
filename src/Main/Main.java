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
    @CommandLine.Parameters(index = "0", description = "Source file to be compiled")
    private File sourceFile;

    @CommandLine.Option(names = {"-o", "--out"}, description = "Output path")
    private String outPath;

    @Override
    public Integer call() throws Exception {
        final String zCode = Files.readString(sourceFile.toPath());

        System.out.println("Starting compilation...");

        try {
            InputStream input = new ByteArrayInputStream(zCode.getBytes(StandardCharsets.UTF_8));

            final String llFilePath;
            final String binaryName;
            if (outPath == null) {
                llFilePath = "output.ll";
                binaryName = "output";
            } else if (outPath.endsWith(".ll")) {
                llFilePath = outPath;
                binaryName = outPath.substring(0, outPath.length() - 3);
            } else {
                llFilePath = outPath + ".ll";
                binaryName = outPath;
            }

            final Lexer lexer = new Lexer(input);
            final Parser parser = new Parser(lexer);

            final ExprAST.FunctionAST mainFunction = parser.ParseDefinition();

            if (mainFunction == null) {
                System.err.println("Syntax error");
                return 1;
            }

            final IRBuilder builder = new IRBuilder();
            final String llvmIR = mainFunction.Codegen(builder);

            if (llvmIR == null) {
                System.err.println("Error building the IR");
                return 1;
            }

            System.out.println(llvmIR);
            final File irFile = new File(llFilePath);
            Files.writeString(irFile.toPath(), llvmIR);
            System.out.println("File IR built successfully: " + llFilePath);

            compileToBinary(irFile.getAbsolutePath(), binaryName);

            return 0;

        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
    }

    private static void compileToBinary(String llFilePath, String outputBinaryName) {
        System.out.println("Launching Clang...");

        final ProcessBuilder processBuilder = createProcessBuilder(llFilePath, outputBinaryName);

        try {
            final Process process = processBuilder.start();
            final int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Executable created: ./" + outputBinaryName);
            } else {
                System.err.println("Error during compilation. Exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to launch Clang");
            e.printStackTrace();
        }
    }

    private static ProcessBuilder createProcessBuilder(String llFilePath, String outputBinaryName) {
        final ProcessBuilder processBuilder = new ProcessBuilder(
                "clang",
                "-O3",
                "-flto",
                "-march=native",
                "-funroll-loops",
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