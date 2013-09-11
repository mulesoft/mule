/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.process;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.devkit.ProcessInterceptor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.common.connection.exception.UnableToAcquireConnectionException;
import org.mule.common.connection.exception.UnableToReleaseConnectionException;
import org.mule.devkit.processor.ExpressionEvaluatorSupport;
import org.mule.security.oauth.OAuth2Adapter;
import org.mule.security.oauth.OAuth2Manager;
import org.mule.security.oauth.callback.ProcessCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedAccessTokenProcessInterceptor<T> extends ExpressionEvaluatorSupport
    implements ProcessInterceptor<T, OAuth2Adapter>
{

    private static Logger logger = LoggerFactory.getLogger(ManagedAccessTokenProcessInterceptor.class);
    private final OAuth2Manager<OAuth2Adapter> oauthManager;
    private final ProcessInterceptor<T, OAuth2Adapter> next;

    public ManagedAccessTokenProcessInterceptor(ProcessInterceptor<T, OAuth2Adapter> next,
                                                OAuth2Manager<OAuth2Adapter> oauthManager)
    {
        this.next = next;
        this.oauthManager = oauthManager;
    }

    @Override
    public T execute(ProcessCallback<T, OAuth2Adapter> processCallback,
                     OAuth2Adapter object,
                     MessageProcessor messageProcessor,
                     MuleEvent event) throws Exception
    {
        OAuth2Adapter connector = null;
        if (!processCallback.isProtected())
        {
            return processCallback.process(this.oauthManager.getDefaultUnauthorizedConnector());
        }

        String accessTokenId = this.getAccessTokenId(event, messageProcessor, this.oauthManager);
        processCallback.setAccessTokenId(accessTokenId);

        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format(
                    "Attempting to acquire access token using from store for [accessTokenId=%s]",
                    accessTokenId));
            }
            connector = oauthManager.acquireAccessToken(accessTokenId);
            if (connector == null)
            {
                throw new UnableToAcquireConnectionException();
            }
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Access token has been acquired for [accessTokenId=%s]",
                        accessTokenId));
                }
            }
            return next.execute(processCallback, connector, messageProcessor, event);
        }
        catch (Exception e)
        {
            if ((processCallback.getManagedExceptions() != null) && (connector != null))
            {
                for (Class<? extends Exception> exceptionClass : processCallback.getManagedExceptions())
                {
                    if (exceptionClass.isInstance(e))
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug(String.format(
                                "An exception (%s) has been thrown. Destroying the access token with [accessTokenId=%s]",
                                exceptionClass.getName(), accessTokenId));
                        }
                        try
                        {
                            oauthManager.destroyAccessToken(accessTokenId, connector);
                            connector = null;
                        }
                        catch (Exception innerException)
                        {
                            logger.error(innerException.getMessage(), innerException);
                        }
                    }
                }
            }
            throw e;
        }
        finally
        {
            try
            {
                if (connector != null)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(String.format(
                            "Releasing the access token back into the pool [accessTokenId=%s]", accessTokenId));
                    }
                    oauthManager.releaseAccessToken(accessTokenId, connector);
                }
            }
            catch (Exception e)
            {
                throw new UnableToReleaseConnectionException(e);
            }
        }
    }

    @Override
    public T execute(ProcessCallback<T, OAuth2Adapter> processCallback,
                     OAuth2Adapter object,
                     Filter filter,
                     MuleMessage message) throws Exception
    {
        throw new UnsupportedOperationException();
    }

}
