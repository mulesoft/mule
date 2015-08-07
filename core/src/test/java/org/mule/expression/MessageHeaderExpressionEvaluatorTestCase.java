/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import static org.mule.transformer.types.MimeTypes.ANY;
import static org.mule.transformer.types.MimeTypes.JSON;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.TypedValue;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class MessageHeaderExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{

    private static final String CUSTOM_ENCODING = StandardCharsets.UTF_16.name();
    public static final String PROP_NAME = "testProp";
    public static final String PROP_VALUE = "testValue";

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
        addInboundMessageProperty(PROP_NAME, "testvalue");

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
        addInboundMessageProperty(PROP_NAME, "testvalue");

        Object result = evaluator.evaluate("testProp?", message);
        assertNull(result);
    }

    @Test
    public void evaluatesWithType() throws Exception
    {
        DataType dataType = DataTypeFactory.create(String.class, JSON);
        dataType.setEncoding(CUSTOM_ENCODING);
        message.setProperty(PROP_NAME, PROP_VALUE, PropertyScope.OUTBOUND, dataType);

        final TypedValue typedValue = evaluator.evaluateTyped(PROP_NAME, message);

        assertThat((String) typedValue.getValue(), equalTo(PROP_VALUE));
        assertThat(typedValue.getDataType(), like(String.class, JSON, CUSTOM_ENCODING));
    }

    @Test
    public void evaluatesUnExistentPropertyWithType() throws Exception
    {
        final TypedValue typedValue = evaluator.evaluateTyped("UNKNOWN?", message);

        assertThat(typedValue.getValue(), equalTo(null));
        assertThat(typedValue.getDataType(), like(Object.class, ANY, null));
    }

    private void addInboundMessageProperty(String key, Object value)
    {
        Map<String, Object> inboundProperties = Collections.singletonMap(key, value);

        DefaultMuleMessage dmm = (DefaultMuleMessage) message;
        dmm.addInboundProperties(inboundProperties);
    }
}
