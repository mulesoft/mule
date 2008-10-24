/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
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
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.util.expression.ExpressionEvaluatorManager;
import org.mule.util.expression.MuleContextExpressionEvaluator;

public class MuleContextExpressionEvaluatorTestCase extends AbstractMuleTestCase
{
    @Override
    protected void doSetUp() throws Exception
    {
        MuleEvent event = getTestEvent("testing",
                getTestService("apple", Apple.class),
                getTestInboundEndpoint("test", "test://foo"));
        RequestContext.setEvent(event);
    }

    public void testExpressions() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test");
        MuleContextExpressionEvaluator extractor = new MuleContextExpressionEvaluator();

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

    public void testExpressionsFromExtractorManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test");
        Object o = ExpressionEvaluatorManager.evaluate("mule:serviceName", message);
        assertEquals("apple", o);

        o = ExpressionEvaluatorManager.evaluate("mule:modelName", message);
        assertNotNull(o);

        o = ExpressionEvaluatorManager.evaluate("mule:inboundEndpoint", message);
        assertEquals("test://foo", o.toString());

        o = ExpressionEvaluatorManager.evaluate("mule:serverId", message);
        assertNotNull(o);

        o = ExpressionEvaluatorManager.evaluate("mule:clusterId", message);
        assertNotNull(o);

        o = ExpressionEvaluatorManager.evaluate("mule:domainId", message);
        assertNotNull(o);

        o = ExpressionEvaluatorManager.evaluate("mule:workingDir", message);
        assertNotNull(o);

        try
        {
            o = ExpressionEvaluatorManager.evaluate("mule:bork", message);
            fail("bork is not a valid mule context value");
        }
        catch (Exception e)
        {
            //expected
        }
    }

    public void testMissingEventContext() throws Exception
    {
        RequestContext.clear();
        
        MuleMessage message = new DefaultMuleMessage("test");
        MuleContextExpressionEvaluator extractor = new MuleContextExpressionEvaluator();

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