/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Date;

import org.junit.Test;

public class ExpressionManagerTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        Object o = muleContext.getExpressionManager().evaluate("org.mule.util.UUID.getUUID()", message);
        assertNotNull(o);
        o = muleContext.getExpressionManager().evaluate("server.dateTime.toDate()", message);
        assertNotNull(o);
        assertTrue(o instanceof Date);
    }

    @Test
    public void testValidator() throws Exception
    {
        // fail for old-style ${}
        assertFalse(muleContext.getExpressionManager().isValidExpression(
            "http://${bean:user}:${bean:password}@${header:host}:${header:port}/foo/bar"));
        assertFalse(muleContext.getExpressionManager().isValidExpression("${bean:user}"));

        // wiggly mule style!
        assertTrue(muleContext.getExpressionManager().isValidExpression("#[bean:user]"));
        assertTrue(muleContext.getExpressionManager().isValidExpression(
            "http://#[bean:user]:#[bean:password]@#[header:host]:#[header:port]/foo/bar"));

        assertFalse(muleContext.getExpressionManager().isValidExpression("{bean:user}"));
        assertFalse(muleContext.getExpressionManager().isValidExpression("#{bean:user"));
        assertFalse(muleContext.getExpressionManager().isValidExpression("user"));

        assertFalse(muleContext.getExpressionManager().isValidExpression(
            "http://#[bean:user:#[bean:password]@#[header:host]:#[header:port]/foo/bar"));
        assertTrue(muleContext.getExpressionManager().isValidExpression(
            "http://#[bean:user]:##[bean:password]@#[header:host]:#[header:port]/foo/bar"));
        assertTrue(muleContext.getExpressionManager().isValidExpression(
            "http://#[bean:user]]:##[bean:password]@#[header:host]:#[header:port]/foo/bar"));
        assertFalse(muleContext.getExpressionManager().isValidExpression(
            "http://#[bean:user]:#[[bean:password]@#[header:host]:#[header:port]/foo/bar"));
        assertTrue(muleContext.getExpressionManager().isValidExpression(
            "http://#[bean:user]:#[#bean:password]@#[header:host]:#[header:port]/foo/bar"));
    }

    @Test
    public void testParsing() throws Exception
    {
        MuleMessage msg = new DefaultMuleMessage("test", muleContext);
        msg.setOutboundProperty("user", "vasya");
        msg.setOutboundProperty("password", "pupkin");
        msg.setOutboundProperty("host", "example.com");
        msg.setOutboundProperty("port", "12345");

        String result = muleContext.getExpressionManager().parse(
            "http://#[message.outboundProperties.user]:#[message.outboundProperties.password]@#[message.outboundProperties.host]:#[message.outboundProperties.port]/foo/bar", msg);
        assertNotNull(result);
        assertEquals("http://vasya:pupkin@example.com:12345/foo/bar", result);
    }

    @Test
    public void testBooleanEvaluation()
    {
        MuleMessage msg = new DefaultMuleMessage("test", muleContext);
        msg.setOutboundProperty("user", "vasya");
        msg.setOutboundProperty("password", "pupkin");
        msg.setOutboundProperty("host", "example.com");
        msg.setOutboundProperty("port", "12345");

        // Non-boolean string value
        assertFalse(muleContext.getExpressionManager().evaluateBoolean("message.outboundProperties.user", msg));
        assertTrue(muleContext.getExpressionManager().evaluateBoolean("message.outboundProperties.user", msg, false, true));
        assertFalse(muleContext.getExpressionManager().evaluateBoolean("message.outboundProperties.user", msg, false, false));

        // Null
        assertFalse(muleContext.getExpressionManager().evaluateBoolean("message.outboundProperties.ur", msg));
        assertTrue(muleContext.getExpressionManager().evaluateBoolean("message.outboundProperties.ur", msg, true, false));
        assertFalse(muleContext.getExpressionManager().evaluateBoolean("message.outboundProperties.ur", msg, false, false));

        // Boolean value
        assertTrue(muleContext.getExpressionManager().evaluateBoolean("true", msg));
        assertFalse(muleContext.getExpressionManager().evaluateBoolean("false", msg));
    }

    @Test
    public void testELExpression()
    {
        assertEquals(4, muleContext.getExpressionManager().evaluate("#[2*2]", (MuleMessage) null));
    }

    @Test
    public void testBooleanELExpression()
    {
        assertEquals(true, muleContext.getExpressionManager().evaluateBoolean("#[2>1]", (MuleMessage) null));
    }

    @Test
    public void testELExpressionValid()
    {
        assertTrue(muleContext.getExpressionManager().isValidExpression("#[2*2]"));
    }

    @Test
    public void testELExpressionInvalid()
    {
        assertFalse(muleContext.getExpressionManager().isValidExpression("#[2*'2]"));
    }

    @Test
    public void testEvaluateELExpressionWithNullEvaluator()
    {
        assertEquals(4, muleContext.getExpressionManager().evaluate("#[2*2]", (MuleMessage) null, false));
    }

    @Test
    public void testEvaluateBooleanELExpressionWithNullEvaluator()
    {
        assertTrue(muleContext.getExpressionManager().evaluateBoolean("#[2>1]", (MuleMessage) null, false, false));
    }

}
