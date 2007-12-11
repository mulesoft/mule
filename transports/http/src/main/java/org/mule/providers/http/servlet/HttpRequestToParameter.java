/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.servlet;

import org.mule.transformers.AbstractDiscoverableTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.SystemUtils;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

public class HttpRequestToParameter extends AbstractDiscoverableTransformer
{

    public HttpRequestToParameter()
    {
        registerSourceType(HttpServletRequest.class);
        setReturnClass(String.class);
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        HttpServletRequest request = (HttpServletRequest)src;

        String payloadParam = (String)request.getAttribute(AbstractReceiverServlet.PAYLOAD_PARAMETER_NAME);
        if (null == payloadParam)
        {
            payloadParam = AbstractReceiverServlet.DEFAULT_PAYLOAD_PARAMETER_NAME;
        }

        String payload = request.getParameter(payloadParam);
        if (null == payload)
        {
            if (isText(request.getContentType()))
            {
                try
                {
                    BufferedReader reader = request.getReader();
                    StringBuffer buffer = new StringBuffer(8192);
                    String line = reader.readLine();
                    while (line != null)
                    {
                        buffer.append(line);
                        line = reader.readLine();
                        if (line != null) buffer.append(SystemUtils.LINE_SEPARATOR);
                    }
                    payload = buffer.toString();
                }
                catch (IOException e)
                {
                    throw new TransformerException(this, e);
                }
            }
        }

        return payload;
    }

    protected boolean isText(String contentType)
    {
        if (contentType == null)
        {
            return true;
        }
        return (contentType.startsWith("text/"));
    }

}
