/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import static java.lang.Boolean.valueOf;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.api.config.MuleProperties.MULE_DEFAULT_BOOLEAN_VALUE;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.simple.StringAppendTransformer;

import java.sql.Timestamp;

import org.junit.Test;

public class ExpressionManagerTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        Object o = muleContext.getExpressionManager().evaluate("function:uuid", message);
        assertNotNull(o);
        o = muleContext.getExpressionManager().evaluate("function:now", message);
        assertNotNull(o);
        assertTrue(o instanceof Timestamp);
    }

    @Test
    public void testRegistration() throws Exception
    {
        // http://mule.mulesoft.org/jira/browse/MULE-3809 . For now ignore duplicate registrations.
        /*
         * try { DefaultExpressionManager.registerEvaluator(new MapPayloadExpressionEvaluator());
         * fail("extractor already exists"); } catch (IllegalArgumentException e) { //Expected }
         */

        try
        {
            muleContext.getExpressionManager().registerEvaluator(null);
            fail("null extractor");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
        assertNull(muleContext.getExpressionManager().unregisterEvaluator(null));
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
            "http://#[header:user]:#[header:password]@#[header:host]:#[header:port]/foo/bar", msg);
        assertNotNull(result);
        assertEquals("http://vasya:pupkin@example.com:12345/foo/bar", result);
    }

    @Test
    public void testNestedParsing() throws Exception
    {
        muleContext.getRegistry().registerObject("proc1", new StringAppendTransformer("c"));
        muleContext.getRegistry().registerObject("proc2", new StringAppendTransformer("e"));

        MuleEvent event = getTestEvent("b");
        RequestContext.setEvent(event);

        assertEquals(
            "-1-abcde-2-",
            muleContext.getExpressionManager().parse(
                "-#[string:1]-#[process:proc2:#[string:a#[process:proc1]d]]-#[string:2]-", event));
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
        assertFalse(muleContext.getExpressionManager().evaluateBoolean("header:user", msg));
        assertTrue(muleContext.getExpressionManager().evaluateBoolean("header:user", msg, false, true));
        assertFalse(muleContext.getExpressionManager().evaluateBoolean("header:user", msg, false, false));

        // Null
        assertFalse(muleContext.getExpressionManager().evaluateBoolean("header:ur", msg));
        assertTrue(muleContext.getExpressionManager().evaluateBoolean("header:ur", msg, true, false));
        assertFalse(muleContext.getExpressionManager().evaluateBoolean("header:ur", msg, false, false));

        // Boolean value
        assertTrue(muleContext.getExpressionManager().evaluateBoolean("string:true", msg));
        assertFalse(muleContext.getExpressionManager().evaluateBoolean("string:false", msg));
    }

    @Test
    public void testDefaultExpressionFilterActsLikeBooleanValueOfInvalidString()
    {
        // Set default value
        setProperty(MULE_DEFAULT_BOOLEAN_VALUE, "false");
        ExpressionFilter filter = new ExpressionFilter("payload");
        assertThat(filter.isNonBooleanReturnsTrue(), is(false));

        // Check that after unset the system property the default value is still the same
        clearProperty(MULE_DEFAULT_BOOLEAN_VALUE);
        assertThat(filter.isNonBooleanReturnsTrue(), is(false));

        filter.setMuleContext(muleContext);

        assertThat(filter.accept(new DefaultMuleMessage("on", muleContext)), is(valueOf("on")));
        assertThat(filter.accept(new DefaultMuleMessage("yes", muleContext)), is(valueOf("yes")));
        assertThat(filter.accept(new DefaultMuleMessage("no", muleContext)), is(valueOf("no")));
        assertThat(filter.accept(new DefaultMuleMessage("off", muleContext)), is(valueOf("off")));
        assertThat(filter.accept(new DefaultMuleMessage("trues", muleContext)), is(valueOf("trues")));
        assertThat(filter.accept(new DefaultMuleMessage("falses", muleContext)), is(valueOf("falses")));
    }

    @Test
    public void testNonBooleanReturnsFalse()
    {
        // Set default value
        setProperty(MULE_DEFAULT_BOOLEAN_VALUE, "false");
        ExpressionFilter filter = new ExpressionFilter("payload");
        assertThat(filter.isNonBooleanReturnsTrue(), is(false));

        // Check that after unset the system property the default value is still the same
        clearProperty(MULE_DEFAULT_BOOLEAN_VALUE);
        assertThat(filter.isNonBooleanReturnsTrue(), is(false));


        filter.setMuleContext(muleContext);

        assertThat(filter.accept(new DefaultMuleMessage("on", muleContext)), is(false));
        assertThat(filter.accept(new DefaultMuleMessage("yes", muleContext)), is(false));
        assertThat(filter.accept(new DefaultMuleMessage("no", muleContext)), is(false));
        assertThat(filter.accept(new DefaultMuleMessage("off", muleContext)), is(false));
        assertThat(filter.accept(new DefaultMuleMessage("trues", muleContext)), is(false));
        assertThat(filter.accept(new DefaultMuleMessage("falses", muleContext)), is(false));
    }

    @Test
    public void testNonBooleanReturnsTrue()
    {
        // Set default value
        setProperty(MULE_DEFAULT_BOOLEAN_VALUE, "true");
        ExpressionFilter filter = new ExpressionFilter("payload");
        assertThat(filter.isNonBooleanReturnsTrue(), is(true));

        // Check that after unset the system property the default value is still the same
        clearProperty(MULE_DEFAULT_BOOLEAN_VALUE);
        assertThat(filter.isNonBooleanReturnsTrue(), is(true));


        filter.setMuleContext(muleContext);

        assertThat(filter.accept(new DefaultMuleMessage("on", muleContext)), is(true));
        assertThat(filter.accept(new DefaultMuleMessage("yes", muleContext)), is(true));
        assertThat(filter.accept(new DefaultMuleMessage("no", muleContext)), is(true));
        assertThat(filter.accept(new DefaultMuleMessage("off", muleContext)), is(true));
        assertThat(filter.accept(new DefaultMuleMessage("trues", muleContext)), is(true));
        assertThat(filter.accept(new DefaultMuleMessage("falses", muleContext)), is(true));
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
        assertEquals(4, muleContext.getExpressionManager()
            .evaluate("#[2*2]", null, (MuleMessage) null, false));
    }

    @Test
    public void testEvaluateBooleanELExpressionWithNullEvaluator()
    {
        assertTrue(muleContext.getExpressionManager().evaluateBoolean("#[2>1]", null, (MuleMessage) null,
            false, false));
    }

}
