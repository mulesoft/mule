/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.http.api.requester.HttpRequester;
import org.mule.module.http.api.requester.HttpRequesterBuilder;

public class AutoTokenRequestHandler extends AbstractTokenRequestHandler implements Initialisable
{

    private HttpRequester httpRequester;
    private String tokenUrl;

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            httpRequester = new HttpRequesterBuilder(getMuleContext())
                    .setAddress(tokenUrl)
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
