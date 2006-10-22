/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.config.JXPathPropertyExtractor;
import org.mule.impl.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JXPathPropertyExtractorTestCase extends AbstractMuleTestCase{

    public void testWithExpressions() {
        Apple apple = new Apple();
        apple.wash();
        FruitBowl payload = new FruitBowl(apple, new Banana());
        Map props = new HashMap();
        props.put("Message-Property", "foo");
        MuleMessage msg = new MuleMessage(payload, props);

        JXPathPropertyExtractor e = new JXPathPropertyExtractor();
        Object value = e.getProperty("apple/washed", msg);
        assertNotNull(value);
        assertTrue(value instanceof Boolean);
        assertTrue(((Boolean)value).booleanValue());

        value = e.getProperty("Message-Property", msg);
        assertNotNull(value);
        assertEquals("foo", value.toString());

        value = e.getProperty("bar", msg);
        assertNull(value);
    }
}
