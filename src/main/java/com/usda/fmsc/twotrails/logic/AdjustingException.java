package com.usda.fmsc.twotrails.logic;

public class AdjustingException extends Exception {
    private AdjustingError _ErrorType;

    public AdjustingException(AdjustingError errorType, Throwable ex) {
        super(ex);
        _ErrorType = errorType;

    }

    public AdjustingError getErrorType() {
        return _ErrorType;
    }

    public enum AdjustingError {
        None,
        Unknown,
        Traverse,
        Sideshot,
        Gps,
        Quondam
    }
}

