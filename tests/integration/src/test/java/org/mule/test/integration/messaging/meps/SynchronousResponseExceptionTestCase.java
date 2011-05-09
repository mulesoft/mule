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

import org.mule.tck.FunctionalTestCase;

/**
 * @see MULE-4512
 */
public class SynchronousResponseExceptionTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/messaging/meps/synchronous-response-exception.xml";
    }

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


