import java.io.*;

public class VMWriter {

    public enum Segment {
        CONST("constant"),
        ARG("argument"),
        LOCAL("local"),
        STATIC("static"),
        THIS("this"),
        THAT("that"),
        POINTER("pointer"),
        TEMP("temp");

        private final String name;

        Segment(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public enum Operator {
        ADD("add"),
        SUB("sub"),
        NEG("neg"),
        EQ("eq"),
        GT("gt"),
        LT("lt"),
        AND("and"),
        OR("or"),
        NOT("not");

        private final String name;

        Operator(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private File mOutputFile;
    private StringBuilder mOutputData;

    public VMWriter(File outputFile) {
        mOutputFile = outputFile;
        mOutputData = new StringBuilder();
    }

    public void writePush(Segment segment, int index) {
        mOutputData.append("push ");
        mOutputData.append(segment);
        mOutputData.append(" ");
        mOutputData.append(index);
        mOutputData.append("\n");
    }

    public void writePop(Segment segment, int index) {
        mOutputData.append("pop ");
        mOutputData.append(segment);
        mOutputData.append(" ");
        mOutputData.append(index);
        mOutputData.append("\n");
    }

    public void writeArithmetic(Operator operator) {
        mOutputData.append(operator);
        mOutputData.append("\n");
    }

    public void writeLabel(String label) {
        mOutputData.append("label ");
        mOutputData.append(label);
        mOutputData.append("\n");
    }

    public void writeGoto(String label) {
        mOutputData.append("goto ");
        mOutputData.append(label);
        mOutputData.append("\n");
    }

    public void writeIf(String label) {
        mOutputData.append("if-goto ");
        mOutputData.append(label);
        mOutputData.append("\n");
    }

    public void writeCall(String name, int nArgs) {
        mOutputData.append("call ");
        mOutputData.append(name);
        mOutputData.append(" ");
        mOutputData.append(nArgs);
        mOutputData.append("\n");
    }

    public void writeFunction(String name, int nLocals) {
        mOutputData.append("function ");
        mOutputData.append(name);
        mOutputData.append(" ");
        mOutputData.append(nLocals);
        mOutputData.append("\n");
    }

    public void writeReturn() {
        mOutputData.append("return\n");
    }

    public void close() {
        try {
            FileOutputStream fos = new FileOutputStream(mOutputFile);

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(mOutputData.toString());
            bw.close();

            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
