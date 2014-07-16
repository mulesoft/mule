/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static junit.framework.Assert.assertEquals;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Map;

import org.junit.Test;

public class HttpThrottlingHeadersTestCase extends AbstractMuleTestCase
{

    public static final int HEADERS_COUNT = 3;
    public static final int REMAINING_ITEMS = 5;
    public static final int TOTAL_ITEMS = 50;
    public static final int RESET_TIME = 60000;

    @Test
    public void ensureCorrectThrottlingHeadersMappings()
    {
        HttpThrottlingHeadersMapBuilder builder = new HttpThrottlingHeadersMapBuilder();
        builder.setThrottlingPolicyStatistics(REMAINING_ITEMS, TOTAL_ITEMS, RESET_TIME);
        Map<String, String> headersMap = builder.build();
        assertEquals(HEADERS_COUNT, headersMap.size());
        assertEquals(String.valueOf(REMAINING_ITEMS), headersMap.get(HttpMessageProcessTemplate.X_RATE_LIMIT_REMAINING_HEADER));
        assertEquals(String.valueOf(TOTAL_ITEMS), headersMap.get(HttpMessageProcessTemplate.X_RATE_LIMIT_LIMIT_HEADER));
        assertEquals(String.valueOf(RESET_TIME), headersMap.get(HttpMessageProcessTemplate.X_RATE_LIMIT_RESET_HEADER));
    }

}
