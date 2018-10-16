/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import static org.mule.module.http.internal.request.RequestResourcesUtils.closeResources;

import java.io.IOException;
import java.io.OutputStream;

import org.mule.module.http.internal.domain.request.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.BodyDeferringAsyncHandler;
import com.ning.http.client.Response;

/**
 * Mule Body Deferring Async Handler to guarantee that the request streams are closed
 * 
 * @since 3.9.2
 */
public class MuleBodyDeferringAsyncHandler extends BodyDeferringAsyncHandler
{
    private HttpRequest request;

    public MuleBodyDeferringAsyncHandler(OutputStream os, HttpRequest request)
    {
        super(os);
        this.request = request;
    }

    @Override
    public Response onCompleted() throws IOException
    {
        try
        {
            return super.onCompleted();
        }
        finally
        {
            closeResources(request);
        }
    }

}
