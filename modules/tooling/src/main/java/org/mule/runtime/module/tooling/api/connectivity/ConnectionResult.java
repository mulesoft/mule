/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.api.connectivity;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.module.tooling.api.connectivity.ConnectionResult.Status.FAILURE;
import static org.mule.runtime.module.tooling.api.connectivity.ConnectionResult.Status.SUCCESS;

import java.util.Optional;

/**
 * Result of doing connectivity testing in a mule component
 *
 * @since 4.0
 */
public class ConnectionResult
{

    public enum Status {SUCCESS, FAILURE}

    private Status status;
    private String failureMessage;
    private Optional<Exception> exception = empty();

    private ConnectionResult()
    {
    }

    /**
     * @param message the reason of the failure
     * @return a failure {@code ConnectionResult}
     */
    public static ConnectionResult createFailureResult(String message)
    {
        return createFailureResult(message, empty());
    }

    /**
     * @param message the reason of the failure
     * @param e the exception that was the cause of the failure
     * @return a failure {@code ConnectionResult}
     */
    public static ConnectionResult createFailureResult(String message, Exception e)
    {
        return createFailureResult(message, of(e));
    }

    private static ConnectionResult createFailureResult(String message, Optional<Exception> e)
    {
        ConnectionResult connectionResult = new ConnectionResult();
        connectionResult.status = FAILURE;
        connectionResult.failureMessage = message;
        connectionResult.exception = e;
        return connectionResult;
    }

    /**
     * @return a successful {@code ConnectionResult}
     */
    public static ConnectionResult createSuccessResult()
    {
        ConnectionResult connectionResult = new ConnectionResult();
        connectionResult.status = SUCCESS;
        return connectionResult;
    }

    /**
     * @return the status of the connection testing
     */
    public Status getStatus()
    {
        return status;
    }

    /**
     * @return a message with the reason of the failure
     */
    public String getFailureMessage()
    {
        return failureMessage;
    }

    /**
     * @return the exception that cause the failure. This optional may be empty even if there was a failure.
     */
    public Optional<Exception> getException()
    {
        return exception;
    }
}
