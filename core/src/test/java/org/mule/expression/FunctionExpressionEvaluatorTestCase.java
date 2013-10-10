/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Date;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FunctionExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testFunctions() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(new Apple(), muleContext);
        FunctionExpressionEvaluator extractor = new FunctionExpressionEvaluator();
        Object o = extractor.evaluate("uuid", message);
        assertNotNull(o);
        o = extractor.evaluate("now", message);
        assertNotNull(o);
        assertTrue(o instanceof Timestamp);

        o = extractor.evaluate("date", message);
        assertNotNull(o);
        assertTrue(o instanceof Date);

        o = extractor.evaluate("hostname", message);
        assertNotNull(o);
        assertEquals(InetAddress.getLocalHost().getHostName(), o);

        o = extractor.evaluate("ip", message);
        assertNotNull(o);
        assertEquals(InetAddress.getLocalHost().getHostAddress(), o);

        o = extractor.evaluate("payloadClass", message);
        assertNotNull(o);
        assertEquals(Apple.class.getName(), o);

        o = extractor.evaluate("shortPayloadClass", message);
        assertNotNull(o);
        assertEquals("Apple", o);

        try
        {
            extractor.evaluate("bork", message);
            fail("bork is not a valid function");
        }
        catch (IllegalArgumentException e)
        {
            //expected
        }
    }

    @Test
    public void testFunctionsFromExtractorManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(new Apple(), muleContext);
        Object o = muleContext.getExpressionManager().evaluate("function:uuid", message);
        assertNotNull(o);
        o = muleContext.getExpressionManager().evaluate("function:now", message);
        assertNotNull(o);
        assertTrue(o instanceof Timestamp);

        o = muleContext.getExpressionManager().evaluate("function:date", message);
        assertNotNull(o);
        assertTrue(o instanceof Date);

        o = muleContext.getExpressionManager().evaluate("function:hostname", message);
        assertNotNull(o);
        assertEquals(InetAddress.getLocalHost().getHostName(), o);

        o = muleContext.getExpressionManager().evaluate("function:ip", message);
        assertNotNull(o);
        assertEquals(InetAddress.getLocalHost().getHostAddress(), o);

        o = muleContext.getExpressionManager().evaluate("function:payloadClass", message);
        assertNotNull(o);
        assertEquals(Apple.class.getName(), o);

        o = muleContext.getExpressionManager().evaluate("function:shortPayloadClass", message);
        assertNotNull(o);
        assertEquals("Apple", o);

        try
        {
            muleContext.getExpressionManager().evaluate("function:bork", message);
            fail("bork is not a valid function");
        }
        catch (IllegalArgumentException e)
        {
            //expected
        }

        try
        {
            muleContext.getExpressionManager().evaluate("function:", message);
            fail("'Empty string' is not a valid function");
        }
        catch (IllegalArgumentException e)
        {
            //expected
        }
    }
}
