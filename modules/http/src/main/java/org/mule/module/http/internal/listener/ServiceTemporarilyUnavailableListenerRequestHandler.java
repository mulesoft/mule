/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static org.mule.module.http.api.HttpConstants.HttpStatus.SERVICE_UNAVAILABLE;

/**
 * Request handler for request calls when service is unavailable
 */
public class ServiceTemporarilyUnavailableListenerRequestHandler extends ErrorRequestHandler
{
    private static ServiceTemporarilyUnavailableListenerRequestHandler instance = new ServiceTemporarilyUnavailableListenerRequestHandler();

    private ServiceTemporarilyUnavailableListenerRequestHandler()
    {
        super(SERVICE_UNAVAILABLE.getStatusCode(), SERVICE_UNAVAILABLE.getReasonPhrase(), "Service not available for request uri: %s");
    }

    public static ServiceTemporarilyUnavailableListenerRequestHandler getInstance()
    {
        return instance;
    }

}