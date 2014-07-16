/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.process;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.devkit.ProcessInterceptor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.routing.filter.Filter;
import org.mule.devkit.processor.ExpressionEvaluatorSupport;
import org.mule.security.oauth.OAuth2Adapter;
import org.mule.security.oauth.RefreshTokenManager;
import org.mule.security.oauth.callback.ProcessCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefreshTokenProcessInterceptor<T> extends ExpressionEvaluatorSupport
    implements ProcessInterceptor<T, OAuth2Adapter>
{

    private transient static final Logger logger = LoggerFactory.getLogger(RefreshTokenProcessInterceptor.class);
    private final ProcessInterceptor<T, OAuth2Adapter> next;
    private RefreshTokenManager refreshTokenManager;

    public RefreshTokenProcessInterceptor(ProcessInterceptor<T, OAuth2Adapter> next, MuleContext context)
    {
        this.next = next;
        try
        {
            this.refreshTokenManager = context.getRegistry().lookupObject(RefreshTokenManager.class);
        }
        catch (RegistrationException e)
        {
            throw new IllegalStateException(
                "Could not create interceptor because RefreshTokenManager could not be found in the registry",
                e);
        }
    }

    public T execute(ProcessCallback<T, OAuth2Adapter> processCallback,
                     OAuth2Adapter object,
                     MessageProcessor messageProcessor,
                     MuleEvent event) throws Exception
    {
        T result = null;
        try
        {
            result = this.next.execute(processCallback, object, messageProcessor, event);
            return result;
        }
        catch (Exception e)
        {
            if (processCallback.getManagedExceptions() != null)
            {
                for (Class<? extends Exception> exceptionClass : processCallback.getManagedExceptions())
                {
                    if (exceptionClass.isInstance(e))
                    {
                        if (((OAuth2Adapter) object).getRefreshToken() != null)
                        {
                            if (logger.isDebugEnabled())
                            {
                                logger.debug("A managed exception has been thrown. Attempting to refresh access token.");
                            }
                            try
                            {
                                this.refreshTokenManager.refreshToken(object,
                                    processCallback.getAccessTokenId());
                            }
                            catch (Exception newException)
                            {
                                if (logger.isDebugEnabled())
                                {
                                    logger.debug(
                                        "Another exception was thrown while attempting to refresh the access token. Throwing original exception back up",
                                        newException);
                                }
                                throw e;
                            }
                            result = this.next.execute(processCallback, object, messageProcessor, event);
                            return result;
                        }
                    }
                }
            }
            throw e;
        }
    }

    public T execute(ProcessCallback<T, OAuth2Adapter> processCallback,
                     OAuth2Adapter object,
                     Filter filter,
                     MuleMessage event) throws Exception
    {
        throw new UnsupportedOperationException();
    }

}
