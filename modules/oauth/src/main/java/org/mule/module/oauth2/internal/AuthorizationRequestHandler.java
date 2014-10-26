/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.http.HttpHeaders;
import org.mule.module.http.listener.HttpListener;
import org.mule.module.http.listener.HttpListenerBuilder;
import org.mule.module.http.listener.HttpResponseBuilder;
import org.mule.util.AttributeEvaluator;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the call to the localAuthorizationUrl and redirects the user to the oauth authentication
 * server authorization url so the user can grant access to the resources to the mule application.
 */
public class AuthorizationRequestHandler implements MuleContextAware
{

    public static final String REDIRECT_STATUS_CODE = "302";
    public static final String OAUTH_STATE_ID_FLOW_VAR_NAME = "oauthStateId";

    private Logger logger = LoggerFactory.getLogger(AuthorizationRequestHandler.class);
    private String scopes;
    private String state;
    private String localAuthorizationUrl;
    private String authorizationUrl;
    private Map<String, String> customParameters = new HashMap<String, String>();
    private HttpListener listener;
    private MuleContext muleContext;
    private AuthorizationCodeConfig oauthConfig;
    private AttributeEvaluator oauthStateIdEvaluator;
    private AttributeEvaluator stateEvaluator;

    public void setScopes(final String scopes)
    {
        this.scopes = scopes;
    }

    public void setState(final String state)
    {
        this.state = state;
    }

    public void setLocalAuthorizationUrl(final String localAuthorizationUrl)
    {
        this.localAuthorizationUrl = localAuthorizationUrl;
    }

    public void setAuthorizationUrl(final String authorizationUrl)
    {
        this.authorizationUrl = authorizationUrl;
    }

    public Map<String, String> getCustomParameters()
    {
        return customParameters;
    }

    public void setCustomParameters(final Map<String, String> customParameters)
    {
        this.customParameters = customParameters;
    }

    public void init() throws MuleException
    {
        try
        {
            oauthStateIdEvaluator = new AttributeEvaluator(oauthConfig.getOAuthStateId()).initialize(muleContext.getExpressionManager());
            stateEvaluator = new AttributeEvaluator(state).initialize(muleContext.getExpressionManager());
            final HttpListenerBuilder httpListenerBuilder = new HttpListenerBuilder(muleContext);
            final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
            responseBuilder.setStatusCode(REDIRECT_STATUS_CODE);
            responseBuilder.setMuleContext(muleContext);
            responseBuilder.initialise();
            this.listener = httpListenerBuilder.setUrl(localAuthorizationUrl)
                    .setMuleContext(muleContext)
                    .setResponseBuilder(responseBuilder)
                    .setListenerConfig(oauthConfig.getListenerConfig())
                    .setListener(new MessageProcessor()
                    {
                        @Override
                        public MuleEvent process(MuleEvent muleEvent) throws MuleException
                        {
                            final String oauthStateId = oauthStateIdEvaluator.resolveStringValue(muleEvent);
                            muleEvent.setFlowVariable(OAUTH_STATE_ID_FLOW_VAR_NAME, oauthStateId);
                            final String stateValue = stateEvaluator.resolveStringValue(muleEvent);
                            String currentState = stateValue;
                            if (oauthStateId != null)
                            {
                                currentState = StateEncoder.encodeOAuthStateIdInState(stateValue, oauthStateId);
                            }
                            final String authorizationUrlWithParams = new AuthorizationRequestUrlBuilder()
                                    .setAuthorizationUrl(authorizationUrl)
                                    .setClientId(oauthConfig.getClientId())
                                    .setClientSecret(oauthConfig.getClientSecret())
                                    .setCustomParameters(customParameters)
                                    .setRedirectUrl(oauthConfig.getRedirectionUrl())
                                    .setState(currentState)
                                    .setScope(scopes).buildUrl();

                            muleEvent.getMessage().setOutboundProperty(HttpHeaders.Names.LOCATION, authorizationUrlWithParams);
                            return muleEvent;
                        }
                    }).build();
            this.listener.start();
        }
        catch (MalformedURLException e)
        {
            logger.warn("Could not parse provided url %s. Validate that the url is correct", localAuthorizationUrl);
            throw new DefaultMuleException(e);
        }
    }

    @Override
    public void setMuleContext(final MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public void setOauthConfig(final AuthorizationCodeConfig oauthConfig)
    {
        this.oauthConfig = oauthConfig;
    }

    public AuthorizationCodeConfig getOauthConfig()
    {
        return oauthConfig;
    }
}
