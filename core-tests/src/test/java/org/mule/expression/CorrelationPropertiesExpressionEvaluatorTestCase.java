/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.routing.correlation.CorrelationPropertiesExpressionEvaluator;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.UUID;

import java.util.HashMap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CorrelationPropertiesExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{
    private HashMap<String, Object> messageProperties;
    private CorrelationPropertiesExpressionEvaluator evaluator = new CorrelationPropertiesExpressionEvaluator();
    private DefaultMuleMessage message;

    public CorrelationPropertiesExpressionEvaluatorTestCase()
    {
        super();
        setDisposeContextPerClass(true);
    }

    @Override
    public void doSetUp()
    {
        messageProperties = new HashMap<String, Object>(3);
        messageProperties.put("foo", "foovalue");
        messageProperties.put("bar", "barvalue");
        messageProperties.put("baz", "bazvalue");

        message = new DefaultMuleMessage(TEST_MESSAGE, messageProperties, muleContext);
    }

    @Test
    public void testCorrelationManagerCorrelationId()
    {
        String correlationId = UUID.getUUID();
        message.setCorrelationId(correlationId);

        Object result = evaluator.evaluate(MuleProperties.MULE_CORRELATION_ID_PROPERTY, message);
        assertNotNull(result);
        assertEquals(correlationId, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCorrelationManagerNullResult()
    {
        message.setUniqueId(null);
        evaluator.evaluate(MuleProperties.MULE_CORRELATION_ID_PROPERTY, message);
    }

    @Test
    public void testCorrelationManagerUniqueId()
    {
        Object result = evaluator.evaluate(MuleProperties.MULE_MESSAGE_ID_PROPERTY, message);
        assertNotNull(result);
        assertEquals(message.getUniqueId(), result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCorrelationManagerInvalidKey()
    {
        evaluator.evaluate("invalid-key", message);
    }
}
