/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml;

import org.mule.module.xml.transformer.XStreamFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.StringUtils;

import com.thoughtworks.xstream.XStream;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class XStreamFactoryTestCase extends AbstractMuleTestCase
{
    @Test
    public void testConcurrentHashMapConverter()
        throws ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        ConcurrentHashMap<Object, Object> map = new ConcurrentHashMap<Object, Object>();
        map.put("foo", "bar");

        XStream xstream = new XStreamFactory().getInstance();
        String mapXML = xstream.toXML(map);
        assertNotNull(mapXML);
        assertTrue(StringUtils.isNotEmpty(mapXML));

        Object newMap = xstream.fromXML(mapXML);
        assertNotNull(newMap);
        assertTrue(newMap instanceof ConcurrentHashMap);
        assertEquals(1, ((Map<?, ?>) newMap).size());
        assertEquals("bar", ((Map<?, ?>) newMap).get("foo"));
    }
}
