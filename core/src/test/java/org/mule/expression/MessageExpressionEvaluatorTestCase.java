/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.message.DefaultExceptionPayload;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class MessageExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{
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
        assertEquals("test", eval.evaluate("payload", message));

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
}
