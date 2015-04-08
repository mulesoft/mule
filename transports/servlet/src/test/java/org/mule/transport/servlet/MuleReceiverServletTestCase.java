/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.junit.Test;

public class MuleReceiverServletTestCase extends AbstractMuleTestCase
{
    private static final String KEY = "key";
    private MuleContext mockContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);

    @Test
    public void responseWithSingleValueForHeaderShouldWriteSingleValueToServletResponse() throws Exception
    {
        String headerValue = "value";

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.addHeader(new Header(KEY, headerValue));

        HttpServletResponse servletResponse = createServletResponseAndWriteResponse(httpResponse);
        verify(servletResponse).addHeader(KEY, headerValue);
    }

    @Test
    public void responseWithMultipleValuesForHeaderShouldWriteMultipleValuesToServletResponse() throws Exception
    {
        String firstValue = "value1";
        String secondValue = "value2";

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.addHeader(new Header(KEY, firstValue));
        httpResponse.addHeader(new Header(KEY, secondValue));

        HttpServletResponse servletResponse = createServletResponseAndWriteResponse(httpResponse);
        verify(servletResponse).addHeader(KEY, firstValue);
        verify(servletResponse).addHeader(KEY, secondValue);
    }

    @Test
    public void responseWithoutContentTypeHeaderShouldGetDefaultContentType() throws Exception
    {
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.removeHeaders(HttpConstants.HEADER_CONTENT_TYPE);

        HttpServletResponse servletResponse = createServletResponseAndWriteResponse(httpResponse);
        verify(servletResponse).setContentType(HttpConstants.DEFAULT_CONTENT_TYPE);
    }

    @Test
    public void responseWithExistingContentTypeHeaderShouldPreserve() throws Exception
    {
        String contentType = "foo/bar";

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.addHeader(new Header(HttpConstants.HEADER_CONTENT_TYPE, contentType));

        HttpServletResponse servletResponse = createServletResponseAndWriteResponse(httpResponse);
        verify(servletResponse).setContentType(contentType);
    }

    private HttpServletResponse createServletResponseAndWriteResponse(HttpResponse httpResponse) throws Exception
    {
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);

        TestReceiverServlet testServlet = new TestReceiverServlet();
        testServlet.writeResponse(servletResponse, new DefaultMuleMessage(httpResponse, mockContext));

        return servletResponse;
    }

    private static class TestReceiverServlet extends AbstractReceiverServlet
    {
        // no custom methods
    }
}
