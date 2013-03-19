/**
 * Mule Development Kit
 * Copyright 2010-2012 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * This software is protected under international copyright law. All use of this software is
 * subject to MuleSoft's Master Subscription Agreement (or other master license agreement)
 * separately entered into in writing between you and MuleSoft. If such an agreement is not
 * in place, you may not use the software.
 */


package org.mule.api;

/**
 * Exception thrown when connect method in cloud connectors fails to connect properly.
 */
public class ConnectionException extends Exception {
    /**
     * Exception code
     */
    private ConnectionExceptionCode code;

    /**
     * Third-party code
     */
    private String thirdPartyCode;

    /**
     * Create a new connection exception
     *
     * @param code           Code describing what went wrong. Use {@link ConnectionExceptionCode} for unexpected problems.
     * @param thirdPartyCode Code as provided by the third party API
     * @param message        Message describing what went wrong
     */
    public ConnectionException(ConnectionExceptionCode code, String thirdPartyCode, String message) {
        super(message);

        this.code = code;
        this.thirdPartyCode = thirdPartyCode;
    }

    /**
     * Create a new connection exception
     *
     * @param code           Code describing what went wrong. Use {@link ConnectionExceptionCode} for unexpected problems.
     * @param thirdPartyCode Code as provided by the third party API
     * @param throwable      Inner exception
     * @param message        Message describing what went wrong
     */
    public ConnectionException(ConnectionExceptionCode code, String thirdPartyCode, String message, Throwable throwable) {
        super(message, throwable);

        this.code = code;
        this.thirdPartyCode = thirdPartyCode;
    }

    /**
     * Get a code for what went wrong
     *
     * @return A {@link ConnectionExceptionCode}
     */
    public ConnectionExceptionCode getCode() {
        return code;
    }

    /**
     * Get a code for what went wrong as provided
     * by the third party API
     *
     * @return A string with the code
     */
    public String getThirdPartyCode() {
        return thirdPartyCode;
    }
}
