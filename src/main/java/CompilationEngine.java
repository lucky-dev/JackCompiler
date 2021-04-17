import java.io.*;
import java.util.Stack;

public class CompilationEngine {

    private enum TypeSubroutine {
        NONE,
        METHOD,
        FUNCTION,
        CONSTRUCTOR
    }

    private JackTokenizer mJackTokenizer;
    private VMWriter mVMWriter;
    private File mInputFile;
    private File mOutputFile;
    private SymbolTable mClassSymbolTable;
    private SymbolTable mSubroutineSymbolTable;
    private String mNameCurrentClass;
    private String mNameCurrentSubroutine;
    private Stack<Character> mStackOperators;
    private TypeSubroutine mTypeCurrentSubroutine = TypeSubroutine.NONE;
    private boolean isCurrentSubroutineReturnVoid = false;
    private int mCountLabelsIf = 0;
    private int mCountLabelsWhile = 0;
    private int mCountArgs = 0;

    public CompilationEngine(File inputFile, File outputFile) {
        mInputFile = inputFile;
        mOutputFile = outputFile;
    }

    public void compileClass() throws CompilationEngineException {
        mJackTokenizer = new JackTokenizer(readFile(mInputFile));
        mVMWriter = new VMWriter(mOutputFile);
        mClassSymbolTable = new SymbolTable();
        mSubroutineSymbolTable = new SymbolTable();

        nextToken();

        if (!matchKeyword(JackTokenizer.KeyWord.CLASS)) {
            throw new CompilationEngineException(String.format("Expect the token CLASS in the line %d", mJackTokenizer.getNumberLine()));
        }

        mNameCurrentClass = mJackTokenizer.identifier();

        if (!matchTokenType(JackTokenizer.TokenType.IDENTIFIER)) {
            throw new CompilationEngineException(String.format("Expect an identifier in the line %d", mJackTokenizer.getNumberLine()));
        }

        if (!matchSymbol('{')) {
            throw new CompilationEngineException(String.format("Expect the symbol { in the line %d", mJackTokenizer.getNumberLine()));
        }

        compileClassVarDec();

        compileSubroutine();

        if (!matchSymbol('}')) {
            throw new CompilationEngineException(String.format("Expect the symbol } in the line %d", mJackTokenizer.getNumberLine()));
        }

        mVMWriter.close();
    }

    public void compileClassVarDec() throws CompilationEngineException {
        while ((mJackTokenizer.keyWord() == JackTokenizer.KeyWord.STATIC) ||
                (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.FIELD)) {

            SymbolTable.KindVar kindVar = SymbolTable.KindVar.FIELD;

            if (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.STATIC) {
                matchKeyword(JackTokenizer.KeyWord.STATIC);
                kindVar = SymbolTable.KindVar.STATIC;
            } else {
                matchKeyword(JackTokenizer.KeyWord.FIELD);
            }

            String varType = ((mJackTokenizer.keyWord() == JackTokenizer.KeyWord.INT) ||
                    (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.CHAR) ||
                    (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.BOOLEAN)) ? mJackTokenizer.keyWord().toString() : mJackTokenizer.identifier();

            if ((!matchKeyword(JackTokenizer.KeyWord.INT)) &&
                    (!matchKeyword(JackTokenizer.KeyWord.CHAR)) &&
                    (!matchKeyword(JackTokenizer.KeyWord.BOOLEAN)) &&
                    (!matchTokenType(JackTokenizer.TokenType.IDENTIFIER))) {
                throw new CompilationEngineException(String.format("Expect the token INT, CHAR, BOOLEAN or an identifier in the line %d", mJackTokenizer.getNumberLine()));
            }

            while (true) {
                String varName = mJackTokenizer.identifier();

                if (!matchTokenType(JackTokenizer.TokenType.IDENTIFIER)) {
                    throw new CompilationEngineException(String.format("Expect an identifier in the line %d", mJackTokenizer.getNumberLine()));
                }

                mClassSymbolTable.define(varName, varType, kindVar);

                if (!matchSymbol(',')) {
                    break;
                }
            }

            if (!matchSymbol(';')) {
                throw new CompilationEngineException(String.format("Expect the symbol ; in the line %d", mJackTokenizer.getNumberLine()));
            }
        }
    }

