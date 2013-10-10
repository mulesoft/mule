/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StringExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{
    private Map props;

        @Override
        public void doSetUp()
        {
            props = new HashMap(3);
            props.put("foo", "moo");
            props.put("bar", "mar");
            props.put("baz", "maz");
        }

    @Test
    public void testString() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(new Apple(), props, muleContext);
        StringExpressionEvaluator extractor = new StringExpressionEvaluator();
        extractor.setMuleContext(muleContext);
        Object o = extractor.evaluate("payload is #[function:shortPayloadClass] and has foo=#[header:foo] header", message);
        assertNotNull(o);
        assertEquals("payload is Apple and has foo=moo header", o.toString());

        o = extractor.evaluate("literal string", message);
        assertNotNull(o);
        assertEquals("literal string", o.toString());
    }


    @Test
    public void testStringUsingManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(new Apple(), props, muleContext);
        Object o = muleContext.getExpressionManager().evaluate("#[string:payload is #[function:shortPayloadClass] and has foo=#[header:foo] header]", message);
        assertNotNull(o);
        assertEquals("payload is Apple and has foo=moo header", o.toString());

        o = muleContext.getExpressionManager().evaluate("#[string:literal string]", message);
        assertNotNull(o);
        assertEquals("literal string", o.toString());
    }
}
