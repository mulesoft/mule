/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

import org.mule.api.transport.Connector;
import org.mule.common.security.oauth.exception.NotAuthorizedException;
import org.mule.common.security.oauth.exception.UnableToAcquireAccessTokenException;
import org.mule.common.security.oauth.exception.UnableToAcquireRequestTokenException;
import org.mule.security.oauth.callback.DefaultHttpCallbackAdapter;
import org.mule.security.oauth.callback.RestoreAccessTokenCallback;
import org.mule.security.oauth.callback.SaveAccessTokenCallback;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import org.slf4j.Logger;

public abstract class BaseOAuth1Manager extends DefaultHttpCallbackAdapter implements OAuth1Manager
{

    protected abstract Logger getLogger();

    /**
     * {@inheritDoc}
     */
    @Override
    public String buildAuthorizeUrl(OAuth1Adapter adapter,
                                    Map<String, String> extraParameters,
                                    String requestTokenUrl,
                                    String accessTokenUrl,
                                    String authorizationUrl,
                                    String redirectUri) throws UnableToAcquireRequestTokenException
    {
        List<String> customOAuthParameters = new ArrayList<String>();

        for (Map.Entry<String, String> entry : extraParameters.entrySet())
        {
            customOAuthParameters.add(entry.getKey());
            customOAuthParameters.add(entry.getValue());
        }

        requestTokenUrl = requestTokenUrl != null ? requestTokenUrl : adapter.getRequestTokenUrl();

        String scope = adapter.getScope();

        if (scope != null)
        {
            try
            {
                String scopeParam = "?scope=".concat(URLEncoder.encode(scope, "UTF-8"));
                requestTokenUrl = requestTokenUrl.concat(scopeParam);
            }
            catch (UnsupportedEncodingException e)
            {
                throw new RuntimeException(e);
            }
        }

        OAuthProvider provider = new DefaultOAuthProvider(requestTokenUrl,
            accessTokenUrl != null ? accessTokenUrl : adapter.getAccessTokenUrl(),
            authorizationUrl != null ? authorizationUrl : adapter.getAuthorizationUrl());

        OAuthConsumer consumer = this.getConsumer(adapter);
        provider.setOAuth10a(true);
        String signedAuthorizationUrl;

        if (getLogger().isDebugEnabled())
        {
            getLogger().debug(
                String.format("Attempting to acquire a request token [consumer = %s] [consumerSecret = %s]",
                    consumer.getConsumerKey(), consumer.getConsumerSecret()));
        }

        try
        {

            signedAuthorizationUrl = provider.retrieveRequestToken(consumer, redirectUri,
                customOAuthParameters.toArray(new String[]{}));

            if (getLogger().isDebugEnabled())
            {
                getLogger().debug(
                    String.format("Request token acquired [requestToken = %s] [requestTokenSecret = %s]",
                        consumer.getToken(), consumer.getTokenSecret()));
            }
        }
        catch (OAuthMessageSignerException e)
        {
            throw new UnableToAcquireRequestTokenException(e);
        }
        catch (OAuthNotAuthorizedException e)
        {
            throw new UnableToAcquireRequestTokenException(e);
        }
        catch (OAuthExpectationFailedException e)
        {
            throw new UnableToAcquireRequestTokenException(e);
        }
        catch (OAuthCommunicationException e)
        {
            throw new UnableToAcquireRequestTokenException(e);
        }

        adapter.setRequestToken(consumer.getToken());
        adapter.setRequestTokenSecret(consumer.getTokenSecret());

        return signedAuthorizationUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean restoreAccessToken(OAuth1Adapter adapter)
    {
        RestoreAccessTokenCallback restore = adapter.getOauthRestoreAccessToken();
        if (restore != null)
        {

            if (getLogger().isDebugEnabled())
            {
                getLogger().debug("Attempting to restore access token...");
            }

            try
            {
                restore.restoreAccessToken();
                adapter.setAccessToken(restore.getAccessToken());
                adapter.setAccessTokenSecret(restore.getAccessTokenSecret());

                this.getConsumer(adapter).setTokenWithSecret(restore.getAccessToken(),
                    restore.getAccessTokenSecret());

                if (getLogger().isDebugEnabled())
                {
                    getLogger().debug(
                        String.format(
                            "Access token and secret has been restored successfully [accessToken = %s] [accessTokenSecret = %s]",
                            restore.getAccessToken(), restore.getAccessTokenSecret()));
                }
                return true;

            }
            catch (Exception e)
            {
                getLogger().error("Cannot restore access token, an unexpected error occurred", e);
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fetchAccessToken(OAuth1Adapter adapter,
                                 String requestTokenUrl,
                                 String accessTokenUrl,
                                 String authorizationUrl,
                                 String redirectUri) throws UnableToAcquireAccessTokenException
    {
        this.restoreAccessToken(adapter);

        if (adapter.getAccessToken() == null || adapter.getAccessTokenSecret() == null)
        {
            requestTokenUrl = requestTokenUrl != null ? requestTokenUrl : adapter.getRequestTokenUrl();
            String scope = adapter.getScope();

            if (scope != null)
            {
                try
                {
                    String scopeParam = "?scope=".concat(URLEncoder.encode(scope, "UTF-8"));
                    requestTokenUrl = requestTokenUrl.concat(scopeParam);
                }
                catch (UnsupportedEncodingException e)
                {
                    throw new RuntimeException(e);
                }
            }

            OAuthProvider provider = new DefaultOAuthProvider(requestTokenUrl,
                accessTokenUrl != null ? accessTokenUrl : adapter.getAccessTokenUrl(),
                authorizationUrl != null ? authorizationUrl : adapter.getAuthorizationUrl());
            provider.setOAuth10a(true);

            OAuthConsumer consumer = this.getConsumer(adapter);
            consumer.setTokenWithSecret(adapter.getRequestToken(), adapter.getRequestTokenSecret());

            if (getLogger().isDebugEnabled())
            {
                getLogger().debug("Retrieving access token...");
            }

            try
            {
                provider.retrieveAccessToken(consumer, adapter.getOauthVerifier());
            }
            catch (OAuthMessageSignerException e)
            {
                throw new UnableToAcquireAccessTokenException(e);
            }
            catch (OAuthNotAuthorizedException e)
            {
                throw new UnableToAcquireAccessTokenException(e);
            }
            catch (OAuthExpectationFailedException e)
            {
                throw new UnableToAcquireAccessTokenException(e);
            }
            catch (OAuthCommunicationException e)
            {
                throw new UnableToAcquireAccessTokenException(e);
            }

            adapter.setAccessToken(consumer.getToken());
            adapter.setAccessTokenSecret(consumer.getTokenSecret());

            if (getLogger().isDebugEnabled())
            {
                getLogger().debug(
                    String.format(
                        "Access token retrieved successfully [accessToken = %s] [accessTokenSecret = %s]",
                        adapter.getAccessToken(), adapter.getAccessTokenSecret()));
            }

            SaveAccessTokenCallback save = adapter.getOauthSaveAccessToken();
            if (save != null)
            {
                if (getLogger().isDebugEnabled())
                {
                    getLogger().debug(
                        String.format(
                            "Attempting to save access token... [accessToken = %s] [accessTokenSecret = %s]",
                            adapter.getAccessToken(), adapter.getAccessTokenSecret()));
                }

                try
                {
                    save.saveAccessToken(adapter.getAccessToken(), adapter.getAccessTokenSecret());
                }
                catch (Exception e)
                {
                    getLogger().error("Cannot save access token, an unexpected error occurred", e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void hasBeenAuthorized(OAuth1Adapter adapter) throws NotAuthorizedException
    {
        if (adapter.getAccessToken() == null)
        {
            this.restoreAccessToken(adapter);
            if (adapter.getAccessToken() == null)
            {
                throw new NotAuthorizedException(
                    "This connector has not yet been authorized, please authorize by calling \"authorize\".");
            }
        }
    }

    @Override
    public Connector getConnector()
    {
        return (Connector) super.getConnector();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset(OAuth1Adapter adapter)
    {
        adapter.setAccessToken(null);
        adapter.setAccessTokenSecret(null);
        OAuthConsumer consumer = adapter.getConsumer();
        if (consumer != null) {
            consumer.setTokenWithSecret(null, null);
        }
    }

    protected OAuthConsumer getConsumer(OAuth1Adapter adapter)
    {
        OAuthConsumer consumer = adapter.getConsumer();
        
        if (consumer == null)
        {
            consumer = new DefaultOAuthConsumer(adapter.getConsumerKey(), adapter.getConsumerSecret());
            consumer.setMessageSigner(adapter.getMessageSigner());
            consumer.setSigningStrategy(adapter.getSigningStrategy());
            
            adapter.setConsumer(consumer);
        }
        
        return consumer;
    }
}
