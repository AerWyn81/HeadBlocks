package fr.aerwyn81.headblocks.utils;

public class InternalException extends Exception {
    public InternalException() {
    }

    public InternalException(String message) {
        super(message);
    }

    public InternalException(Throwable cause) {
        super(cause);
    }
}