    public void compileSubroutine() throws CompilationEngineException {
        while ((mJackTokenizer.keyWord() == JackTokenizer.KeyWord.CONSTRUCTOR) ||
                (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.FUNCTION) ||
                (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.METHOD)) {

            mSubroutineSymbolTable.startSubroutine();

            if (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.CONSTRUCTOR) {
                matchKeyword(JackTokenizer.KeyWord.CONSTRUCTOR);
                mTypeCurrentSubroutine = TypeSubroutine.CONSTRUCTOR;
            } else if (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.FUNCTION) {
                matchKeyword(JackTokenizer.KeyWord.FUNCTION);
                mTypeCurrentSubroutine = TypeSubroutine.FUNCTION;
            } else {
                matchKeyword(JackTokenizer.KeyWord.METHOD);
                mTypeCurrentSubroutine = TypeSubroutine.METHOD;
            }

            isCurrentSubroutineReturnVoid = mJackTokenizer.keyWord() == JackTokenizer.KeyWord.VOID;

            if ((!matchKeyword(JackTokenizer.KeyWord.VOID)) &&
                    (!matchKeyword(JackTokenizer.KeyWord.INT)) &&
                    (!matchKeyword(JackTokenizer.KeyWord.BOOLEAN)) &&
                    (!matchKeyword(JackTokenizer.KeyWord.CHAR)) &&
                    (!matchTokenType(JackTokenizer.TokenType.IDENTIFIER))) {
                throw new CompilationEngineException(String.format("Expect the token VOID, int, boolean, char or identifier in the line %d", mJackTokenizer.getNumberLine()));
            }

            mNameCurrentSubroutine = mJackTokenizer.identifier();

            if (!matchTokenType(JackTokenizer.TokenType.IDENTIFIER)) {
                throw new CompilationEngineException(String.format("Expect an identifier in the line %d", mJackTokenizer.getNumberLine()));
            }

            if (!matchSymbol('(')) {
                throw new CompilationEngineException(String.format("Expect the symbol ( in the line %d", mJackTokenizer.getNumberLine()));
            }

            compileParameterList();

            if (!matchSymbol(')')) {
                throw new CompilationEngineException(String.format("Expect the symbol ) in the line %d", mJackTokenizer.getNumberLine()));
            }

            if (!matchSymbol('{')) {
                throw new CompilationEngineException(String.format("Expect the symbol { in the line %d", mJackTokenizer.getNumberLine()));
            }

            compileVarDec();

            mVMWriter.writeFunction(String.format("%s.%s", mNameCurrentClass, mNameCurrentSubroutine), mSubroutineSymbolTable.varCount(SymbolTable.KindVar.LOCAL));

            if (mTypeCurrentSubroutine == TypeSubroutine.CONSTRUCTOR) {
                mVMWriter.writePush(VMWriter.Segment.CONST, mClassSymbolTable.varCount(SymbolTable.KindVar.FIELD));
                mVMWriter.writeCall("Memory.alloc", 1);
                mVMWriter.writePop(VMWriter.Segment.POINTER, 0);
            }

            if (mTypeCurrentSubroutine == TypeSubroutine.METHOD) {
                mVMWriter.writePush(VMWriter.Segment.ARG, 0);
                mVMWriter.writePop(VMWriter.Segment.POINTER, 0);
            }

            compileStatements();

            if (!matchSymbol('}')) {
                throw new CompilationEngineException(String.format("Expect the symbol } in the line %d", mJackTokenizer.getNumberLine()));
            }
        }
    }

    public void compileParameterList() throws CompilationEngineException {
        if (mTypeCurrentSubroutine == TypeSubroutine.METHOD) {
            mSubroutineSymbolTable.define("this", mNameCurrentClass, SymbolTable.KindVar.ARG);
        }

        while (true) {
            String varType = ((mJackTokenizer.keyWord() == JackTokenizer.KeyWord.INT) ||
                    (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.CHAR) ||
                    (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.BOOLEAN)) ? mJackTokenizer.keyWord().toString() : mJackTokenizer.identifier();

            if ((!matchKeyword(JackTokenizer.KeyWord.INT)) &&
                    (!matchKeyword(JackTokenizer.KeyWord.CHAR)) &&
                    (!matchKeyword(JackTokenizer.KeyWord.BOOLEAN)) &&
                    (!matchTokenType(JackTokenizer.TokenType.IDENTIFIER))) {
                break;
            }

            String varName = mJackTokenizer.identifier();

            if (!matchTokenType(JackTokenizer.TokenType.IDENTIFIER)) {
                throw new CompilationEngineException(String.format("Expect the token INT, CHAR, BOOLEAN or an identifier in the line %d", mJackTokenizer.getNumberLine()));
            }

            mSubroutineSymbolTable.define(varName, varType, SymbolTable.KindVar.ARG);

            if (!matchSymbol(',')) {
                break;
            }
        }
    }

