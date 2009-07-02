/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.HashMap;
import java.util.Map;

public class StringExpressionEvaluatorTestCase extends AbstractMuleTestCase
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

    public void teststring() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(new Apple(), props, muleContext);
        StringExpressionEvaluator extractor = new StringExpressionEvaluator();
        extractor.setMuleContext(muleContext);
        Object o = extractor.evaluate("payload is #[function:shortPayloadClass] and has #[headers:{count}] headers", message);
        assertNotNull(o);
        assertEquals("payload is Apple and has 3 headers", o.toString());

        o = extractor.evaluate("literal string", message);
        assertNotNull(o);
        assertEquals("literal string", o.toString());
    }


    public void testStringUsingManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(new Apple(), props, muleContext);
        Object o = muleContext.getExpressionManager().evaluate("#[string:payload is #[function:shortPayloadClass] and has #[headers:{count}] headers]", message);
        assertNotNull(o);
        assertEquals("payload is Apple and has 3 headers", o.toString());

        o = muleContext.getExpressionManager().evaluate("#[string:literal string]", message);
        assertNotNull(o);
        assertEquals("literal string", o.toString());
    }
}