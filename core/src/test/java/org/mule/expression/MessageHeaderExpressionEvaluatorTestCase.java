/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.expression.RequiredValueException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MessageHeaderExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{
    private HashMap<String, Object> messageProperties;
    private MessageHeaderExpressionEvaluator evaluator = new MessageHeaderExpressionEvaluator();
    private MuleMessage message;

    public MessageHeaderExpressionEvaluatorTestCase()
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
    public void requiredHeaderWithExistingValueShouldReturnValue()
    {
        Object result = evaluator.evaluate("foo", message);
        assertNotNull(result);
        assertEquals("foovalue", result);
    }

    @Test
    public void requiredHeaderWithExistingValueViaExpressionManagerShouldReturnValue()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[header:foo]", message);
        assertNotNull(result);
        assertEquals("foovalue", result);
    }

    @Test(expected = RequiredValueException.class)
    public void requiredHeaderWithMissingValueShouldFail()
    {
        evaluator.evaluate("nonexistent", message);
    }

    @Test(expected = RequiredValueException.class)
    public void requiredHeaderWithMissingValueViaExpressionManagerShouldFail()
    {
        muleContext.getExpressionManager().evaluate("#[header:fool]", message);
    }

    @Test
    public void requiredHeaderWithExplicitPropertyScopeShouldReturnValue() throws Exception
    {
        addInboundMessageProperty("testProp", "testvalue");

        Object result = evaluator.evaluate("INBOUND:testProp", message);
        assertEquals("testvalue", result);
    }

    @Test
    public void optionalHeaderWithExistingValueShouldReturnValue()
    {
        Object result = evaluator.evaluate("foo?", message);
        assertEquals("foovalue", result);
    }

    @Test
    public void optionalHeaderWithExistingValueViaExpressionManagerShouldReturnValue()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[header:foo?]", message);
        assertNotNull(result);
        assertEquals("foovalue", result);
    }

    @Test
    public void optionalHeaderWithMissingValueShouldReturnNull()
    {
        Object result = evaluator.evaluate("nonexistent?", message);
        assertNull(result);
    }

    @Test
    public void optionalHeaderWithMissingValueViaExpressionManagerShouldReturnNull()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[header:nonexistent?]", message);
        assertNull(result);
    }

    @Test
    public void optionalHeaderWithMissingValueInDefaultScopeShouldReturnNull()
    {
        // default scope for header expression evaluation is OUTBOUND. We add INBOUND message
        // properties here, expecting that the header will not be found
        addInboundMessageProperty("testProp", "testvalue");

        Object result = evaluator.evaluate("testProp?", message);
        assertNull(result);
    }

    private void addInboundMessageProperty(String key, Object value)
    {
        Map<String, Object> inboundProperties = Collections.singletonMap(key, value);

        DefaultMuleMessage dmm = (DefaultMuleMessage) message;
        dmm.addInboundProperties(inboundProperties);
    }
}
