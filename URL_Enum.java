public enum URL_Enum{
    SendURL(1),
    GetURL(2),
    EXIT(3);

    private final int value;

    private URL_Enum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}