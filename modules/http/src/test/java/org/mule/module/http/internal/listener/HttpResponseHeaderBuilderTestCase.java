package org.mule.module.http.internal.listener;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class HttpResponseHeaderBuilderTestCase {

    @Test
    public void testNullHeader()
    {
        HttpResponseHeaderBuilder httpResponseHeaderBuilder = new HttpResponseHeaderBuilder();
        httpResponseHeaderBuilder.addHeader("header", null);
        assertTrue(httpResponseHeaderBuilder.getHeaderNames().isEmpty());
    }
}