    public void compileVarDec() throws CompilationEngineException {
        while (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.VAR) {
            matchKeyword(JackTokenizer.KeyWord.VAR);

            String varType = ((mJackTokenizer.keyWord() == JackTokenizer.KeyWord.INT) ||
                    (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.CHAR) ||
                    (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.BOOLEAN)) ? mJackTokenizer.keyWord().toString() : mJackTokenizer.identifier();

            if ((!matchKeyword(JackTokenizer.KeyWord.INT)) &&
                    (!matchKeyword(JackTokenizer.KeyWord.CHAR)) &&
                    (!matchKeyword(JackTokenizer.KeyWord.BOOLEAN)) &&
                    (!matchTokenType(JackTokenizer.TokenType.IDENTIFIER))) {
                throw new CompilationEngineException(String.format("Expect the token INT, CHAR, BOOLEAN or an identifier in the line %d", mJackTokenizer.getNumberLine()));
            }

            while (true) {
                String varName = mJackTokenizer.identifier();

                if (!matchTokenType(JackTokenizer.TokenType.IDENTIFIER)) {
                    throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
                }

                mSubroutineSymbolTable.define(varName, varType, SymbolTable.KindVar.LOCAL);

                if (!matchSymbol(',')) {
                    break;
                }
            }

            if (!matchSymbol(';')) {
                throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
            }
        }
    }

    public void compileStatements() throws CompilationEngineException {
        if ((mJackTokenizer.keyWord() != JackTokenizer.KeyWord.IF) &&
                (mJackTokenizer.keyWord() != JackTokenizer.KeyWord.LET) &&
                (mJackTokenizer.keyWord() != JackTokenizer.KeyWord.WHILE) &&
                (mJackTokenizer.keyWord() != JackTokenizer.KeyWord.DO) &&
                (mJackTokenizer.keyWord() != JackTokenizer.KeyWord.RETURN)) {
            return;
        }

        while (true) {
            switch (mJackTokenizer.keyWord()) {
                case IF: {
                    compileIf();
                } break;

                case LET: {
                    compileLet();
                } break;

                case WHILE: {
                    compileWhile();
                } break;

                case DO: {
                    compileDo();
                } break;

                case RETURN: {
                    compileReturn();
                } break;

                default:
                    return;
            }
        }
    }

