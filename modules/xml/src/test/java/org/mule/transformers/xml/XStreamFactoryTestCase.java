/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
