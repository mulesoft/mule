/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
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

    public HttpMultipartMuleMessageFactory(MuleContext context)
    {
        super(context);
    }

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
            if (body == null)
            {
                throw new IllegalArgumentException("no part named \"payload\" found");
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


