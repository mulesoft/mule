/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import static org.junit.Assert.assertEquals;
import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class MessageProcessorExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{

    private ExpressionManager expressionManager;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        expressionManager = muleContext.getExpressionManager();
        RequestContext.setEvent(getTestEvent(""));

        muleContext.getRegistry().registerObject("processor", new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setPayload(event.getMessageAsString() + "123");
                return event;
            }
        });
    }

    @Test
    public void testNameOnly() throws Exception
    {
        MessageProcessorExpressionEvaluator evaluator = new MessageProcessorExpressionEvaluator();
        assertEquals("0123", getPayloadAsString((MuleMessage) evaluator.evaluate("processor", createTestMessage())));
    }

    @Test
    public void testNameOnlyExpressionManager() throws ExpressionRuntimeException, Exception
    {
        assertEquals("0123", getPayloadAsString((MuleMessage) expressionManager.evaluate("#[process:processor]",
            createTestMessage())));
    }

    @Test
    public void testNestedPayloadExpression() throws Exception
    {
        MessageProcessorExpressionEvaluator evaluator = new MessageProcessorExpressionEvaluator();
        assertEquals("0123", getPayloadAsString((MuleMessage) evaluator.evaluate("processor:payload:", createTestMessage())));
    }

    @Test
    public void testNestedPayloadExpressionExpressionManager() throws ExpressionRuntimeException, Exception
    {
        assertEquals("0123", getPayloadAsString((MuleMessage) expressionManager.evaluate("#[process:processor:#[payload:]]",
            createTestMessage())));
    }

    @Test
    public void testNestedHeaderExpression() throws Exception
    {
        MessageProcessorExpressionEvaluator evaluator = new MessageProcessorExpressionEvaluator();
        assertEquals("value123", getPayloadAsString((MuleMessage) evaluator.evaluate("processor:header:one",
            createTestMessage())));
    }

    @Test
    public void testNestedHeaderExpressionExpressionManager() throws ExpressionRuntimeException, Exception
    {
        assertEquals("value123", getPayloadAsString((MuleMessage) expressionManager.evaluate(
            "#[process:processor:#[header:one]]", createTestMessage())));
    }

    private MuleMessage createTestMessage()
    {
        MuleMessage message = new DefaultMuleMessage("0", muleContext);
        message.setProperty("one", "value", PropertyScope.OUTBOUND);
        return message;
    }

}
