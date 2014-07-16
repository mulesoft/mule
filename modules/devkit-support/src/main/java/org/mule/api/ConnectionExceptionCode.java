/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

/**
 * List of possible outcomes to a connection failure
 */
public enum ConnectionExceptionCode
{
    /**
     * The host cannot be resolved to an IP address
     */
    UNKNOWN_HOST,
    /**
     * The destination cannot be reached. Either the host is wrong
     * or the port might be.
     */
    CANNOT_REACH,
    /**
     * The supplied credentials are not correct.
     */
    INCORRECT_CREDENTIALS,
    /**
     * The credentials used to authenticate has expired.
     */
    CREDENTIALS_EXPIRED,
    /**
     * Something else went wrong.
     */
    UNKNOWN;
}
