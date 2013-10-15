/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class MuleContextExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{
    @Override
    protected void doSetUp() throws Exception
    {
        MuleEvent event = getTestEvent("testing",
                getTestService("apple", Apple.class),
                getTestInboundEndpoint("test", "test://foo"));
        RequestContext.setEvent(event);
    }

    @Test
    public void testExpressions() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        MuleContextExpressionEvaluator extractor = new MuleContextExpressionEvaluator();
        extractor.setMuleContext(muleContext);

        Object o = extractor.evaluate("serviceName", message);
        assertEquals("apple", o);

        o = extractor.evaluate("modelName", message);
        assertNotNull(o);

        o = extractor.evaluate("inboundEndpoint", message);
        assertEquals("test://foo", o.toString());

        o = extractor.evaluate("serverId", message);
        assertNotNull(o);

        o = extractor.evaluate("clusterId", message);
        assertNotNull(o);

        o = extractor.evaluate("domainId", message);
        assertNotNull(o);

        o = extractor.evaluate("workingDir", message);
        assertNotNull(o);

        try
        {
            o = extractor.evaluate("bork", message);
            fail("bork is not a valid mule context value");
        }
        catch (Exception e)
        {
            //expected
        }
    }

    @Test
    public void testExpressionsFromExtractorManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        Object o = muleContext.getExpressionManager().evaluate("context:serviceName", message);
        assertEquals("apple", o);

        o = muleContext.getExpressionManager().evaluate("context:modelName", message);
        assertNotNull(o);

        o = muleContext.getExpressionManager().evaluate("context:inboundEndpoint", message);
        assertEquals("test://foo", o.toString());

        o = muleContext.getExpressionManager().evaluate("context:serverId", message);
        assertNotNull(o);

        o = muleContext.getExpressionManager().evaluate("context:clusterId", message);
        assertNotNull(o);

        o = muleContext.getExpressionManager().evaluate("context:domainId", message);
        assertNotNull(o);

        o = muleContext.getExpressionManager().evaluate("context:workingDir", message);
        assertNotNull(o);

        try
        {
            o = muleContext.getExpressionManager().evaluate("context:bork", message);
            fail("bork is not a valid mule context value");
        }
        catch (Exception e)
        {
            //expected
        }
    }

    @Test
    public void testMissingEventContext() throws Exception
    {
        RequestContext.clear();
        
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        MuleContextExpressionEvaluator extractor = new MuleContextExpressionEvaluator();
        extractor.setMuleContext(muleContext);

        Object o = extractor.evaluate("serverId", message);
        assertNotNull(o);

        try
        {
            o = extractor.evaluate("serviceName", message);
            fail("There is no current event context");
        }
        catch (MuleRuntimeException e)
        {
            //expected
        }
    }
}
