/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.clientcredentials;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.construct.Flow;
import org.mule.module.oauth2.internal.ClientApplicationCredentials;
import org.mule.module.oauth2.internal.NameValuePair;
import org.mule.module.oauth2.internal.OAuthConstants;
import org.mule.module.oauth2.internal.TokenResponseProcessor;
import org.mule.module.oauth2.internal.AutoTokenRequestHandler;
import org.mule.module.oauth2.internal.authorizationcode.TokenResponseConfiguration;
import org.mule.transport.NullPayload;

import java.util.HashMap;

public class ClientCredentialsTokenRequestHandler extends AutoTokenRequestHandler
{
    private String scopes;
    private ClientApplicationCredentials clientApplicationCredentials;
    private TokenResponseConfiguration tokenResponseConfiguration = new TokenResponseConfiguration();
    private ObjectStoreClientCredentialsStore clientCredentialsStore;

    public void setClientCredentialsStore(ObjectStoreClientCredentialsStore clientCredentialsStore)
    {
        this.clientCredentialsStore = clientCredentialsStore;
    }

    public void setClientApplicationCredentials(ClientApplicationCredentials clientApplicationCredentials)
    {
        this.clientApplicationCredentials = clientApplicationCredentials;
    }

    public void setScopes(String scopes)
    {
        this.scopes = scopes;
    }

    public void setTokenResponseConfiguration(TokenResponseConfiguration tokenResponseConfiguration)
    {
        this.tokenResponseConfiguration = tokenResponseConfiguration;
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        super.setMuleContext(muleContext);
    }

    private void setMapPayloadWithTokenRequestParameters(final MuleEvent event)
    {
        final HashMap<String, String> formData = new HashMap<String, String>();
        formData.put(OAuthConstants.CLIENT_ID_PARAMETER, clientApplicationCredentials.getClientId());
        formData.put(OAuthConstants.CLIENT_SECRET_PARAMETER, clientApplicationCredentials.getClientSecret());
        formData.put(OAuthConstants.GRANT_TYPE_PARAMETER, OAuthConstants.GRANT_TYPE_CLIENT_CREDENTIALS);
        if (scopes != null)
        {
            formData.put(OAuthConstants.SCOPE_PARAMETER, scopes);
        }
        event.getMessage().setPayload(formData);
    }

    public void refreshAccessToken() throws MuleException
    {
        final DefaultMuleEvent accessTokenEvent = new DefaultMuleEvent(new DefaultMuleMessage(NullPayload.getInstance(), getMuleContext()), MessageExchangePattern.REQUEST_RESPONSE, new Flow("test", getMuleContext()));
        setMapPayloadWithTokenRequestParameters(accessTokenEvent);
        final MuleEvent response = invokeTokenUrl(accessTokenEvent);
        final TokenResponseProcessor tokenResponseProcessor = TokenResponseProcessor.createClientCredentialsrocessor(tokenResponseConfiguration, getMuleContext().getExpressionManager());
        tokenResponseProcessor.process(response);
        clientCredentialsStore.storeAccessToken(tokenResponseProcessor.getAccessToken());
        clientCredentialsStore.storeExpiresIn(tokenResponseProcessor.getExpiresIn());
        for (NameValuePair parameter : tokenResponseProcessor.getCustomResponseParameters())
        {
            clientCredentialsStore.storeCustomParameter(parameter.getName(), parameter.getValue());
        }
    }
}
