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
    public void keysCaseSensitive() throws Exception
    {
        Map<String, Object> map = createTestMap();

        assertEquals(2, map.keySet().size());
        assertTrue(map.keySet().toArray()[0].equals("FOO") || map.keySet().toArray()[0].equals("doo"));
        assertTrue(map.keySet().toArray()[1].equals("FOO") || map.keySet().toArray()[1].equals("doo"));
    }

    @Test
    public void caseInsensitiveDelegate() throws Exception
    {
        assertMapContents(createTestMap().clone());
    }

    @Test
    public void caseInsensitiveCopiedDelegate() throws Exception
    {
        Map<String, Object> map = createTestMap().clone();
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
        assertMapContents(serializeAndDeserialize(createTestMap().clone()));
    }

    @Test
    public void serializeCopiedDelegate() throws Exception
    {
        Map<String, Object> map = createTestMap().clone();
        map.put("new", "val");
        assertMapContents(serializeAndDeserialize(map));
    }

    protected Map<String, Object> serializeAndDeserialize(Map<String, Object> map)
    {
        byte[] bytes = SerializationUtils.serialize((Serializable) map);
        return (Map) SerializationUtils.deserialize(bytes);
    }

    /*
     * Assert that Map created with createTestMap() has both of the original properties (FOO and DOO) and that
     * these can be accessed using case-insensitive keys
     */
    protected void assertMapContents(Map<String, Object> map)
    {
        assertEquals("BAR", map.get("FOO"));
        assertEquals("BAR", map.get("foo"));
        assertEquals("BAR", map.get("Foo"));

        assertEquals(Integer.valueOf(3), map.get("DOO"));
        assertEquals(Integer.valueOf(3), map.get("doo"));
        assertEquals(Integer.valueOf(3), map.get("Doo"));
    }

    /*
     * Create a CopyOnWriteCaseInsensitiveMap with two properties: F00=BAR (String) and DOO=3 (int).
     */
    protected CopyOnWriteCaseInsensitiveMap<String, Object> createTestMap()
    {
        CopyOnWriteCaseInsensitiveMap<String, Object> map = new CopyOnWriteCaseInsensitiveMap<String, Object>();
        map.put("FOO", "BAR");
        map.put("doo", Integer.valueOf(3));
        return map;
    }

    @Test
    public void putClone() throws Exception
    {
        CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
        Map<String, Object> copyOnWriteMap = original.clone();

        original.put("newOriginal", "val");
        copyOnWriteMap.put("newCopy", "val");

        // Assert state of original map
        assertMapContents(original);
        assertEquals(3, original.size());
        assertFalse(original.containsKey("newCopy"));
        assertTrue(original.containsKey("newOriginal"));

        // Assert state of copy on write map
        assertMapContents(copyOnWriteMap);
        assertEquals(3, copyOnWriteMap.size());
        assertTrue(copyOnWriteMap.containsKey("newCopy"));
        assertFalse(copyOnWriteMap.containsKey("newOriginal"));
    }

    @Test
    public void putAllClone() throws Exception
    {
        CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
        Map<String, Object> copyOnWriteMap = original.clone();

        Map<String, String> newOriginalEntriesMap = new HashMap<String, String>();
        newOriginalEntriesMap.put("newOriginal1", "val");
        newOriginalEntriesMap.put("newOriginal2", "val");
        newOriginalEntriesMap.put("newOriginal3", "val");
        original.putAll(newOriginalEntriesMap);

        Map<String, String> newCopyEntriesMap = new HashMap<String, String>();
        newCopyEntriesMap.put("newCopy1", "val");
        newCopyEntriesMap.put("newCopy2", "val");
        newCopyEntriesMap.put("newCopy3", "val");
        copyOnWriteMap.putAll(newCopyEntriesMap);

        // Assert state of original map
        assertMapContents(original);
        assertEquals(5, original.size());
        assertTrue(original.containsKey("newOriginal1"));
        assertTrue(original.containsKey("newOriginal2"));
        assertTrue(original.containsKey("newOriginal3"));
        assertFalse(original.containsKey("newCopy1"));
        assertFalse(original.containsKey("newCopy2"));
        assertFalse(original.containsKey("newCopy3"));

        // Assert state of copy on write map
        assertMapContents(copyOnWriteMap);
        assertEquals(5, copyOnWriteMap.size());
        assertTrue(copyOnWriteMap.containsKey("newCopy1"));
        assertTrue(copyOnWriteMap.containsKey("newCopy2"));
        assertTrue(copyOnWriteMap.containsKey("newCopy3"));
        assertFalse(copyOnWriteMap.containsKey("newOriginal1"));
        assertFalse(copyOnWriteMap.containsKey("newOriginal2"));
        assertFalse(copyOnWriteMap.containsKey("newOriginal3"));
    }

    @Test
    public void removeClone() throws Exception
    {
        CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
        original.put("extra", "value");
        original.put("extra2", "value");
        Map<String, Object> copyOnWriteMap = original.clone();

        original.remove("extra");
        copyOnWriteMap.remove("extra2");

        // Assert state of original map
        assertMapContents(original);
        assertEquals(3, original.size());
        assertFalse(original.containsKey("extra"));
        assertTrue(original.containsKey("extra2"));

        // Assert state of copy on write map
        assertMapContents(copyOnWriteMap);
        assertEquals(3, copyOnWriteMap.size());
        assertTrue(copyOnWriteMap.containsKey("extra"));
        assertFalse(copyOnWriteMap.containsKey("extra2"));
    }

    @Test
    public void clearOrignalClone() throws Exception
    {
        CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
        Map<String, Object> copyOnWriteMap = original.clone();

        original.clear();
        assertEquals(0, original.size());
        assertEquals(0, original.entrySet().size());
        assertEquals(2, copyOnWriteMap.size());
        assertEquals(2, copyOnWriteMap.entrySet().size());
    }

    @Test
    public void clearCopyClone() throws Exception
    {
        CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
        Map<String, Object> copyOnWriteMap = original.clone();

        copyOnWriteMap.clear();
        assertEquals(2, original.size());
        assertEquals(2, original.entrySet().size());
        assertEquals(0, copyOnWriteMap.size());
        assertEquals(0, copyOnWriteMap.entrySet().size());
    }

    @Test
    public void putDeserialized() throws Exception
    {
        CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
        Map<String, Object> copyOnWriteMap = serializeAndDeserialize(original);

        original.put("newOriginal", "val");
        copyOnWriteMap.put("newCopy", "val");

        // Assert state of original map
        assertMapContents(original);
        assertEquals(3, original.size());
        assertFalse(original.containsKey("newCopy"));
        assertTrue(original.containsKey("newOriginal"));

        // Assert state of copy on write map
        assertMapContents(copyOnWriteMap);
        assertEquals(3, copyOnWriteMap.size());
        assertTrue(copyOnWriteMap.containsKey("newCopy"));
        assertFalse(copyOnWriteMap.containsKey("newOriginal"));
    }

    @Test
    public void putAllDeserialized() throws Exception
    {
        CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
        Map<String, Object> copyOnWriteMap = serializeAndDeserialize(original);

        Map<String, String> newOriginalEntriesMap = new HashMap<String, String>();
        newOriginalEntriesMap.put("newOriginal1", "val");
        newOriginalEntriesMap.put("newOriginal2", "val");
        newOriginalEntriesMap.put("newOriginal3", "val");
        original.putAll(newOriginalEntriesMap);

        Map<String, String> newCopyEntriesMap = new HashMap<String, String>();
        newCopyEntriesMap.put("newCopy1", "val");
        newCopyEntriesMap.put("newCopy2", "val");
        newCopyEntriesMap.put("newCopy3", "val");
        copyOnWriteMap.putAll(newCopyEntriesMap);

        // Assert state of original map
        assertMapContents(original);
        assertEquals(5, original.size());
        assertTrue(original.containsKey("newOriginal1"));
        assertTrue(original.containsKey("newOriginal2"));
        assertTrue(original.containsKey("newOriginal3"));
        assertFalse(original.containsKey("newCopy1"));
        assertFalse(original.containsKey("newCopy2"));
        assertFalse(original.containsKey("newCopy3"));

        // Assert state of copy on write map
        assertMapContents(copyOnWriteMap);
        assertEquals(5, copyOnWriteMap.size());
        assertTrue(copyOnWriteMap.containsKey("newCopy1"));
        assertTrue(copyOnWriteMap.containsKey("newCopy2"));
        assertTrue(copyOnWriteMap.containsKey("newCopy3"));
        assertFalse(copyOnWriteMap.containsKey("newOriginal1"));
        assertFalse(copyOnWriteMap.containsKey("newOriginal2"));
        assertFalse(copyOnWriteMap.containsKey("newOriginal3"));
    }

    @Test
    public void removeDeserialized() throws Exception
    {
        CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
        original.put("extra", "value");
        original.put("extra2", "value");
        Map<String, Object> copyOnWriteMap = serializeAndDeserialize(original);

        original.remove("extra");
        copyOnWriteMap.remove("extra2");

        // Assert state of original map
        assertMapContents(original);
        assertEquals(3, original.size());
        assertFalse(original.containsKey("extra"));
        assertTrue(original.containsKey("extra2"));

        // Assert state of copy on write map
        assertMapContents(copyOnWriteMap);
        assertEquals(3, copyOnWriteMap.size());
        assertTrue(copyOnWriteMap.containsKey("extra"));
        assertFalse(copyOnWriteMap.containsKey("extra2"));
    }

    @Test
    public void clearOrignalDeserialized() throws Exception
    {
        CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
        Map<String, Object> copyOnWriteMap = serializeAndDeserialize(original);

        original.clear();
        assertEquals(0, original.size());
        assertEquals(0, original.entrySet().size());
        assertEquals(2, copyOnWriteMap.size());
        assertEquals(2, copyOnWriteMap.entrySet().size());
    }

    @Test
    public void clearCopyDeserialized() throws Exception
    {
        CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
        Map<String, Object> copyOnWriteMap = serializeAndDeserialize(original);

        copyOnWriteMap.clear();
        assertEquals(2, original.size());
        assertEquals(2, original.entrySet().size());
        assertEquals(0, copyOnWriteMap.size());
        assertEquals(0, copyOnWriteMap.entrySet().size());
    }

}
