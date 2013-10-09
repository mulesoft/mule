/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SmallTest
public class CaseInsensitiveHashMapTestCase extends AbstractMuleTestCase
{
    protected CaseInsensitiveHashMap createTestMap()
    {
        CaseInsensitiveHashMap map = new CaseInsensitiveHashMap();
        map.put("FOO", "BAR");
        map.put("DOO", Integer.valueOf(3));
        return map;
    }

    @Test
    public void testMap() throws Exception
    {
        CaseInsensitiveHashMap map = createTestMap();
        doTestMap(map);
    }

    @Test
    public void testMapSerialization() throws Exception
    {
        CaseInsensitiveHashMap map = createTestMap();
        doTestMap(map);

        byte[] bytes = SerializationUtils.serialize(map);
        CaseInsensitiveHashMap resultMap = (CaseInsensitiveHashMap)SerializationUtils.deserialize(bytes);
        doTestMap(resultMap);
    }

    public void doTestMap(CaseInsensitiveHashMap  map) throws Exception
    {
        assertEquals("BAR", map.get("FOO"));
        assertEquals("BAR", map.get("foo"));
        assertEquals("BAR", map.get("Foo"));

        assertEquals(Integer.valueOf(3), map.get("DOO"));
        assertEquals(Integer.valueOf(3), map.get("doo"));
        assertEquals(Integer.valueOf(3), map.get("Doo"));

        assertEquals(2, map.size());

        // Test that the key set contains the same case as we put in
        for (Object o : map.keySet())
        {
            assertTrue(o.equals("FOO") || o.equals("DOO"));
            assertFalse(o.equals("foo") || o.equals("doo"));
        }
    }
}
