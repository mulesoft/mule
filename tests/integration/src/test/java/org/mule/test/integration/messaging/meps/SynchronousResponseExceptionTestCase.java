/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.messaging.meps;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.tck.AbstractServiceAndFlowTestCase;

/**
 * @see MULE-4512
 */
public class SynchronousResponseExceptionTestCase extends AbstractServiceAndFlowTestCase
{
    public SynchronousResponseExceptionTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/messaging/meps/synchronous-response-exception-service.xml"},
            {ConfigVariant.FLOW,
                "org/mule/test/integration/messaging/meps/synchronous-response-exception-flow.xml"}});
    }

    @Test
    public void testComponentException() throws Exception
    {
        try
        {
            muleContext.getClient().send("vm://in1", "request", null);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    @Test
    public void testOutboundRoutingException() throws Exception
    {
        try
        {
            muleContext.getClient().send("vm://in2", "request", null);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    @Test
    public void testInboundTransformerException() throws Exception
    {
        try
        {
            muleContext.getClient().send("vm://in3", "request", null);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    @Test
    public void testOutboundTransformerException() throws Exception
    {
        try
        {
            muleContext.getClient().send("vm://in4", "request", null);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    @Test
    public void testResponseTransformerException() throws Exception
    {
        try
        {
            muleContext.getClient().send("vm://in5", "request", null);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            // expected
        }
    }
}
