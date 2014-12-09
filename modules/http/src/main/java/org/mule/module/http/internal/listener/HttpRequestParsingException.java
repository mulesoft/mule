/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

/**
 * Exception thrown when there is a problem processing an incoming HttpRequest to generate a MuleEvent.
 */
public class HttpRequestParsingException extends Exception
{

    public HttpRequestParsingException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
