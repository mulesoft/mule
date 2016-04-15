/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import org.mule.module.http.internal.domain.request.HttpRequestContext;
import org.mule.module.http.internal.domain.response.HttpResponseBuilder;
import org.mule.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.module.http.internal.listener.async.RequestHandler;
import org.mule.module.http.internal.listener.async.ResponseStatusCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request handle for request calls to paths with no listener configured.
 */
public class ServiceTemporarilyUnavailableListenerRequestHandler implements RequestHandler
{

    public static final int SERVICE_TEMPORARILY_UNAVAILABLE_STATUS_CODE = 503;

    private Logger logger = LoggerFactory.getLogger(ServiceTemporarilyUnavailableListenerRequestHandler.class);

    private static ServiceTemporarilyUnavailableListenerRequestHandler instance = new ServiceTemporarilyUnavailableListenerRequestHandler();

    private ServiceTemporarilyUnavailableListenerRequestHandler()
    {

    }

    public static ServiceTemporarilyUnavailableListenerRequestHandler getInstance()
    {
        return instance;
    }

    @Override
    public void handleRequest(HttpRequestContext requestContext, HttpResponseReadyCallback responseCallback)
    {
        responseCallback.responseReady(new HttpResponseBuilder()
                                               .setStatusCode(SERVICE_TEMPORARILY_UNAVAILABLE_STATUS_CODE)
                                               .setReasonPhrase("Service Temporarily Unavailable")
                                               .build(), new ResponseStatusCallback()
        {
            @Override
            public void responseSendFailure(Throwable exception)
            {
                logger.warn("Error while sending 503 response " + exception.getMessage());
                if (logger.isDebugEnabled())
                {
                    logger.debug("exception thrown",exception);
                }
            }

            @Override
            public void responseSendSuccessfully()
            {
            }
        });
    }

}
