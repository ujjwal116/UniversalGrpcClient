package com.ugc.exceptions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApplicationException extends RuntimeException {
    public ApplicationException(Throwable t, String errorMessage, String... args) {
        super(t);
        log.error(errorMessage, args);
    }

    public ApplicationException(String errorMessage, String... args) {
        super();
        log.error(errorMessage, args);
    }
}
