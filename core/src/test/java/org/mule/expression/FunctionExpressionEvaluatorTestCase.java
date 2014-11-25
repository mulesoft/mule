/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.util.NetworkUtils;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.Test;

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
        assertEquals(NetworkUtils.getLocalHost().getHostName(), o);

        o = extractor.evaluate("ip", message);
        assertNotNull(o);
        assertEquals(NetworkUtils.getLocalHost().getHostAddress(), o);

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
        assertEquals(NetworkUtils.getLocalHost().getHostName(), o);

        o = muleContext.getExpressionManager().evaluate("function:ip", message);
        assertNotNull(o);
        assertEquals(NetworkUtils.getLocalHost().getHostAddress(), o);

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
