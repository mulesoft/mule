/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import org.mule.api.MuleEvent;

/**
 * It was not possible to retrieve the access token or the refresh token from the token URL response
 */
public class TokenNotFoundException extends Exception
{

    private final TokenResponseProcessor tokenResponseProcessor;
    private final MuleEvent tokenUrlResponse;

    public TokenNotFoundException(MuleEvent tokenUrlResponse, TokenResponseProcessor tokenResponseProcessor)
    {
        this.tokenUrlResponse = tokenUrlResponse;
        this.tokenResponseProcessor = tokenResponseProcessor;
    }

    public TokenResponseProcessor getTokenResponseProcessor()
    {
        return tokenResponseProcessor;
    }

    public MuleEvent getTokenUrlResponse()
    {
        return tokenUrlResponse;
    }
}
