/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.util;

import static junit.framework.Assert.assertEquals;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.StringUtils;

import org.junit.Rule;
import org.junit.Test;

public class HttpUtilEchoTestCase extends FunctionalTestCase
{

    private static final String SMALL_PAYLOAD = "This is a test.";
    private static final String BIG_PAYLOAD = StringUtils.repeat(SMALL_PAYLOAD, 100);

    private final HttpUtil util = new HttpUtilImpl();

    @Rule
    public DynamicPort port = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "http-util-config.xml";
    }

    @Test
    public void testSmallPayload() throws Exception
    {
        postAndAssert(SMALL_PAYLOAD);
    }

    @Test
    public void testBigPayload() throws Exception
    {
        postAndAssert(BIG_PAYLOAD);
    }

    private void postAndAssert(String payload)
    {
        assertEquals(payload, util.post(String.format("http://localhost:%d/echo", port.getNumber()), payload));
    }

}
