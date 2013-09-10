/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;


/**
 * Exception thrown when connect method in cloud connectors fails to connect properly.
 */
public class ConnectionException extends Exception
{

    private static final long serialVersionUID = 1131270076379901356L;

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
    public ConnectionException(ConnectionExceptionCode code, String thirdPartyCode, String message)
    {
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
    public ConnectionException(ConnectionExceptionCode code, String thirdPartyCode, String message, Throwable throwable)
    {
        super(message, throwable);

        this.code = code;
        this.thirdPartyCode = thirdPartyCode;
    }

    /**
     * Get a code for what went wrong
     *
     * @return A {@link ConnectionExceptionCode}
     */
    public ConnectionExceptionCode getCode()
    {
        return code;
    }

    /**
     * Get a code for what went wrong as provided
     * by the third party API
     *
     * @return A string with the code
     */
    public String getThirdPartyCode()
    {
        return thirdPartyCode;
    }
}