    public void compileDo() throws CompilationEngineException {
        matchKeyword(JackTokenizer.KeyWord.DO);

        String identifier = mJackTokenizer.identifier();

        if (!matchTokenType(JackTokenizer.TokenType.IDENTIFIER)) {
            throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
        }

        if (matchSymbol('(')) {
            if (mTypeCurrentSubroutine == TypeSubroutine.FUNCTION) {
                throw new CompilationEngineException(String.format("Function must call other subroutines via object or class in the line %d", mJackTokenizer.getNumberLine()));
            }

            mCountArgs = 1;

            mVMWriter.writePush(VMWriter.Segment.POINTER, 0);

            if (mJackTokenizer.symbol() != ')') {
                mCountArgs++;
            }

            compileExpressionList();

            if (!matchSymbol(')')) {
                throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
            }

            mVMWriter.writeCall(String.format("%s.%s", mNameCurrentClass, identifier), mCountArgs);
        } else if (matchSymbol('.')) {
            String nameMethod = mJackTokenizer.identifier();

            if (!matchTokenType(JackTokenizer.TokenType.IDENTIFIER)) {
                throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
            }

            if (!matchSymbol('(')) {
                throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
            }

            mCountArgs = 0;

            if (mSubroutineSymbolTable.kindOf(identifier) != SymbolTable.KindVar.NONE) {
                mVMWriter.writePush(getSegment(mSubroutineSymbolTable.kindOf(identifier)), mSubroutineSymbolTable.indexOf(identifier));
                mCountArgs++;
            } else if (mClassSymbolTable.kindOf(identifier) != SymbolTable.KindVar.NONE) {
                mVMWriter.writePush(getSegment(mClassSymbolTable.kindOf(identifier)), mClassSymbolTable.indexOf(identifier));
                mCountArgs++;
            }

            if (mJackTokenizer.symbol() != ')') {
                mCountArgs++;
            }

            compileExpressionList();

            if (!matchSymbol(')')) {
                throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
            }

            if (mSubroutineSymbolTable.kindOf(identifier) != SymbolTable.KindVar.NONE) {
                mVMWriter.writeCall(String.format("%s.%s", mSubroutineSymbolTable.typeOf(identifier), nameMethod), mCountArgs);
            } else if (mClassSymbolTable.kindOf(identifier) != SymbolTable.KindVar.NONE) {
                mVMWriter.writeCall(String.format("%s.%s", mClassSymbolTable.typeOf(identifier), nameMethod), mCountArgs);
            } else {
                mVMWriter.writeCall(String.format("%s.%s", identifier, nameMethod), mCountArgs);
            }
        }

        mVMWriter.writePop(VMWriter.Segment.TEMP, 0);

        if (!matchSymbol(';')) {
            throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
        }
    }

    public void compileLet() throws CompilationEngineException {
        matchKeyword(JackTokenizer.KeyWord.LET);

        String identifier = mJackTokenizer.identifier();

        boolean isArray = false;

        if (!matchTokenType(JackTokenizer.TokenType.IDENTIFIER)) {
            throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
        }

        if ((mTypeCurrentSubroutine == TypeSubroutine.FUNCTION) &&
                (mClassSymbolTable.kindOf(identifier) == SymbolTable.KindVar.FIELD)) {
            throw new CompilationEngineException(String.format("An object field %s in the static context in the line %d", identifier, mJackTokenizer.getNumberLine()));
        }

        if (matchSymbol('[')) {
            if (matchSymbol(']')) {
                throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
            }

            isArray = true;

            if (mSubroutineSymbolTable.kindOf(identifier) != SymbolTable.KindVar.NONE) {
                mVMWriter.writePush(getSegment(mSubroutineSymbolTable.kindOf(identifier)), mSubroutineSymbolTable.indexOf(identifier));
            } else if (mClassSymbolTable.kindOf(identifier) != SymbolTable.KindVar.NONE) {
                mVMWriter.writePush(getSegment(mClassSymbolTable.kindOf(identifier)), mClassSymbolTable.indexOf(identifier));
            }

            Stack<Character> oldStackOperators = mStackOperators;
            mStackOperators = new Stack<>();

            compileExpression();

            while (!mStackOperators.isEmpty()) {
                writeOperator(mStackOperators.pop());
            }

            mStackOperators = oldStackOperators;

            if (!matchSymbol(']')) {
                throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
            }

            mVMWriter.writeArithmetic(VMWriter.Operator.ADD);
        }

        if (!matchSymbol('=')) {
            throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
        }

        if (matchSymbol(';')) {
            throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
        }

        Stack<Character> oldStackOperators = mStackOperators;
        mStackOperators = new Stack<>();

        compileExpression();

        while (!mStackOperators.isEmpty()) {
            writeOperator(mStackOperators.pop());
        }

        mStackOperators = oldStackOperators;

        if (!matchSymbol(';')) {
            throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
        }

        if (isArray) {
            mVMWriter.writePop(VMWriter.Segment.TEMP, 0);
            mVMWriter.writePop(VMWriter.Segment.POINTER, 1);
            mVMWriter.writePush(VMWriter.Segment.TEMP, 0);
            mVMWriter.writePop(VMWriter.Segment.THAT, 0);
        } else {
            if (mSubroutineSymbolTable.kindOf(identifier) != SymbolTable.KindVar.NONE) {
                mVMWriter.writePop(getSegment(mSubroutineSymbolTable.kindOf(identifier)), mSubroutineSymbolTable.indexOf(identifier));
            } else if (mClassSymbolTable.kindOf(identifier) != SymbolTable.KindVar.NONE) {
                mVMWriter.writePop(getSegment(mClassSymbolTable.kindOf(identifier)), mClassSymbolTable.indexOf(identifier));
            }
        }
    }

