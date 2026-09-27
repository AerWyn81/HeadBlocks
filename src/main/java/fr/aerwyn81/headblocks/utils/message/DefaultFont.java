package fr.aerwyn81.headblocks.utils.message;

public enum DefaultFont {
    A('A', 5),
    LOWER_A('a', 5),
    B('B', 5),
    LOWER_B('b', 5),
    C('C', 5),
    LOWER_C('c', 5),
    D('D', 5),
    LOWER_D('d', 5),
    E('E', 5),
    LOWER_E('e', 5),
    F('F', 5),
    LOWER_F('f', 4),
    G('G', 5),
    LOWER_G('g', 5),
    H('H', 5),
    LOWER_H('h', 5),
    I('I', 3),
    LOWER_I('i', 1),
    J('J', 5),
    LOWER_J('j', 5),
    K('K', 5),
    LOWER_K('k', 4),
    L('L', 5),
    LOWER_L('l', 1),
    M('M', 5),
    LOWER_M('m', 5),
    N('N', 5),
    LOWER_N('n', 5),
    O('O', 5),
    LOWER_O('o', 5),
    P('P', 5),
    LOWER_P('p', 5),
    Q('Q', 5),
    LOWER_Q('q', 5),
    R('R', 5),
    LOWER_R('r', 5),
    S('S', 5),
    LOWER_S('s', 5),
    T('T', 5),
    LOWER_T('t', 4),
    U('U', 5),
    LOWER_U('u', 5),
    V('V', 5),
    LOWER_V('v', 5),
    W('W', 5),
    LOWER_W('w', 5),
    X('X', 5),
    LOWER_X('x', 5),
    Y('Y', 5),
    LOWER_Y('y', 5),
    Z('Z', 5),
    LOWER_Z('z', 5),
    NUM_1('1', 5),
    NUM_2('2', 5),
    NUM_3('3', 5),
    NUM_4('4', 5),
    NUM_5('5', 5),
    NUM_6('6', 5),
    NUM_7('7', 5),
    NUM_8('8', 5),
    NUM_9('9', 5),
    NUM_0('0', 5),
    EXCLAMATION_POINT('!', 1),
    AT_SYMBOL('@', 6),
    NUM_SIGN('#', 5),
    DOLLAR_SIGN('$', 5),
    PERCENT('%', 5),
    UP_ARROW('^', 5),
    AMPERSAND('&', 5),
    ASTERISK('*', 5),
    LEFT_PARENTHESIS('(', 4),
    RIGHT_PERENTHESIS(')', 4),
    MINUS('-', 5),
    UNDERSCORE('_', 5),
    PLUS_SIGN('+', 5),
    EQUALS_SIGN('=', 5),
    LEFT_CURL_BRACE('{', 4),
    RIGHT_CURL_BRACE('}', 4),
    LEFT_BRACKET('[', 3),
    RIGHT_BRACKET(']', 3),
    COLON(':', 1),
    SEMI_COLON(';', 1),
    DOUBLE_QUOTE('"', 3),
    SINGLE_QUOTE('\'', 1),
    LEFT_ARROW('<', 4),
    RIGHT_ARROW('>', 4),
    QUESTION_MARK('?', 5),
    SLASH('/', 5),
    BACK_SLASH('\\', 5),
    LINE('|', 1),
    TILDE('~', 5),
    TICK('`', 2),
    PERIOD('.', 1),
    COMMA(',', 1),
    SPACE(' ', 3),
    DEFAULT('a', 4);

    private final char character;
    private final int length;

    DefaultFont(char character, int length) {
        this.character = character;
        this.length = length;
    }

    public char getCharacter() {
        return this.character;
    }

    public int getLength() {
        return this.length;
    }

    public int getBoldLength() {
        if (this == SPACE)
            return getLength();
        return this.length + 1;
    }

    public static DefaultFont getDefaultFontInfo(char c) {
        for (DefaultFont dFI : values()) {
            if (dFI.getCharacter() == c)
                return dFI;
        }
        return DEFAULT;
    }
}
