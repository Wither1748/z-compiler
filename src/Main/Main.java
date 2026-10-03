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
/**
 * Command line interface for the compiler
 */
class CompilerCmd implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = "Source file to be compiled")
    private File sourceFile;

    @CommandLine.Option(names = {"-o", "--out"}, description = "Output path")
    private String outPath;

    @Override
    /**
     * Main method for the compiler
     * @return 0 if the compilation is successful, 1 otherwise
     * @throws Exception if an I/O error occurs
     */
    public Integer call() throws Exception {
        final String zCode = Files.readString(sourceFile.toPath());

        System.out.println("Starting compilation...");

        try {
            InputStream input = new ByteArrayInputStream(zCode.getBytes(StandardCharsets.UTF_8));

            final String llFilePath;
            final String binaryName;
            final String optFilePath;

            if (outPath == null) {
                llFilePath = "output.ll";
                optFilePath = "output_opt.ll";
                binaryName = "output";
            } else if (outPath.endsWith(".ll")) {
                llFilePath = outPath;
                optFilePath = outPath.substring(0, outPath.length() - 3) + "_opt.ll";
                binaryName = outPath.substring(0, outPath.length() - 3);
            } else {
                llFilePath = outPath + ".ll";
                optFilePath = outPath + "_opt.ll";
                binaryName = outPath;
            }

            final Lexer lexer = new Lexer(input);
            final Parser parser = new Parser(lexer);
            final IRBuilder builder = new IRBuilder();

            final ExprAST.FunctionAST mainFunction = parser.ParseDefinition();

            if (mainFunction == null) {
                System.err.println("Syntax error");
                return 1;
            }

            final String llvmIR = mainFunction.Codegen(builder);

            if (llvmIR == null) {
                System.err.println("Error building the IR");
                return 1;
            }

            final File irFile = new File(llFilePath);
            Files.writeString(irFile.toPath(), llvmIR);
            System.out.println("Raw IR built successfully: " + llFilePath);

            // opt passes
            boolean optSuccess = runOptPasses(llFilePath, optFilePath);
            String finalIrFile = optSuccess ? optFilePath : llFilePath;

            // compilation
            compileToBinary(new File(finalIrFile).getAbsolutePath(), binaryName);

            return 0;

        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * Run LLVM optimization passes using checks
     * @param inputLl LLVM IR input
     * @param outputLl LLVM IR output
     * @return true if the optimization passes were successful, false otherwise
     */
    private static boolean runOptPasses(String inputLl, String outputLl) {
        System.out.println("Running LLVM Optimization passes...");
        ProcessBuilder processBuilder = new ProcessBuilder(
                "opt",
                "-O3",
                "-S",             // maintains a readable output
                inputLl,
                "-o",
                outputLl
        );

        processBuilder.inheritIO();

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Optimized IR saved to: " + outputLl);
                return true;
            } else {
                System.err.println("Opt tool failed with exit code " + exitCode + ". Proceeding with raw IR.");
                return false;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to launch 'opt' tool. Is it installed and in your PATH? idiot. Do sudo apt install opt");
            return false;
        }
    }

    /**
     * Wrapper for calling cLang with a try/catch block
     * @param llFilePath file path for the LLVM file
     * @param outputBinaryName name for the final binary file
     */
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

    /**
     * invokes Clang to create the final binary
     * @param llFilePath path for the LLVM file
     * @param outputBinaryName name for the final binary file
     * @return a ProcesssBuilder object
     */
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

/**
 * Main class, nothing to say
 */
public class Main {
    public static void main(String[] args) {
        new CommandLine(new CompilerCmd()).execute(args);
    }
}