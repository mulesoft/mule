/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Test;

public class HttpRequestMultipartTestCase extends AbstractHttpRequestTestCase
{

    private static final String BOUNDARY =  "bec89590-35fe-11e5-a966-de100cec9c0d";
    private static final String CONTENT_TYPE_VALUE = String.format("multipart/form-data; boundary=%s", BOUNDARY);
    private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition: form-data; name=\"partName\"\r\n";
    private static final String MULTIPART_FORMAT = "--%1$s\r\n %2$sContent-Type: text/plain\n\r\ntest\r\n--%1$s--\r\n";
    private static final String CONTENT_DISPOSITION_PATH = "/contentDisposition";
    private static final String NO_CONTENT_DISPOSITION = "/noContentDisposition";
    private static final String MULTIPART_BODY =
      "--" + BOUNDARY + "\r\n"
      + "Content-Disposition: form-data; name=\"another\"\r\n"
      + "Content-Type: text/plain\r\n"
      + "Content-Transfer-Encoding: binary\r\n"
      + "\r\n"
      + "no\r\n"
      + "--" + BOUNDARY + "\r\n"
      + "Content-Disposition: form-data; name=\"field1\"\r\n"
      + "Content-Type: text/plain\r\n"
      + "Content-Transfer-Encoding: binary\r\n"
      + "\r\n"
      + "yes\r\n"
      + "--" + BOUNDARY + "--\r\n";


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
        extractBaseRequestParts(baseRequest);
        String contentDispositionHeader;
        String path = baseRequest.getUri().getPath();
        if (path.equals(CONTENT_DISPOSITION_PATH))
        {
            contentDispositionHeader = CONTENT_DISPOSITION_HEADER;
            response.getWriter().print(String.format(MULTIPART_FORMAT, BOUNDARY, contentDispositionHeader));
        }
        else if (path.equals(NO_CONTENT_DISPOSITION))
        {
            contentDispositionHeader = "";
            response.getWriter().print(String.format(MULTIPART_FORMAT, BOUNDARY, contentDispositionHeader));
        } else {
            response.getWriter().print(MULTIPART_BODY);
        }
        baseRequest.setHandled(true);
    }

    @Test
    public void getMultipartContentWithContentDisposition() throws Exception
    {
        testWithPath(CONTENT_DISPOSITION_PATH);
    }

    @Test
    public void getMultipartContentWithoutContentDisposition() throws Exception
    {
        testWithPath(NO_CONTENT_DISPOSITION);
    }

    @Test
    public void sendAndReceiveMultipleParts() throws Exception {
        MuleEvent testEvent = getTestEvent(null);
        testEvent.getMessage().setInvocationProperty("requestPath", "/");
        testEvent.getMessage().setOutboundProperty(CONTENT_TYPE, CONTENT_TYPE_VALUE);
        MuleMessage result = runFlow("multiple", testEvent).getMessage();

        assertThat(body, is(MULTIPART_BODY));

        Set<String> inboundAttachmentNames = result.getInboundAttachmentNames();
        assertThat(inboundAttachmentNames, hasSize(2));
        Iterator<String> iterator = inboundAttachmentNames.iterator();
        assertThat(iterator.next(), is("another"));
        assertThat(iterator.next(), is("field1"));
    }

    private void testWithPath(String path) throws Exception
    {
        MuleEvent testEvent = getTestEvent(null);
        testEvent.getMessage().setInvocationProperty("requestPath", path);
        MuleEvent response = runFlow("requestFlow", testEvent);
        assertThat(response.getMessage().getInboundAttachmentNames().size(), is(1));
    }
}
