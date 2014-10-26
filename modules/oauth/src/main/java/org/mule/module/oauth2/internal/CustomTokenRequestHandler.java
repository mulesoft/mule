/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.construct.Flow;
import org.mule.module.oauth2.internal.state.UserOAuthState;

/**
 * Token request handler that allows to customize the token url call and refresh token call by using
 * custom flows.
 */
public class CustomTokenRequestHandler extends AbstractTokenRequestHandler
{

    private Flow tokenUrlCallFlow;
    private Flow refreshTokenFlow;


    @Override
    protected void doRefreshToken(MuleEvent currentEvent, UserOAuthState userOAuthState) throws MuleException
    {
        this.refreshTokenFlow.process(currentEvent);
    }

    public void setTokenUrlCallFlow(final Flow tokenUrlCallFlow)
    {
        this.tokenUrlCallFlow = tokenUrlCallFlow;
    }

    public void setRefreshTokenFlow(final Flow refreshTokenFlow)
    {
        this.refreshTokenFlow = refreshTokenFlow;
    }

    @Override
    public void init() throws MuleException
    {
        try
        {
            if (tokenUrlCallFlow == null)
            {
                throw new DefaultMuleException(CoreMessages.createStaticMessage("You must configure tokenUrlCallFlow in your oauth config or provide a flow listening in the redirect url to handel calls to redirect url in the oauth dance."));
            }
            createListenerForRedirectUrl();
        }
        catch (Exception e)
        {
            logger.info("No listener was created for OAuth config %s in the redirect url since there's already once registered probably by a flow in the app config", getOauthConfig().getConfigName());
        }
    }

    @Override
    protected MessageProcessor createRedirectUrlProcessor()
    {
        return tokenUrlCallFlow;
    }

}
