/* 
* $Id$
* ------------------------------------------------------------------------------------------------------
* 
* Copyright (c) SymphonySoft Limited. All rights reserved.
* http://www.symphonysoft.com
* 
* The software in this package is published under the terms of the BSD
* style license a copy of which has been included with this distribution in
* the LICENSE.txt file. 
*
*/
package org.mule.test.config;

import org.mule.config.JXPathPropertyExtractor;
import org.mule.impl.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        List exp = new ArrayList();
        exp.add("apple/washed");
        exp.add("apple/bitten");
        exp.add("Message-Property");
        exp.add("bar");

        Map values = e.getProperties(exp, msg);
        assertNotNull(values);
        assertEquals(Boolean.TRUE, values.get("apple/washed"));
        assertEquals(Boolean.FALSE, values.get("apple/bitten"));
        assertEquals("foo", values.get("Message-Property"));
        assertNull(values.get("bar"));

        assertEquals(4, values.size());

    }
}
