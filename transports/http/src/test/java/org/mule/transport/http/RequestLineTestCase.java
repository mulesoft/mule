/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.apache.commons.httpclient.HttpVersion;
import org.junit.Test;

public class RequestLineTestCase extends AbstractMuleTestCase
{

    @Test
    public void getWithoutParamsWithNoParams()
    {
        RequestLine requestLine = new RequestLine("GET", "/server/order", HttpVersion.HTTP_1_1);
        assertThat(requestLine.getUrlWithoutParams(), is("/server/order"));
    }

    @Test
    public void getWithoutParamsWithParams()
    {
        RequestLine requestLine = new RequestLine("GET", "/server/order?param1=value1", HttpVersion.HTTP_1_1);
        assertThat(requestLine.getUrlWithoutParams(), is("/server/order"));
    }

    @Test
    public void getWithoutParamsWithParamsInRootPath()
    {
        RequestLine requestLine = new RequestLine("GET", "/?param1=value1", HttpVersion.HTTP_1_1);
        assertThat(requestLine.getUrlWithoutParams(), is("/"));
    }
}
