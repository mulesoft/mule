/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.xml.util.properties;

import org.mule.DefaultMuleMessage;
import org.mule.module.xml.expression.JXPathExpressionEvaluator;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;

public class JXPathExpressionEvaluatorTestCase extends AbstractMuleTestCase
{

    public void testWithExpressions()
    {
        Apple apple = new Apple();
        apple.wash();
        FruitBowl payload = new FruitBowl(apple, new Banana());
        DefaultMuleMessage msg = new DefaultMuleMessage(payload, muleContext);

        JXPathExpressionEvaluator e = new JXPathExpressionEvaluator();
        Object value = e.evaluate("apple/washed", msg);
        assertNotNull(value);
        assertTrue(value instanceof Boolean);
        assertTrue(((Boolean)value).booleanValue());

        value = e.evaluate("bar", msg);
        assertNull(value);
    }
}
