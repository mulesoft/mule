/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transport.http.HttpResponse;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MultipleResponseHeadersTestCase extends AbstractMuleTestCase
{
    private static final String KEY = "key";

    @Test
    public void responseWithSingleValueForHeaderShouldWriteSingleValueToServletResponse()
    {
        String headerValue = "value";

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.addHeader(new Header(KEY, headerValue));

        HttpServletResponse servletResponse = mock(HttpServletResponse.class);

        TestReceiverServlet testServlet = new TestReceiverServlet();
        testServlet.setHttpHeadersOnServletResponse(httpResponse, servletResponse);

        verify(servletResponse).addHeader(KEY, headerValue);
    }

    @Test
    public void responseWithMultipleValuesForHeaderShouldWriteMultipleValuesToServletResponse()
    {
        String firstValue = "value1";
        String secondValue = "value2";

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.addHeader(new Header(KEY, firstValue));
        httpResponse.addHeader(new Header(KEY, secondValue));

        HttpServletResponse servletResponse = mock(HttpServletResponse.class);

        TestReceiverServlet testServlet = new TestReceiverServlet();
        testServlet.setHttpHeadersOnServletResponse(httpResponse, servletResponse);

        verify(servletResponse).addHeader(KEY, firstValue);
        verify(servletResponse).addHeader(KEY, secondValue);
    }

    private static class TestReceiverServlet extends AbstractReceiverServlet
    {
        // no custom methods
    }
}