    public void compileWhile() throws CompilationEngineException {
        matchKeyword(JackTokenizer.KeyWord.WHILE);

        int countLabelsWhile = mCountLabelsWhile;
        mCountLabelsWhile++;

        mVMWriter.writeLabel(String.format("BEGIN_WHILE_%d", countLabelsWhile));

        if (!matchSymbol('(')) {
            throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
        }

        Stack<Character> oldStackOperators = mStackOperators;
        mStackOperators = new Stack<>();

        compileExpression();

        while (!mStackOperators.isEmpty()) {
            writeOperator(mStackOperators.pop());
        }

        mStackOperators = oldStackOperators;

        if (!matchSymbol(')')) {
            throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
        }

        mVMWriter.writeIf(String.format("WHILE_TRUE_%d", countLabelsWhile));
        mVMWriter.writeGoto(String.format("WHILE_FALSE_%d", countLabelsWhile));
        mVMWriter.writeLabel(String.format("WHILE_TRUE_%d", countLabelsWhile));


        if (!matchSymbol('{')) {
            throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
        }

        compileStatements();

        if (!matchSymbol('}')) {
            throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
        }

        mVMWriter.writeGoto(String.format("BEGIN_WHILE_%d", countLabelsWhile));

        mVMWriter.writeLabel(String.format("WHILE_FALSE_%d", countLabelsWhile));
    }

    public void compileReturn() throws CompilationEngineException {
        matchKeyword(JackTokenizer.KeyWord.RETURN);

        Stack<Character> oldStackOperators = mStackOperators;
        mStackOperators = new Stack<>();

        compileExpression();

        while (!mStackOperators.isEmpty()) {
            writeOperator(mStackOperators.pop());
        }

        mStackOperators = oldStackOperators;

        if (!matchSymbol(';')) {
            throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
        }

        if (isCurrentSubroutineReturnVoid) {
            mVMWriter.writePush(VMWriter.Segment.CONST, 0);
        }

        mVMWriter.writeReturn();
    }

    public void compileIf() throws CompilationEngineException {
        int countLabelsIf = mCountLabelsIf;
        mCountLabelsIf++;

        matchKeyword(JackTokenizer.KeyWord.IF);

        if (!matchSymbol('(')) {
            throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
        }

        if (matchSymbol(')')) {
            throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
        }

        Stack<Character> oldStackOperators = mStackOperators;
        mStackOperators = new Stack<>();

        compileExpression();

        while (!mStackOperators.isEmpty()) {
            writeOperator(mStackOperators.pop());
        }

        mStackOperators = oldStackOperators;

        if (!matchSymbol(')')) {
            throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
        }

        mVMWriter.writeIf(String.format("IF_TRUE_%d", countLabelsIf));
        mVMWriter.writeGoto(String.format("IF_FALSE_%d", countLabelsIf));
        mVMWriter.writeLabel(String.format("IF_TRUE_%d", countLabelsIf));

        if (!matchSymbol('{')) {
            throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
        }

        compileStatements();

        if (!matchSymbol('}')) {
            throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
        }

        if (matchKeyword(JackTokenizer.KeyWord.ELSE)) {
            mVMWriter.writeGoto(String.format("END_IF_%d", countLabelsIf));

            mVMWriter.writeLabel(String.format("IF_FALSE_%d", countLabelsIf));

            if (!matchSymbol('{')) {
                throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
            }

            compileStatements();

            if (!matchSymbol('}')) {
                throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
            }

            mVMWriter.writeLabel(String.format("END_IF_%d", countLabelsIf));
        } else {
            mVMWriter.writeLabel(String.format("IF_FALSE_%d", countLabelsIf));
        }
    }

