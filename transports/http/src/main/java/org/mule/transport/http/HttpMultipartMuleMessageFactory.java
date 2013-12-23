/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.mule.DefaultMuleMessage;
import org.mule.transport.http.multipart.MultiPartInputStream;
import org.mule.transport.http.multipart.Part;
import org.mule.transport.http.multipart.PartDataSource;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.activation.DataHandler;

public class HttpMultipartMuleMessageFactory extends HttpMuleMessageFactory
{

    private Collection<Part> parts;

    @Override
    protected Object extractPayloadFromHttpRequest(HttpRequest httpRequest) throws IOException
    {
        Object body = null;

        if (httpRequest.getContentType().contains("multipart/form-data"))
        {
            MultiPartInputStream in = new MultiPartInputStream(httpRequest.getBody(), httpRequest.getContentType(), null);

            // We need to store this so that the headers for the part can be read
            parts = in.getParts();
            for (Part part : parts)
            {
                if (part.getName().equals("payload"))
                {
                    body = part.getInputStream();
                    break;
                }
            }
        }
        else
        {
            body = super.extractPayloadFromHttpRequest(httpRequest);
        }

        return body;
    }

    @Override
    protected void addAttachments(DefaultMuleMessage message, Object transportMessage) throws Exception
    {
        if (parts != null)
        {
            try
            {
                for (Part part : parts)
                {
                    if (!part.getName().equals("payload"))
                    {
                        message.addInboundAttachment(part.getName(), new DataHandler(new PartDataSource(part)));
                    }
                }
            }
            finally
            {
                // Attachments are the last thing to get processed
                parts.clear();
                parts = null;
            }
        }
    }

    @Override
    protected void convertMultiPartHeaders(Map<String, Object> headers)
    {
        if (parts != null)
        {
            for (Part part : parts)
            {
                if (part.getName().equals("payload"))
                {
                    for (String name : part.getHeaderNames())
                    {
                        headers.put(name, part.getHeader(name));
                    }
                    break;
                }
            }

        }

    }

}


