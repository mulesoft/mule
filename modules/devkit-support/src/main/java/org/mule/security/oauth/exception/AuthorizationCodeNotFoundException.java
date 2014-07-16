/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth.exception;

import java.util.regex.Pattern;

/**
 * Exception to signal that an OAuth http callback was received but an authorization
 * code could not be found on it.
 */
public class AuthorizationCodeNotFoundException extends Exception
{

    private static final long serialVersionUID = 2239738983890995605L;

    private final Pattern pattern;
    private final String callbackResponse;

    /**
     * Creates a new instance
     * 
     * @param pattern the {@link Pattern} used to extract the authorization code from
     *            the response
     * @param callbackResponse the callback's response
     */
    public AuthorizationCodeNotFoundException(Pattern pattern, String callbackResponse)
    {
        this.pattern = pattern;
        this.callbackResponse = callbackResponse;
    }

    /**
     * @return the {@link Pattern} used to extract the authorization code from the
     *         response
     */
    public Pattern getPattern()
    {
        return pattern;
    }

    /**
     * @return the callback's response
     */
    public String getCallbackResponse()
    {
        return callbackResponse;
    }
}