    public void compileExpression() throws CompilationEngineException {
        compileTerm();

        char symbol = mJackTokenizer.symbol();

        while ((matchSymbol('+')) ||
                (matchSymbol('-')) ||
                (matchSymbol('*')) ||
                (matchSymbol('/')) ||
                (matchSymbol('&')) ||
                (matchSymbol('|')) ||
                (matchSymbol('>')) ||
                (matchSymbol('<')) ||
                (matchSymbol('='))) {

            if (!mStackOperators.isEmpty()) {
                while (!mStackOperators.isEmpty()) {
                    char topOperator = mStackOperators.pop();
                    if (topOperator == '(') {
                        mStackOperators.push(topOperator);
                        break;
                    } else {
                        if (getPriorityOperator(topOperator) < getPriorityOperator(symbol)) {
                            mStackOperators.push(topOperator);
                            break;
                        } else {
                            writeOperator(topOperator);
                        }
                    }
                }
            }

            mStackOperators.push(symbol);

            compileTerm();

            symbol = mJackTokenizer.symbol();
        }
    }

    public void compileTerm() throws CompilationEngineException {
        if (mJackTokenizer.tokenType() == JackTokenizer.TokenType.INT_CONST) {
            mVMWriter.writePush(VMWriter.Segment.CONST, mJackTokenizer.intVal());
            matchTokenType(JackTokenizer.TokenType.INT_CONST);
            return;
        }

        if (mJackTokenizer.tokenType() == JackTokenizer.TokenType.STRING_CONST) {
            String str = mJackTokenizer.stringVal();

            mVMWriter.writePush(VMWriter.Segment.CONST, str.length());
            mVMWriter.writeCall("String.new", 1);

            for (int i = 0; i < str.length(); i++) {
                mVMWriter.writePush(VMWriter.Segment.CONST, str.charAt(i));
                mVMWriter.writeCall("String.appendChar", 2);
            }

            matchTokenType(JackTokenizer.TokenType.STRING_CONST);
            return;
        }

        if (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.TRUE) {
            mVMWriter.writePush(VMWriter.Segment.CONST, 0);
            mVMWriter.writeArithmetic(VMWriter.Operator.NOT);
            matchKeyword(JackTokenizer.KeyWord.TRUE);
            return;
        }

        if (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.FALSE) {
            mVMWriter.writePush(VMWriter.Segment.CONST, 0);
            matchKeyword(JackTokenizer.KeyWord.FALSE);
            return;
        }

        if (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.NULL) {
            mVMWriter.writePush(VMWriter.Segment.CONST, 0);
            matchKeyword(JackTokenizer.KeyWord.NULL);
            return;
        }

        if (mJackTokenizer.keyWord() == JackTokenizer.KeyWord.THIS) {
            mVMWriter.writePush(VMWriter.Segment.POINTER, 0);
            matchKeyword(JackTokenizer.KeyWord.THIS);
            return;
        }

        if (mJackTokenizer.tokenType() == JackTokenizer.TokenType.IDENTIFIER) {
            String identifier = mJackTokenizer.identifier();

            matchTokenType(JackTokenizer.TokenType.IDENTIFIER);

            if (matchSymbol('[')) {
                if (matchSymbol(']')) {
                    throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
                }

                if (mSubroutineSymbolTable.kindOf(identifier) != SymbolTable.KindVar.NONE) {
                    mVMWriter.writePush(getSegment(mSubroutineSymbolTable.kindOf(identifier)), mSubroutineSymbolTable.indexOf(identifier));
                } else if (mClassSymbolTable.kindOf(identifier) != SymbolTable.KindVar.NONE) {
                    mVMWriter.writePush(getSegment(mClassSymbolTable.kindOf(identifier)), mClassSymbolTable.indexOf(identifier));
                }

                Stack<Character> oldStackOperators = mStackOperators;
                mStackOperators = new Stack<>();

                compileExpression();

                while (!mStackOperators.isEmpty()) {
                    writeOperator(mStackOperators.pop());
                }

                mStackOperators = oldStackOperators;

                if (!matchSymbol(']')) {
                    throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
                }

                mVMWriter.writeArithmetic(VMWriter.Operator.ADD);

                mVMWriter.writePop(VMWriter.Segment.POINTER, 1);
                mVMWriter.writePush(VMWriter.Segment.THAT, 0);

                return;
            }

            if (matchSymbol('(')) {
                if (mTypeCurrentSubroutine == TypeSubroutine.FUNCTION) {
                    throw new CompilationEngineException(String.format("Function must call other subroutines via object or class in the line %d", mJackTokenizer.getNumberLine()));
                }

                int oldCountArgs = mCountArgs;

                mCountArgs = 1;

                mVMWriter.writePush(VMWriter.Segment.POINTER, 0);

                if (mJackTokenizer.symbol() != ')') {
                    mCountArgs++;
                }

                compileExpressionList();

                if (!matchSymbol(')')) {
                    throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
                }

                mVMWriter.writeCall(String.format("%s.%s", mNameCurrentClass, identifier), mCountArgs);

                mCountArgs = oldCountArgs;

                return;
            }

            if (matchSymbol('.')) {
                String nameMethod = mJackTokenizer.identifier();

                if (!matchTokenType(JackTokenizer.TokenType.IDENTIFIER)) {
                    throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
                }

                if (!matchSymbol('(')) {
                    throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
                }

                int oldCountArgs = mCountArgs;

                mCountArgs = 0;

                if (mSubroutineSymbolTable.kindOf(identifier) != SymbolTable.KindVar.NONE) {
                    mVMWriter.writePush(getSegment(mSubroutineSymbolTable.kindOf(identifier)), mSubroutineSymbolTable.indexOf(identifier));
                    mCountArgs++;
                } else if (mClassSymbolTable.kindOf(identifier) != SymbolTable.KindVar.NONE) {
                    mVMWriter.writePush(getSegment(mClassSymbolTable.kindOf(identifier)), mClassSymbolTable.indexOf(identifier));
                    mCountArgs++;
                }

                if (mJackTokenizer.symbol() != ')') {
                    mCountArgs++;
                }

                compileExpressionList();

                if (!matchSymbol(')')) {
                    throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
                }

                if (mSubroutineSymbolTable.kindOf(identifier) != SymbolTable.KindVar.NONE) {
                    mVMWriter.writeCall(String.format("%s.%s", mSubroutineSymbolTable.typeOf(identifier), nameMethod), mCountArgs);
                } else if (mClassSymbolTable.kindOf(identifier) != SymbolTable.KindVar.NONE) {
                    mVMWriter.writeCall(String.format("%s.%s", mClassSymbolTable.typeOf(identifier), nameMethod), mCountArgs);
                } else {
                    mVMWriter.writeCall(String.format("%s.%s", identifier, nameMethod), mCountArgs);
                }

                mCountArgs = oldCountArgs;

                return;
            }

            if ((mTypeCurrentSubroutine == TypeSubroutine.FUNCTION) &&
                    (mClassSymbolTable.kindOf(identifier) == SymbolTable.KindVar.FIELD)) {
                throw new CompilationEngineException(String.format("An object field %s in the static context in the line %d", identifier, mJackTokenizer.getNumberLine()));
            }

            if (mSubroutineSymbolTable.kindOf(identifier) != SymbolTable.KindVar.NONE) {
                mVMWriter.writePush(getSegment(mSubroutineSymbolTable.kindOf(identifier)), mSubroutineSymbolTable.indexOf(identifier));
            } else if (mClassSymbolTable.kindOf(identifier) != SymbolTable.KindVar.NONE) {
                mVMWriter.writePush(getSegment(mClassSymbolTable.kindOf(identifier)), mClassSymbolTable.indexOf(identifier));
            } else {
                throw new CompilationEngineException(String.format("Unknown identifier %s in the line %d", identifier, mJackTokenizer.getNumberLine()));
            }

            return;
        }

        if (matchSymbol('(')) {
            if (matchSymbol(')')) {
                throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
            }

            mStackOperators.push('(');

            compileExpression();

            if (!matchSymbol(')')) {
                throw new CompilationEngineException(String.format("An error in the line %d", mJackTokenizer.getNumberLine()));
            }

            while (!mStackOperators.isEmpty()) {
                Character operator = mStackOperators.pop();

                if (operator != '(') {
                    writeOperator(operator);
                } else {
                    break;
                }
            }

            return;
        }

        if (matchSymbol('-')) {
            Stack<Character> oldStackOperators = mStackOperators;
            mStackOperators = new Stack<>();

            compileTerm();

            while (!mStackOperators.isEmpty()) {
                writeOperator(mStackOperators.pop());
            }

            mStackOperators = oldStackOperators;

            mVMWriter.writeArithmetic(VMWriter.Operator.NEG);

            return;
        }

        if (matchSymbol('~')) {
            Stack<Character> oldStackOperators = mStackOperators;
            mStackOperators = new Stack<>();

            compileTerm();

            while (!mStackOperators.isEmpty()) {
                writeOperator(mStackOperators.pop());
            }

            mStackOperators = oldStackOperators;

            mVMWriter.writeArithmetic(VMWriter.Operator.NOT);

            return;
        }
    }

