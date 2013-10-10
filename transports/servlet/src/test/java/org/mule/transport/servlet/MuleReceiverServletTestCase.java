/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MuleReceiverServletTestCase extends AbstractMuleTestCase
{
    private static final String KEY = "key";
    private MuleContext mockContext = mock(MuleContext.class);

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
