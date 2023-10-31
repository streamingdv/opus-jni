package com.grill.opuscodec;

public class OpusError extends RuntimeException {

    private final int error;

    private OpusError(String message, int error) {
        super(message);
        this.error = error;
    }

    public int getError() {
        return this.error;
    }

    public static int throwIfError(int error) {
        if (error < 0) {
            throw new OpusError("Error from codec: " + error + ". " + getErrorMessage(error), error);
        }
        return error;
    }

    private static String getErrorMessage(int error) {
        String errorMessage;
        switch (error) {
            case -1:
                errorMessage = "One or more invalid/out of range arguments.";
                break;
            case -2:
                errorMessage = "Not enough bytes allocated in the buffer.";
                break;
            case -3:
                errorMessage = "An internal error was detected.";
                break;
            case -4:
                errorMessage = "The compressed data passed is corrupted.";
                break;
            case -5:
                errorMessage = "Invalid/unsupported request number.";
                break;
            case -6:
                errorMessage = "An encoder or decoder structure is invalid or already freed.";
                break;
            case -7:
                errorMessage = "Memory allocation has failed.";
                break;
            default:
                errorMessage = "Unknown error.";
                break;
        }
        return errorMessage;

    }
}