    public void compileExpressionList() throws CompilationEngineException {
        Stack<Character> oldStackOperators = mStackOperators;
        mStackOperators = new Stack<>();

        compileExpression();

        while (!mStackOperators.isEmpty()) {
            writeOperator(mStackOperators.pop());
        }

        while (matchSymbol(',')) {
            mCountArgs++;

            mStackOperators = new Stack<>();

            compileExpression();

            while (!mStackOperators.isEmpty()) {
                writeOperator(mStackOperators.pop());
            }
        }

        mStackOperators = oldStackOperators;
    }

    private boolean matchKeyword(JackTokenizer.KeyWord keyWord) throws CompilationEngineException {
        if (keyWord == mJackTokenizer.keyWord()) {
            nextToken();
            return true;
        }

        return false;
    }

    private boolean matchTokenType(JackTokenizer.TokenType tokenType) throws CompilationEngineException {
        if (mJackTokenizer.tokenType() == tokenType) {
            nextToken();
            return true;
        }

        return false;
    }

    private boolean matchSymbol(char symbol) throws CompilationEngineException {
        if (symbol == mJackTokenizer.symbol()) {
            nextToken();
            return true;
        }

        return false;
    }

    private void nextToken() throws CompilationEngineException {
        if (mJackTokenizer.hasMoreTokens()) {
            mJackTokenizer.advance();

            if (mJackTokenizer.tokenType() == JackTokenizer.TokenType.NONE) {
                throw new CompilationEngineException(String.format("An unexpected token in the line %d", mJackTokenizer.getNumberLine()));
            }
        }
    }

