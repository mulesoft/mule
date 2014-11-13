/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.MultiPartInputStreamParser;
import org.junit.Test;

public class HttpRequestOutboundAttachmentsTestCase extends AbstractHttpRequestTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "http-request-outbound-attachments-config.xml";
    }

    private Collection<Part> parts;
    private String requestContentType;

    @Test
    public void payloadIsIgnoredWhenSendingOutboundAttachments() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("requestFlow");
        MuleEvent event = getTestEvent(TEST_MESSAGE);

        event.getMessage().addOutboundAttachment("attachment1", "Contents 1", "text/plain");
        event.getMessage().addOutboundAttachment("attachment2", "Contents 2", "text/html");

        flow.process(event);

        assertThat(requestContentType, startsWith("multipart/form-data; boundary="));
        assertThat(parts.size(), equalTo(2));

        assertPart("attachment1", "text/plain", "Contents 1");
        assertPart("attachment2", "text/html", "Contents 2");
    }

    @Test
    public void outboundAttachmentsCustomContentType() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("requestFlow");
        MuleEvent event = getTestEvent(TEST_MESSAGE);

        event.getMessage().addOutboundAttachment("attachment1", "Contents 1", "text/plain");
        event.getMessage().addOutboundAttachment("attachment2", "Contents 2", "text/html");
        event.getMessage().setOutboundProperty("Content-Type", "multipart/form-data2");
        flow.process(event);

        assertThat(requestContentType, startsWith("multipart/form-data2; boundary="));
        assertThat(parts.size(), equalTo(2));

        assertPart("attachment1", "text/plain", "Contents 1");
        assertPart("attachment2", "text/html", "Contents 2");
    }

    private void assertPart(String name, String expectedContentType, String expectedBody) throws Exception
    {
        Part part = getPart(name);
        assertThat(part, notNullValue());
        assertThat(part.getContentType(), startsWith(expectedContentType));
        assertThat(IOUtils.toString(part.getInputStream()), equalTo(expectedBody));
    }

    private Part getPart(String name)
    {
        for (Part part : parts)
        {
            if (part.getName().equals(name))
            {
                return part;
            }
        }
        return null;
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        requestContentType = request.getHeader("Content-Type");

        MultiPartInputStreamParser inputStreamParser = new MultiPartInputStreamParser(request.getInputStream(), request.getContentType(), null, null);

        try
        {
            parts = inputStreamParser.getParts();
        }
        catch (ServletException e)
        {
            throw new IOException(e);
        }


        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print(DEFAULT_RESPONSE);
    }
}
