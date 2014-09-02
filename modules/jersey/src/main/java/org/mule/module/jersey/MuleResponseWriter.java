/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import org.mule.api.MuleMessage;
import org.mule.transport.http.HttpConnector;

import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MuleResponseWriter implements ContainerResponseWriter
{

    private final MuleMessage message;

    private OutputStream outputStream;
    private boolean headersWritten = false;

    public MuleResponseWriter(MuleMessage request)
    {
        super();
        this.message = request;
    }

    public OutputStream writeStatusAndHeaders(long x, ContainerResponse response) throws IOException
    {
        if (!headersWritten)
        {
            Map<String, String> customHeaders = new HashMap<String, String>();
            for (Map.Entry<String, List<Object>> e : response.getHttpHeaders().entrySet())
            {
                // TODO: is this correct?
                message.setOutboundProperty(e.getKey(), getHeaderValue(e.getValue()));
            }

            message.setInvocationProperty(JerseyResourcesComponent.JERSEY_RESPONSE, response);
            message.setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, response.getStatus());

            headersWritten = true;
        }

        return outputStream;
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

            sb.append(ContainerResponse.getHeaderValue(o));
        }
        return sb.toString();
    }

    public void finish() throws IOException
    {
    }

    public void setOutputStream(OutputStream outputStream)
    {
        this.outputStream = outputStream;
    }
}
