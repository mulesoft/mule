/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.expression.RequiredValueException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class MapPayloadExpressionEvaluatorTestCase extends AbstractMuleTestCase
{
    private MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    private Map<String, String> messageProperties = new HashMap<String, String>(3);
    private MapPayloadExpressionEvaluator evaluator = new MapPayloadExpressionEvaluator();
    private MuleMessage message;

    @Before
    public void createMessagePropertiesAndMuleMessage() throws Exception
    {
        messageProperties.put("foo", "moo");
        messageProperties.put("bar", "mar");
        messageProperties.put("ba?z", "maz");

        message = new DefaultMuleMessage(messageProperties, muleContext);
    }

    @Test
    public void requiredKeyWithExistingValueShouldReturnValue()
    {
        Object result = evaluator.evaluate("foo", message);
        assertEquals("moo", result);
    }

    @Test
    public void requiredKeyWithOptionalityMarkerInlineAndExistingValueShouldReturnValue()
    {
        Object result = evaluator.evaluate("ba?z", message);
        assertEquals("maz", result);
    }

    @Test(expected = RequiredValueException.class)
    public void requireKeyWithMissingValueShouldFail()
    {
        evaluator.evaluate("nonexisting", message);
    }

    @Test
    public void optionalKeyWithExistingValueShouldReturnValue()
    {
        Object result = evaluator.evaluate("bar?", message);
        assertEquals("mar", result);
    }

    @Test
    public void optionalKeyWithMissingValueShouldReturnNull()
    {
        Object result = evaluator.evaluate("nonexistent?", message);
        assertNull(result);
    }

    @Test
    public void multipleExpressionsShouldReturnMultipleValues() throws Exception
    {
        // direct match
        Object result = evaluator.evaluate("foo,bar?,ba?z,fool?", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(3, map.size());

        assertEquals("moo", map.get("foo"));
        assertEquals("mar", map.get("bar"));
        assertEquals("maz", map.get("ba?z"));
        assertNull(map.get("fool?"));
    }
}
