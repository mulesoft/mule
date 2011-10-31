/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.routing.correlation.CorrelationPropertiesExpressionEvaluator;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.UUID;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class HeadersExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{
    private Map<String, Object> props;

    public HeadersExpressionEvaluatorTestCase()
    {
        super();
        setDisposeContextPerClass(true);
    }

    @Override
    public void doSetUp()
    {
        props = new HashMap<String, Object>(3);
        props.put("foo", "foovalue");
        props.put("bar", "barvalue");
        props.put("baz", "bazvalue");
    }



    @Test
    public void testCorrelationManagerCorrelationId()
    {
        CorrelationPropertiesExpressionEvaluator evaluator = new CorrelationPropertiesExpressionEvaluator();
        String correlationId = UUID.getUUID();

        MuleMessage message = new DefaultMuleMessage("test", props, muleContext);
        message.setCorrelationId(correlationId);

        Object result = evaluator.evaluate(MuleProperties.MULE_CORRELATION_ID_PROPERTY, message);
        assertNotNull(result);
        assertEquals(correlationId, result);
    }

    @Test
    public void testCorrelationManagerNullResult()
    {
        CorrelationPropertiesExpressionEvaluator evaluator = new CorrelationPropertiesExpressionEvaluator();

        DefaultMuleMessage message = new DefaultMuleMessage("test", props, muleContext);
        message.setUniqueId(null);

        try
        {
            evaluator.evaluate(MuleProperties.MULE_CORRELATION_ID_PROPERTY, message);
            fail("Null result on CorrelationPropertiesExpressionEvaluator must throw");
        }
        catch (IllegalArgumentException iae)
        {
            // this one was expected
        }
    }

    @Test
    public void testCorrelationManagerUniqueId()
    {
        CorrelationPropertiesExpressionEvaluator evaluator = new CorrelationPropertiesExpressionEvaluator();

        MuleMessage message = new DefaultMuleMessage("test", props, muleContext);
        Object result = evaluator.evaluate(MuleProperties.MULE_MESSAGE_ID_PROPERTY, message);
        assertNotNull(result);
        assertEquals(message.getUniqueId(), result);
    }

//    @Test
//    public void testCorrelationManagerNullInput()
//    {
//        CorrelationPropertiesExpressionEvaluator evaluator = new CorrelationPropertiesExpressionEvaluator();
//        evaluator.evaluate(MuleProperties.MULE_CORRELATION_ID_PROPERTY, null);
//    }

    @Test
    public void testCorrelationManagerInvalidKey()
    {
        CorrelationPropertiesExpressionEvaluator evaluator = new CorrelationPropertiesExpressionEvaluator();

        MuleMessage message = new DefaultMuleMessage("test", props, muleContext);
        try
        {
            evaluator.evaluate("invalid-key", message);
            fail("invalid key on CorrelationPropertiesExpressionEvaluator must fail");
        }
        catch (IllegalArgumentException iax)
        {
            // this one was expected
        }
    }
}
