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

import java.sql.Timestamp;

/** TODO */
public class PropertyExtractorManagerTestCase extends AbstractMuleTestCase
{
    public void testManager() throws Exception
    {
        ExpressionEvaluatorManager.setDefaultEvaluator(FunctionExpressionEvaluator.NAME);

        MuleMessage message = new DefaultMuleMessage("test");
        Object o = ExpressionEvaluatorManager.evaluate("uuid", message);
        assertNotNull(o);
        o = ExpressionEvaluatorManager.evaluate("now", message);
        assertNotNull(o);
        assertTrue(o instanceof Timestamp);
    }

    public void testDefaultExtractor() throws Exception
    {
        assertEquals(MessageHeaderExpressionEvaluator.NAME, ExpressionEvaluatorManager.getDefaultEvaluator());
        ExpressionEvaluatorManager.setDefaultEvaluator(MapPayloadExpressionEvaluator.NAME);

        assertEquals(MapPayloadExpressionEvaluator.NAME, ExpressionEvaluatorManager.getDefaultEvaluator());
        try
        {
            ExpressionEvaluatorManager.setDefaultEvaluator("bork");
            fail("bork is not a valid property extractor");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }
        assertEquals(MapPayloadExpressionEvaluator.NAME, ExpressionEvaluatorManager.getDefaultEvaluator());
        //Lets remove the default
        ExpressionEvaluatorManager.unregisterEvaluator(MapPayloadExpressionEvaluator.NAME);
        assertEquals(ExpressionEvaluatorManager.DEFAULT_EVALUATOR_NAME, ExpressionEvaluatorManager.getDefaultEvaluator());

        try
        {
            ExpressionEvaluatorManager.setDefaultEvaluator(MapPayloadExpressionEvaluator.NAME);
            fail("Map extractor should no longer be registered");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }
    }

    public void testRegistration() throws Exception
    {
        try
        {
            ExpressionEvaluatorManager.registerEvaluator(new MapPayloadExpressionEvaluator());
            fail("extractor already exists");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }

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
}
