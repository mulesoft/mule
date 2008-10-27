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
import org.mule.util.expression.ExpressionEvaluatorManager;

import java.sql.Timestamp;
import java.util.Map;

public class ExpressionEvaluatorManagerTestCase extends AbstractMuleTestCase
{
    public void testManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", (Map) null);
        Object o = ExpressionEvaluatorManager.evaluate("function:uuid", message);
        assertNotNull(o);
        o = ExpressionEvaluatorManager.evaluate("function:now", message);
        assertNotNull(o);
        assertTrue(o instanceof Timestamp);
    }

    public void testRegistration() throws Exception
    {
        // http://mule.mulesource.org/jira/browse/MULE-3809 . For now ignore duplicate registrations.
        /*try
        {
            ExpressionEvaluatorManager.registerEvaluator(new MapPayloadExpressionEvaluator());
            fail("extractor already exists");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }*/

        try
        {
            ExpressionEvaluatorManager.registerEvaluator(null);
            fail("null extractor");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }
        assertNull(ExpressionEvaluatorManager.unregisterEvaluator(null));

    }


    public void testValidator() throws Exception
    {
        // fail for old-style ${}
        assertFalse(ExpressionEvaluatorManager.isValidExpression("http://${bean:user}:${bean:password}@${header:host}:${header:port}/foo/bar"));
        assertFalse(ExpressionEvaluatorManager.isValidExpression("${bean:user}"));

        // wiggly mule style!
        assertTrue(ExpressionEvaluatorManager.isValidExpression("#[bean:user]"));
        assertTrue(ExpressionEvaluatorManager.isValidExpression("http://#[bean:user]:#[bean:password]@#[header:host]:#[header:port]/foo/bar"));

        assertFalse(ExpressionEvaluatorManager.isValidExpression("{bean:user}"));
        assertFalse(ExpressionEvaluatorManager.isValidExpression("#{bean:user"));
        assertFalse(ExpressionEvaluatorManager.isValidExpression("user"));
    }
}
