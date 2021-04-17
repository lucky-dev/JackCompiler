public class JackTokenizer {

    enum TokenType {
        NONE,
        KEYWORD,
        SYMBOL,
        IDENTIFIER,
        INT_CONST,
        STRING_CONST,
        END_OF_FILE
    }

    enum KeyWord {
        NONE (""),
        CLASS ("class"),
        METHOD ("method"),
        FUNCTION ("function"),
        CONSTRUCTOR ("constructor"),
        INT ("int"),
        BOOLEAN ("boolean"),
        CHAR ("char"),
        VOID ("void"),
        VAR ("var"),
        STATIC ("static"),
        FIELD ("field"),
        LET ("let"),
        DO ("do"),
        IF ("if"),
        ELSE ("else"),
        WHILE ("while"),
        RETURN ("return"),
        TRUE ("true"),
        FALSE ("false"),
        NULL ("null"),
        THIS ("this");

        private final String name;

        KeyWord(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private String  mInputData;
    private TokenType mTokenType = TokenType.NONE;
    private KeyWord mKeyWord = KeyWord.NONE;
    private char mSymbol = '\0';
    private String mIdentifier = null;
    private int mIntVal = 0;
    private String mStringVal = null;
    private char mCurrentChar = '\0';
    private int mCurrentState = 0;
    private int mCurrentPosition = 0;
    private StringBuffer mBuffer = new StringBuffer();
    private int mNumberLine = 1;

    public JackTokenizer(String inputData) {
        mInputData = inputData;
    }

    public boolean hasMoreTokens() {
        return mTokenType != TokenType.END_OF_FILE;
    }

    public void advance() {
        mCurrentState = 0;
        mTokenType = TokenType.NONE;
        mKeyWord = KeyWord.NONE;
        mSymbol = '\0';
        mIdentifier = null;
        mIntVal = 0;
        mStringVal = null;

        int mOldCurrentPosition = mCurrentPosition;

        while (true) {
            if (mCurrentPosition < mInputData.length()) {
                mCurrentChar = mInputData.charAt(mCurrentPosition);
            } else {
                mCurrentChar = '\0';
            }

            if ((mCurrentChar == '\n') && (mOldCurrentPosition != mCurrentPosition)) {
                mOldCurrentPosition = mCurrentPosition;
                mNumberLine++;
            }

            switch (mCurrentState) {
                case 0 : {
                    mBuffer = new StringBuffer();

                    if (((mCurrentChar >= 'A') && (mCurrentChar <= 'Z')) ||
                            ((mCurrentChar >= 'a') && (mCurrentChar <= 'z')) ||
                            (mCurrentChar == '_')) {
                        mCurrentState = 1;
                        mBuffer.append(mCurrentChar);
                        mCurrentPosition++;
                    } else if ((mCurrentChar == '{') ||
                            (mCurrentChar == '}') ||
                            (mCurrentChar == '(') ||
                            (mCurrentChar == ')') ||
                            (mCurrentChar == '[') ||
                            (mCurrentChar == ']') ||
                            (mCurrentChar == '.') ||
                            (mCurrentChar == ',') ||
                            (mCurrentChar == ';') ||
                            (mCurrentChar == '+') ||
                            (mCurrentChar == '-') ||
                            (mCurrentChar == '*') ||
                            (mCurrentChar == '&') ||
                            (mCurrentChar == '|') ||
                            (mCurrentChar == '<') ||
                            (mCurrentChar == '>') ||
                            (mCurrentChar == '=') ||
                            (mCurrentChar == '~')) {
                        mCurrentState = 2;
                        mBuffer.append(mCurrentChar);
                        mCurrentPosition++;
                    } else if ((mCurrentChar >= '0') && (mCurrentChar <= '9')) {
                        mCurrentState = 3;
                        mBuffer.append(mCurrentChar);
                        mCurrentPosition++;
                    } else if (mCurrentChar == '"') {
                        mCurrentState = 4;
                        mCurrentPosition++;
                    } else if (mCurrentChar == '/') {
                        mCurrentState = 6;
                        mBuffer.append(mCurrentChar);
                        mCurrentPosition++;
                    } else if ((mCurrentChar == ' ') || (mCurrentChar == '\n') || (mCurrentChar == '\t') || (mCurrentChar == '\r')) {
                        mCurrentState = 8;
                        mCurrentPosition++;
                    } else if (mCurrentChar == '\0') {
                        mTokenType = TokenType.END_OF_FILE;
                        return;
                    } else {
                        return;
                    }
                } break;

                case 1 : {
                    if (((mCurrentChar >= 'A') && (mCurrentChar <= 'Z')) ||
                            ((mCurrentChar >= 'a') && (mCurrentChar <= 'z')) ||
                            ((mCurrentChar >= '0') && (mCurrentChar <= '9')) ||
                            (mCurrentChar == '_')) {
                        mCurrentState = 1;
                        mBuffer.append(mCurrentChar);
                        mCurrentPosition++;
                    } else {
                        String str = mBuffer.toString();

                        if (str.equalsIgnoreCase("class")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.CLASS;
                        } else if (str.equalsIgnoreCase("constructor")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.CONSTRUCTOR;
                        } else if (str.equalsIgnoreCase("function")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.FUNCTION;
                        } else if (str.equalsIgnoreCase("method")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.METHOD;
                        } else if (str.equalsIgnoreCase("field")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.FIELD;
                        } else if (str.equalsIgnoreCase("static")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.STATIC;
                        } else if (str.equalsIgnoreCase("var")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.VAR;
                        } else if (str.equalsIgnoreCase("int")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.INT;
                        } else if (str.equalsIgnoreCase("char")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.CHAR;
                        } else if (str.equalsIgnoreCase("boolean")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.BOOLEAN;
                        } else if (str.equalsIgnoreCase("void")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.VOID;
                        } else if (str.equalsIgnoreCase("true")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.TRUE;
                        } else if (str.equalsIgnoreCase("false")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.FALSE;
                        } else if (str.equalsIgnoreCase("null")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.NULL;
                        } else if (str.equalsIgnoreCase("this")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.THIS;
                        } else if (str.equalsIgnoreCase("let")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.LET;
                        } else if (str.equalsIgnoreCase("do")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.DO;
                        } else if (str.equalsIgnoreCase("if")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.IF;
                        } else if (str.equalsIgnoreCase("else")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.ELSE;
                        } else if (str.equalsIgnoreCase("while")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.WHILE;
                        } else if (str.equalsIgnoreCase("return")) {
                            mTokenType = TokenType.KEYWORD;
                            mKeyWord = KeyWord.RETURN;
                        } else {
                            mIdentifier = str;
                            mTokenType = TokenType.IDENTIFIER;
                        }

                        return;
                    }
                } break;

                case 2 : {
                    mTokenType = TokenType.SYMBOL;
                    mSymbol = mBuffer.charAt(0);

                    return;
                }

                case 3 : {
                    if ((mCurrentChar >= '0') && (mCurrentChar <= '9')) {
                        mCurrentState = 3;
                        mBuffer.append(mCurrentChar);
                        mCurrentPosition++;
                    } else {
                        mIntVal = Integer.parseInt(mBuffer.toString());
                        mTokenType = TokenType.INT_CONST;

                        return;
                    }
                } break;

                case 4 : {
                    if (mCurrentChar != '"') {
                        mCurrentState = 4;
                        mBuffer.append(mCurrentChar);
                    } else {
                        mCurrentState = 5;
                    }

                    mCurrentPosition++;
                } break;

                case 5 : {
                    mStringVal = mBuffer.toString();
                    mTokenType = TokenType.STRING_CONST;

                    return;
                }

                case 6 : {
                    if (mCurrentChar == '/') {
                        mCurrentState = 7;
                        mCurrentPosition++;
                    } else if (mCurrentChar == '*') {
                        mCurrentState = 9;
                        mCurrentPosition++;
                    } else {
                        mTokenType = TokenType.SYMBOL;
                        mSymbol = mBuffer.charAt(0);

                        return;
                    }
                } break;

                case 7 : {
                    if ((mCurrentChar == '\n') || (mCurrentChar == '\0')) {
                        mCurrentState = 0;
                    } else {
                        mCurrentState = 7;
                    }

                    mCurrentPosition++;
                } break;

                case 8 : {
                    if ((mCurrentChar == ' ') || (mCurrentChar == '\n') || (mCurrentChar == '\t') || (mCurrentChar == '\r')) {
                        mCurrentState = 8;
                        mCurrentPosition++;
                    } else {
                        mCurrentState = 0;
                    }
                } break;

                case 9 : {
                    if (mCurrentChar == '*') {
                        mCurrentState = 10;
                    } else {
                        mCurrentState = 9;
                    }

                    mCurrentPosition++;
                } break;

                case 10 : {
                    if (mCurrentChar == '*') {
                        mCurrentState = 10;
                    } else if (mCurrentChar == '/') {
                        mCurrentState = 0;
                    } else {
                        mCurrentState = 9;
                    }

                    mCurrentPosition++;
                } break;
            }
        }
    }

    public TokenType tokenType() {
        return mTokenType;
    }

    public KeyWord keyWord() {
        return mKeyWord;
    }

    public char symbol() {
        return mSymbol;
    }

    public String identifier() {
        return mIdentifier;
    }

    public int intVal() {
        return mIntVal;
    }

    public String stringVal() {
        return mStringVal;
    }

    public int getNumberLine() {
        return mNumberLine;
    }

}
