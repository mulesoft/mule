/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.http.api.requester.HttpRequester;
import org.mule.module.http.api.requester.HttpRequesterBuilder;

public abstract class AbstractTokenRequestHandler implements MuleContextAware, Initialisable
{

    private MuleContext muleContext;
    private String refreshTokenWhen = OAuthConstants.DEFAULT_REFRESH_TOKEN_WHEN_EXPRESSION;
    private HttpRequester httpRequester;
    private String tokenUrl;


    /**
     * @param refreshTokenWhen expression to use to determine if the response from a request to the API requires a new token
     */
    public void setRefreshTokenWhen(String refreshTokenWhen)
    {
        this.refreshTokenWhen = refreshTokenWhen;
    }

    public String getRefreshTokenWhen()
    {
        return refreshTokenWhen;
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    protected MuleContext getMuleContext()
    {
        return muleContext;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            httpRequester = new HttpRequesterBuilder(getMuleContext())
                    .setUrl(tokenUrl)
                    .setMethod("POST")
                    .build();
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    public void setTokenUrl(String tokenUrl)
    {
        this.tokenUrl = tokenUrl;
    }

    protected MuleEvent invokeTokenUrl(final MuleEvent event) throws MuleException
    {
        return httpRequester.process(event);
    }

}
