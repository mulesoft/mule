/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Date;

public class FunctionPropertyExtractorTestCase extends AbstractMuleTestCase
{
    public void testFunctions() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test");
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
        ExpressionEvaluatorManager.setDefaultEvaluator(FunctionExpressionEvaluator.NAME);
        
        MuleMessage message = new DefaultMuleMessage("test");
        Object o = ExpressionEvaluatorManager.evaluate("uuid", message);
        assertNotNull(o);
        o = ExpressionEvaluatorManager.evaluate("now", message);
        assertNotNull(o);
        assertTrue(o instanceof Timestamp);

        o = ExpressionEvaluatorManager.evaluate("date", message);
        assertNotNull(o);
        assertTrue(o instanceof Date);

        o = ExpressionEvaluatorManager.evaluate("hostname", message);
        assertNotNull(o);
        assertEquals(InetAddress.getLocalHost().getHostName(), o);

        o = ExpressionEvaluatorManager.evaluate("ip", message);
        assertNotNull(o);
        assertEquals(InetAddress.getLocalHost().getHostAddress(), o);

        try
        {
            o = ExpressionEvaluatorManager.evaluate("bork", message);
            fail("bork is not a valid function");
        }
        catch (Exception e)
        {
            //expected
        }
    }
}
