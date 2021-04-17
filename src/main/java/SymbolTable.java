import java.util.HashMap;

public class SymbolTable {

    public enum KindVar {
        NONE, STATIC, FIELD, ARG, LOCAL
    }

    private class Symbol {
        private String mName;
        private String mType;
        private KindVar mKind;
        private int mIndex;

        public Symbol(String name, String type, KindVar kind, int index) {
            mName = name;
            mType = type;
            mKind = kind;
            mIndex = index;
        }

        public String getName() {
            return mName;
        }

        public String getType() {
            return mType;
        }

        public KindVar getKind() {
            return mKind;
        }

        public int getIndex() {
            return mIndex;
        }
    }

    private HashMap<String, Symbol> mSymbolsTable = new HashMap<>();
    private int mCountStaticVars = 0;
    private int mCountFieldVars = 0;
    private int mCountArgVars = 0;
    private int mCountLocalVars = 0;

    public void startSubroutine() {
        mSymbolsTable.clear();

        mCountStaticVars = 0;
        mCountFieldVars = 0;
        mCountArgVars = 0;
        mCountLocalVars = 0;
    }

    public void define(String name, String type, KindVar kindVar) {
        switch (kindVar) {
            case STATIC: {
                mSymbolsTable.put(name, new Symbol(name, type, kindVar, mCountStaticVars++));
            } break;

            case FIELD: {
                mSymbolsTable.put(name, new Symbol(name, type, kindVar, mCountFieldVars++));
            } break;

            case ARG: {
                mSymbolsTable.put(name, new Symbol(name, type, kindVar, mCountArgVars++));
            } break;

            default: {
                mSymbolsTable.put(name, new Symbol(name, type, kindVar, mCountLocalVars++));
            } break;
        }
    }

    public int varCount(KindVar kindVar) {
        switch (kindVar) {
            case STATIC: {
                return mCountStaticVars;
            }

            case FIELD: {
                return mCountFieldVars;
            }

            case ARG: {
                return mCountArgVars;
            }

            default: {
                return mCountLocalVars;
            }
        }
    }

    public KindVar kindOf(String name) {
        if (mSymbolsTable.containsKey(name)) {
            return mSymbolsTable.get(name).getKind();
        }

        return KindVar.NONE;
    }

    public String typeOf(String name) {
        return mSymbolsTable.get(name).getType();
    }

    public int indexOf(String name) {
        return mSymbolsTable.get(name).getIndex();
    }

}
