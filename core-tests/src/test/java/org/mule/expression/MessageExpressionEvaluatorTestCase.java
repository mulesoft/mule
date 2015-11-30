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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import static org.mule.transformer.types.MimeTypes.JSON;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.message.DefaultExceptionPayload;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.TypedValue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class MessageExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{

    private static final String CUSTOM_ENCODING = StandardCharsets.UTF_16.name();
    private static final String PAYLOAD = "test";
    private static final String PAYLOAD_EXPRESSION = "payload";

    @Test
    public void testUsingEvaluatorDirectly() throws Exception
    {
        MessageExpressionEvaluator eval = new MessageExpressionEvaluator();
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        message.setCorrelationId(message.getUniqueId());
        message.setCorrelationSequence(1);
        message.setCorrelationGroupSize(2);
        message.setReplyTo("foo");
        message.setEncoding("UTF-8");
        Exception e = new Exception("dummy");
        message.setExceptionPayload(new DefaultExceptionPayload(e));

        //no expression
        Object result = eval.evaluate(null, message);
        assertNotNull(result);
        assertEquals(message, result);

        //no expression
        result = eval.evaluate(null, null);
        assertNull(result);

        assertEquals(message.getUniqueId(), eval.evaluate("id", message));
        assertEquals(message.getUniqueId(), eval.evaluate("correlationId", message));
        assertEquals(new Integer(1), eval.evaluate("correlationSequence", message));
        assertEquals(new Integer(2), eval.evaluate("correlationGroupSize", message));
        assertEquals("foo", eval.evaluate("replyTo", message));
        assertEquals(e, eval.evaluate("exception", message));
        assertEquals("UTF-8", eval.evaluate("encoding", message));
        assertEquals("test", eval.evaluate(PAYLOAD_EXPRESSION, message));

        try
        {
            eval.evaluate("xxx", message);
            fail("xxx is not a supported expresion");
        }
        catch (Exception e1)
        {
            //Exprected
        }
    }

    /**
     * Make sure the evaluator gets registered properly
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testUsingManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        message.setCorrelationId(message.getUniqueId());
        message.setCorrelationSequence(1);
        message.setCorrelationGroupSize(2);
        message.setReplyTo("foo");
        message.setEncoding("UTF-8");
        Exception e = new Exception("dummy");
        message.setExceptionPayload(new DefaultExceptionPayload(e));


        assertEquals(message.getUniqueId(), muleContext.getExpressionManager().evaluate("#[message:id]", message));
        assertEquals(message.getUniqueId(), muleContext.getExpressionManager().evaluate("#[message:correlationId]", message));
        assertEquals(new Integer(1), muleContext.getExpressionManager().evaluate("#[message:correlationSequence]", message));
        assertEquals(new Integer(2), muleContext.getExpressionManager().evaluate("#[message:correlationGroupSize]", message));
        assertEquals("foo", muleContext.getExpressionManager().evaluate("#[message:replyTo]", message));
        assertEquals(e, muleContext.getExpressionManager().evaluate("#[message:exception]", message));
        assertEquals("UTF-8", muleContext.getExpressionManager().evaluate("#[message:encoding]", message));
        assertEquals("test", muleContext.getExpressionManager().evaluate("#[message:payload]", message));

        try
        {
            muleContext.getExpressionManager().evaluate("#[message:xxx]", message, true);
            fail("xxx is not a supported expresion");
        }
        catch (Exception e1)
        {
            //Expected
        }
    }

    @Test
    public void evaluatesPayloadWithType() throws Exception
    {
        MessageExpressionEvaluator evaluator = new MessageExpressionEvaluator();

        final DataType dataType = DataTypeFactory.create(String.class, JSON);
        dataType.setEncoding(CUSTOM_ENCODING);

        final MuleMessage message = mock(MuleMessage.class);
        when(message.getPayload()).thenReturn(PAYLOAD);
        when(message.getDataType()).thenReturn(dataType);

        final TypedValue typedValue = evaluator.evaluateTyped(PAYLOAD_EXPRESSION, message);

        assertThat((String) typedValue.getValue(), equalTo("test"));
        assertThat(typedValue.getDataType(), like(String.class, JSON, CUSTOM_ENCODING));
    }
}
