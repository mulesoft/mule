/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.transport.http.HttpConnector;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;

import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.internal.JerseyRequestTimeoutHandler;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

class MuleResponseWriter implements ContainerResponseWriter
{

    private ContainerResponse response = null;

    private final String requestMethodName;
    private final MuleEvent muleEvent;
    private final JerseyRequestTimeoutHandler requestTimeoutHandler;
    private final DeferredForwardOutputStream outputStream = new DeferredForwardOutputStream();

    MuleResponseWriter(MuleEvent muleEvent,
                       String requestMethodName,
                       ScheduledExecutorService backgroundScheduler)
    {
        this.muleEvent = muleEvent;
        this.requestMethodName = requestMethodName;
        this.requestTimeoutHandler = new JerseyRequestTimeoutHandler(this, backgroundScheduler);
    }

    @Override
    public OutputStream writeResponseStatusAndHeaders(final long contentLength, final ContainerResponse response)
    {
        this.response = response;

        if (contentLength >= 0)
        {
            response.getHeaders().putSingle(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
        }

        MuleMessage muleMessage = muleEvent.getMessage();

        for (Map.Entry<String, List<Object>> e : response.getHeaders().entrySet())
        {
            muleMessage.setOutboundProperty(e.getKey(), getHeaderValue(e.getValue()));
        }

        muleMessage.setInvocationProperty(JerseyResourcesComponent.JERSEY_RESPONSE, response);
        muleMessage.setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, response.getStatus());

        return getOutputStream();
    }

    @Override
    public boolean suspend(final long time, final TimeUnit unit, final TimeoutHandler handler)
    {
        return requestTimeoutHandler.suspend(time, unit, handler);
    }

    @Override
    public void setSuspendTimeout(final long time, final TimeUnit unit)
    {
        requestTimeoutHandler.setSuspendTimeout(time, unit);
    }

    @Override
    public void commit()
    {
        final ContainerResponse current = response;
        if (current != null)
        {
            if (HttpMethod.HEAD.equals(requestMethodName) && current.hasEntity())
            {
                // for testing purposes:
                // need to also strip the object entity as it was stripped when writing to output
                current.setEntity(null);
            }
            requestTimeoutHandler.close();
        }
    }

    @Override
    public void failure(final Throwable error)
    {
        requestTimeoutHandler.close();
    }

    @Override
    public boolean enableResponseBuffering()
    {
        return true;
    }

    private String getHeaderValue(List<Object> values)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object o : values)
        {
            if (!first)
            {
                sb.append(", ");
            }
            else
            {
                first = false;
            }

            sb.append(o);
        }

        return sb.toString();
    }

    DeferredForwardOutputStream getOutputStream()
    {
        return outputStream;
    }
}
