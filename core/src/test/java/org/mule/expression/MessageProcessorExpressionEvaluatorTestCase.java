/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

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

import static org.junit.Assert.assertEquals;

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
        assertEquals("0123",
            ((MuleMessage) evaluator.evaluate("processor", createTestMessage())).getPayloadAsString());
    }

    @Test
    public void testNameOnlyExpressionManager() throws ExpressionRuntimeException, Exception
    {
        assertEquals("0123", ((MuleMessage) expressionManager.evaluate("#[process:processor]",
            createTestMessage())).getPayloadAsString());
    }

    @Test
    public void testNestedPayloadExpression() throws Exception
    {
        MessageProcessorExpressionEvaluator evaluator = new MessageProcessorExpressionEvaluator();
        assertEquals("0123",
            ((MuleMessage) evaluator.evaluate("processor:payload:", createTestMessage())).getPayloadAsString());
    }

    @Test
    public void testNestedPayloadExpressionExpressionManager() throws ExpressionRuntimeException, Exception
    {
        assertEquals("0123", ((MuleMessage) expressionManager.evaluate("#[process:processor:#[payload:]]",
            createTestMessage())).getPayloadAsString());
    }

    @Test
    public void testNestedHeaderExpression() throws Exception
    {
        MessageProcessorExpressionEvaluator evaluator = new MessageProcessorExpressionEvaluator();
        assertEquals("value123", ((MuleMessage) evaluator.evaluate("processor:header:one",
            createTestMessage())).getPayloadAsString());
    }

    @Test
    public void testNestedHeaderExpressionExpressionManager() throws ExpressionRuntimeException, Exception
    {
        assertEquals("value123", ((MuleMessage) expressionManager.evaluate(
            "#[process:processor:#[header:one]]", createTestMessage())).getPayloadAsString());
    }

    private MuleMessage createTestMessage()
    {
        MuleMessage message = new DefaultMuleMessage("0", muleContext);
        message.setProperty("one", "value", PropertyScope.OUTBOUND);
        return message;
    }

}
