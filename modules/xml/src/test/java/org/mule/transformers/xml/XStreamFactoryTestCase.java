/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.StringUtils;

import com.thoughtworks.xstream.XStream;

import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

public class XStreamFactoryTestCase extends AbstractMuleTestCase
{

    public void testConcurrentHashMapConverter()
        throws ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        ConcurrentHashMap map = new ConcurrentHashMap();
        map.put("foo", "bar");

        XStream xstream = new XStreamFactory().getInstance();
        String mapXML = xstream.toXML(map);
        assertNotNull(mapXML);
        assertTrue(StringUtils.isNotEmpty(mapXML));

        Object newMap = xstream.fromXML(mapXML);
        assertNotNull(newMap);
        assertTrue(newMap instanceof ConcurrentHashMap);
        assertEquals(1, ((Map)newMap).size());
        assertEquals("bar", ((Map)newMap).get("foo"));
    }

}
