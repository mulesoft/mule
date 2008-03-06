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
import org.mule.module.xml.util.properties.JXPathPropertyExtractor;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;

public class JXPathPropertyExtractorTestCase extends AbstractMuleTestCase
{

    public void testWithExpressions()
    {
        Apple apple = new Apple();
        apple.wash();
        FruitBowl payload = new FruitBowl(apple, new Banana());
        DefaultMuleMessage msg = new DefaultMuleMessage(payload);

        JXPathPropertyExtractor e = new JXPathPropertyExtractor();
        Object value = e.getProperty("apple/washed", msg);
        assertNotNull(value);
        assertTrue(value instanceof Boolean);
        assertTrue(((Boolean)value).booleanValue());

        value = e.getProperty("apple/washed", payload);
        assertNotNull(value);
        assertTrue(value instanceof Boolean);
        assertTrue(((Boolean)value).booleanValue());

        value = e.getProperty("bar", msg);
        assertNull(value);
    }
}
