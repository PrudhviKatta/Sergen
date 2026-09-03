package com.company.resumeai.common.exception;

/**
 * Business-rule violation that is not a bean-validation failure, e.g. the
 * chronology check in §29 (start_date <= end_date is enforced at the DB level
 * as a backstop, but we reject earlier, at the service layer, for a clean 400).
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
