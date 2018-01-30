/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import org.mule.api.MuleEvent;
import org.mule.util.IOUtils;

import java.io.IOException;

import javax.activation.DataHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Test;

public class HttpRequestMultipartRelatedTestCase extends AbstractHttpRequestTestCase
{

    private static final String BOUNDARY = "bec89590-35fe-11e5-a966-de100cec9c0d";
    private static final String MULTIPART_FORMAT = "--%1$s\r\n Content-Type: text/plain \r\nContent-ID: %2$s\n\r\ntest\r\n--%1$s--\r\n";
    private static final String CONTENT_TYPE_VALUE = format("multipart/related; boundary=%s", BOUNDARY);
    private static final String CONTENT_ID_VALUE = "<test-content-id>";

    @Override
    protected String getConfigFile()
    {
        return "http-request-multipart-config.xml";
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.setHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE);
        response.setStatus(SC_OK);
        response.getWriter().print(format(MULTIPART_FORMAT, BOUNDARY, CONTENT_ID_VALUE));
        baseRequest.setHandled(true);
    }

    @Test
    public void testMultipartRelated() throws Exception
    {
        MuleEvent testEvent = getTestEvent(null);
        testEvent.getMessage().setInvocationProperty("requestPath", "/");
        MuleEvent response = runFlow("requestFlow", testEvent);
        DataHandler attachment = response.getMessage().getInboundAttachment(CONTENT_ID_VALUE);
        assertThat(IOUtils.toString(attachment.getDataSource().getInputStream()), is("test"));
        assertThat(response.getMessage().getInboundProperty("content-type").toString(), containsString(CONTENT_TYPE_VALUE));
    }

}