    private String readFile(File file) {
        StringBuilder inputData = new StringBuilder();

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);

            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            try {
                String line;
                while ((line = br.readLine()) != null) {
                    inputData.append(line);
                    inputData.append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return inputData.toString();
    }

    private int getPriorityOperator(char operator) {
        if ((operator == '&') ||
                (operator == '|') ||
                (operator == '>') ||
                (operator == '<') ||
                (operator == '=')) {
            return 0;
        }

        if ((operator == '+') || (operator == '-')) {
            return 1;
        }

        if ((operator == '*') || (operator == '/')) {
            return 2;
        }

        return -1;
    }

    private void writeOperator(Character operator) {
        switch (operator) {
            case '&': {
                mVMWriter.writeArithmetic(VMWriter.Operator.AND);
            } break;

            case '|': {
                mVMWriter.writeArithmetic(VMWriter.Operator.OR);
            } break;

            case '>': {
                mVMWriter.writeArithmetic(VMWriter.Operator.GT);
            } break;

            case '<': {
                mVMWriter.writeArithmetic(VMWriter.Operator.LT);
            } break;

            case '=': {
                mVMWriter.writeArithmetic(VMWriter.Operator.EQ);
            } break;

            case '+': {
                mVMWriter.writeArithmetic(VMWriter.Operator.ADD);
            } break;

            case '-': {
                mVMWriter.writeArithmetic(VMWriter.Operator.SUB);
            } break;

            case '*': {
                mVMWriter.writeCall("Math.multiply", 2);
            } break;

            case '/': {
                mVMWriter.writeCall("Math.divide", 2);
            } break;
        }
    }

    private VMWriter.Segment getSegment(SymbolTable.KindVar kindVar) {
        switch (kindVar) {
            case STATIC: {
                return VMWriter.Segment.STATIC;
            }

            case FIELD: {
                return VMWriter.Segment.THIS;
            }

            case ARG: {
                return VMWriter.Segment.ARG;
            }

            default: {
                return VMWriter.Segment.LOCAL;
            }
        }
    }

}
