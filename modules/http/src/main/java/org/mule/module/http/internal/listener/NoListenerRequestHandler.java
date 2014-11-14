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
public class NoListenerRequestHandler implements RequestHandler
{

    public static final int RESOURCE_NOT_FOUND_STATUS_CODE = 404;

    private Logger logger = LoggerFactory.getLogger(NoListenerRequestHandler.class);

    private static NoListenerRequestHandler instance = new NoListenerRequestHandler();

    private NoListenerRequestHandler()
    {

    }

    public static NoListenerRequestHandler getInstance()
    {
        return instance;
    }

    @Override
    public void handleRequest(HttpRequestContext requestContext, HttpResponseReadyCallback responseCallback)
    {
        responseCallback.responseReady(new HttpResponseBuilder()
                                               .setStatusCode(RESOURCE_NOT_FOUND_STATUS_CODE)
                                               .setReasonPhrase("No listener for endpoint: " + requestContext.getRequest().getUri())
                                               .build(), new ResponseStatusCallback()
        {
            @Override
            public void responseSendFailure(Throwable exception)
            {
                logger.warn("Error while sending 404 response " + exception.getMessage());
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
