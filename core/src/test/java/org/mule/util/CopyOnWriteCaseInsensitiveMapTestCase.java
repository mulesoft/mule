/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

@SuppressWarnings("unchecked")
@SmallTest
public class CopyOnWriteCaseInsensitiveMapTestCase extends AbstractMuleTestCase
{

    @Test
    public void caseInsensitive() throws Exception
    {
        assertMapContents(createTestMap());
    }

    @Test
    public void caseInsensitiveDelegate() throws Exception
    {
        assertMapContents(new CopyOnWriteCaseInsensitiveMap<String, Object>(createTestMap()));
    }

    @Test
    public void caseInsensitiveCopiedDelegate() throws Exception
    {
        Map<String, Object> map = new CopyOnWriteCaseInsensitiveMap<String, Object>(createTestMap());
        map.put("new", "val");
        assertMapContents(map);
    }

    @Test
    public void serialize() throws Exception
    {
        assertMapContents(serializeAndDeserialize(createTestMap()));
    }

    @Test
    public void serializeDelegate() throws Exception
    {
        assertMapContents(serializeAndDeserialize(new CopyOnWriteCaseInsensitiveMap<String, Object>(
            createTestMap())));
    }

    @Test
    public void serializeCopiedDelegate() throws Exception
    {
        Map<String, Object> map = new CopyOnWriteCaseInsensitiveMap<String, Object>(createTestMap());
        map.put("new", "val");
        assertMapContents(serializeAndDeserialize(map));
    }

    protected Map<String, Object> serializeAndDeserialize(Map<String, Object> map)
    {
        byte[] bytes = SerializationUtils.serialize((Serializable) map);
        return (Map) SerializationUtils.deserialize(bytes);
    }

    protected void assertMapContents(Map<String, Object> map)
    {
        assertEquals("BAR", map.get("FOO"));
        assertEquals("BAR", map.get("foo"));
        assertEquals("BAR", map.get("Foo"));

        assertEquals(Integer.valueOf(3), map.get("DOO"));
        assertEquals(Integer.valueOf(3), map.get("doo"));
        assertEquals(Integer.valueOf(3), map.get("Doo"));

        // Test that the key set contains the same case as we put in
        for (Object o : map.keySet())
        {
            assertFalse(o.equals("foo") || o.equals("doo"));
        }
    }

    protected CopyOnWriteCaseInsensitiveMap<String, Object> createTestMap()
    {
        CopyOnWriteCaseInsensitiveMap<String, Object> map = new CopyOnWriteCaseInsensitiveMap<String, Object>();
        map.put("FOO", "BAR");
        map.put("DOO", Integer.valueOf(3));
        return map;
    }

    @Test
    public void copyOnPut() throws Exception
    {
        CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
        Map<String, Object> copyOnWriteMap = new CopyOnWriteCaseInsensitiveMap<String, Object>(original);

        copyOnWriteMap.put("other", "val");

        // Assert state of original map
        assertMapContents(original);
        assertEquals(2, original.size());
        assertFalse(original.containsKey("other"));

        // Assert state of copy on write map
        assertMapContents(copyOnWriteMap);
        assertEquals(3, copyOnWriteMap.size());
        assertTrue(copyOnWriteMap.containsKey("other"));
    }

    @Test
    public void copyOnPutAll() throws Exception
    {
        CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
        Map<String, Object> copyOnWriteMap = new CopyOnWriteCaseInsensitiveMap<String, Object>(original);

        Map<String, String> extrasMap = new HashMap<String, String>();
        extrasMap.put("extra1", "val");
        extrasMap.put("extra2", "val");
        extrasMap.put("extra3", "val");
        copyOnWriteMap.putAll(extrasMap);

        // Assert state of original map
        assertMapContents(original);
        assertEquals(2, original.size());
        assertFalse(original.containsKey("extra1"));
        assertFalse(original.containsKey("extra2"));
        assertFalse(original.containsKey("extra3"));

        // Assert state of copy on write map
        assertMapContents(copyOnWriteMap);
        assertEquals(5, copyOnWriteMap.size());
        assertTrue(copyOnWriteMap.containsKey("extra1"));
        assertTrue(copyOnWriteMap.containsKey("extra2"));
        assertTrue(copyOnWriteMap.containsKey("extra3"));
    }

    @Test
    public void copyOnPutRemove() throws Exception
    {
        CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
        original.put("extra", "value");
        Map<String, Object> copyOnWriteMap = new CopyOnWriteCaseInsensitiveMap<String, Object>(original);

        copyOnWriteMap.remove("extra");

        // Assert state of original map
        assertMapContents(original);
        assertEquals(3, original.size());
        assertTrue(original.containsKey("extra"));

        // Assert state of copy on write map
        assertMapContents(copyOnWriteMap);
        assertEquals(2, copyOnWriteMap.size());
        assertFalse(copyOnWriteMap.containsKey("extra"));
    }

    @Test
    public void copyOnClear() throws Exception
    {
        CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
        Map<String, Object> copyOnWriteMap = new CopyOnWriteCaseInsensitiveMap<String, Object>(original);

        copyOnWriteMap.clear();

        // Assert state of original map
        assertMapContents(original);
        assertEquals(2, original.size());

        // Assert state of copy on write map
        assertEquals(0, copyOnWriteMap.size());
    }

}
