/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth.process;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.ProcessInterceptor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.common.connection.exception.UnableToAcquireConnectionException;
import org.mule.common.connection.exception.UnableToReleaseConnectionException;
import org.mule.security.oauth.OAuth2Adapter;
import org.mule.security.oauth.OAuth2Manager;
import org.mule.security.oauth.callback.ProcessCallback;
import org.mule.security.oauth.processor.AbstractConnectedProcessor;
import org.mule.security.oauth.processor.AbstractExpressionEvaluator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedAccessTokenProcessInterceptor<T> extends AbstractExpressionEvaluator
    implements ProcessInterceptor<T, OAuth2Adapter>
{

    private static Logger logger = LoggerFactory.getLogger(ManagedAccessTokenProcessInterceptor.class);
    private final OAuth2Manager<OAuth2Adapter> oauthManager;
    private final MuleContext muleContext;
    private final ProcessInterceptor<T, OAuth2Adapter> next;

    public ManagedAccessTokenProcessInterceptor(ProcessInterceptor<T, OAuth2Adapter> next,
                                                OAuth2Manager<OAuth2Adapter> oauthManager,
                                                MuleContext muleContext)
    {
        this.next = next;
        this.oauthManager = oauthManager;
        this.muleContext = muleContext;
    }

    public T execute(ProcessCallback<T, OAuth2Adapter> processCallback, OAuth2Adapter object, MessageProcessor messageProcessor, MuleEvent event)
    throws Exception
{
    OAuth2Adapter connector = null;
    if (!processCallback.isProtected()) {
        return processCallback.process(oauthManager.getDefaultUnauthorizedConnector());
    }
    if (((AbstractConnectedProcessor) messageProcessor).getAccessTokenId() == null) {
        throw new IllegalArgumentException("The accessTokenId cannot be null");
    }
    String _transformedToken = ((String) evaluateAndTransform(muleContext, event, AbstractConnectedProcessor.class.getDeclaredField("_accessTokenIdType").getGenericType(), null, ((AbstractConnectedProcessor) messageProcessor).getAccessTokenId()));
    try {
        if (logger.isDebugEnabled()) {
            logger.debug(("Attempting to acquire access token using from store for [accessTokenId="+ _transformedToken.toString()));
        }
        connector = oauthManager.acquireAccessToken(_transformedToken);
        if (connector == null) {
            throw new UnableToAcquireConnectionException();
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug((("Access token has been acquired for [accessTokenId="+ connector.getAccessTokenId())+"]"));
            }
        }
        return next.execute(processCallback, connector, messageProcessor, event);
    } catch (Exception e) {
        if ((processCallback.getManagedExceptions()!= null)&&(connector!= null)) {
            for (Class<? extends Exception> exceptionClass: processCallback.getManagedExceptions()) {
                if (exceptionClass.isInstance(e)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug((((("An exception ( "+ exceptionClass.getName())+") has been thrown. Destroying the access token with [accessTokenId=")+ connector.getAccessTokenId())+"]"));
                    }
                    try {
                        oauthManager.destroyAccessToken(_transformedToken, connector);
                        connector = null;
                    } catch (Exception innerException) {
                        logger.error(innerException.getMessage(), innerException);
                    }
                }
            }
        }
        throw e;
    } finally {
        try {
            if (connector!= null) {
                if (logger.isDebugEnabled()) {
                    logger.debug((("Releasing the access token back into the pool [accessTokenId="+ connector.getAccessTokenId())+"]"));
                }
                oauthManager.releaseAccessToken(_transformedToken, connector);
            }
        } catch (Exception e) {
            throw new UnableToReleaseConnectionException(e);
        }
    }
}

    public T execute(ProcessCallback<T, OAuth2Adapter> processCallback,
                     OAuth2Adapter object,
                     Filter filter,
                     MuleMessage message) throws Exception
    {
        throw new UnsupportedOperationException();
    }

}
