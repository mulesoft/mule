/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ExceptionAfterAggregationTestCase extends FunctionalTestCase
{

    private final String configResources;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"exception-after-aggregation-test-config-simple.xml"},
                {"exception-after-aggregation-test-config.xml"}});
    }

    public ExceptionAfterAggregationTestCase(String configResources)
    {
        super();
        this.configResources = configResources;
    }

    protected String getConfigResources()
    {
        return configResources;
    }

    @Test
    public void testReceiveCorrectExceptionAfterAggregation() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://in", "some data", null);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        assertEquals("Ad hoc message exception", result.getExceptionPayload().getMessage());
    }
}
