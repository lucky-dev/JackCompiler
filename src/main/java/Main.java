import java.io.*;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Need name of an JACK file as the first parameter!");
            return;
        }

        File inputFile = new File(args[0]);

        File[] files = inputFile.isFile() ? new File[] { inputFile } : inputFile.listFiles();

        for (File file : files) {
            if (!file.getName().endsWith(".jack")) {
                continue;
            }

            File outputFile = new File(file.getAbsolutePath().replace(".jack", ".vm"));

            CompilationEngine compilationEngine = new CompilationEngine(file, outputFile);

            try {
                compilationEngine.compileClass();
            } catch (CompilationEngineException e) {
                e.printStackTrace();
            }
        }
    }

}
