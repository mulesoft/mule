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
import org.mule.routing.outbound.MultiExpressionMessageSplitter;
import org.mule.util.expression.FunctionExpressionEvaluator;
import org.mule.util.expression.ExpressionEvaluatorManager;
import org.mule.api.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Date;

public class FunctionExpressionEvaluatorTestCase extends AbstractMuleTestCase
{
    public void testFunctions() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(new Apple());
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
            o = extractor.evaluate("bork", message);
            fail("bork is not a valid function");
        }
        catch (Exception e)
        {
            //expected
        }
    }

    public void testFunctionsFromExtractorManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(new Apple());
        Object o = ExpressionEvaluatorManager.evaluate("function:uuid", message);
        assertNotNull(o);
        o = ExpressionEvaluatorManager.evaluate("function:now", message);
        assertNotNull(o);
        assertTrue(o instanceof Timestamp);

        o = ExpressionEvaluatorManager.evaluate("function:date", message);
        assertNotNull(o);
        assertTrue(o instanceof Date);

        o = ExpressionEvaluatorManager.evaluate("function:hostname", message);
        assertNotNull(o);
        assertEquals(InetAddress.getLocalHost().getHostName(), o);

        o = ExpressionEvaluatorManager.evaluate("function:ip", message);
        assertNotNull(o);
        assertEquals(InetAddress.getLocalHost().getHostAddress(), o);

        o = ExpressionEvaluatorManager.evaluate("function:payloadClass", message);
        assertNotNull(o);
        assertEquals(Apple.class.getName(), o);

        o = ExpressionEvaluatorManager.evaluate("function:shortPayloadClass", message);
        assertNotNull(o);
        assertEquals("Apple", o);

        try
        {
            o = ExpressionEvaluatorManager.evaluate("function:bork", message);
            fail("bork is not a valid function");
        }
        catch (Exception e)
        {
            //expected
        }
    }
}
