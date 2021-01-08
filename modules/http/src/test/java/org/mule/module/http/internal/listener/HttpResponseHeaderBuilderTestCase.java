/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class HttpResponseHeaderBuilderTestCase {

    @Test
    public void testNullHeaderValueIsIgnored()
    {
        HttpResponseHeaderBuilder httpResponseHeaderBuilder = new HttpResponseHeaderBuilder();
        httpResponseHeaderBuilder.addHeader("header", null);
        assertTrue(httpResponseHeaderBuilder.getHeaderNames().isEmpty());
    }
}
