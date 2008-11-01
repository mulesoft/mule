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
import org.mule.api.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Map;

public class ExpressionEvaluatorManagerTestCase extends AbstractMuleTestCase
{
    public void testManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", (Map) null);
        Object o = muleContext.getExpressionManager().evaluate("function:uuid", message);
        assertNotNull(o);
        o = muleContext.getExpressionManager().evaluate("function:now", message);
        assertNotNull(o);
        assertTrue(o instanceof Timestamp);
    }

    public void testRegistration() throws Exception
    {
        // http://mule.mulesource.org/jira/browse/MULE-3809 . For now ignore duplicate registrations.
        /*try
        {
            DefaultExpressionManager.registerEvaluator(new MapPayloadExpressionEvaluator());
            fail("extractor already exists");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }*/

        try
        {
            muleContext.getExpressionManager().registerEvaluator(null);
            fail("null extractor");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }
        assertNull(muleContext.getExpressionManager().unregisterEvaluator(null));

    }


    public void testValidator() throws Exception
    {
        // fail for old-style ${}
        assertFalse(muleContext.getExpressionManager().isValidExpression("http://${bean:user}:${bean:password}@${header:host}:${header:port}/foo/bar"));
        assertFalse(muleContext.getExpressionManager().isValidExpression("${bean:user}"));

        // wiggly mule style!
        assertTrue(muleContext.getExpressionManager().isValidExpression("#[bean:user]"));
        assertTrue(muleContext.getExpressionManager().isValidExpression("http://#[bean:user]:#[bean:password]@#[header:host]:#[header:port]/foo/bar"));

        assertFalse(muleContext.getExpressionManager().isValidExpression("{bean:user}"));
        assertFalse(muleContext.getExpressionManager().isValidExpression("#{bean:user"));
        assertFalse(muleContext.getExpressionManager().isValidExpression("user"));
    }

    public void testParsing()
    {
        MuleMessage msg = new DefaultMuleMessage("test", Collections.emptyMap());
        msg.setProperty("user", "vasya");
        msg.setProperty("password", "pupkin");
        msg.setProperty("host", "example.com");
        msg.setProperty("port", "12345");

        String result = muleContext.getExpressionManager().parse("http://#[header:user]:#[header:password]@#[header:host]:#[header:port]/foo/bar", msg);
        assertNotNull(result);
        assertEquals("http://vasya:pupkin@example.com:12345/foo/bar", result);
    }
}
