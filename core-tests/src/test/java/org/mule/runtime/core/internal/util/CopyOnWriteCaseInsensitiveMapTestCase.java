/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

@SuppressWarnings("unchecked")
@SmallTest
public class CopyOnWriteCaseInsensitiveMapTestCase extends AbstractMuleTestCase {

  private static final String KEY1 = "FOO";
  private static final String KEY2 = "doo";

  @Test
  public void caseInsensitive() throws Exception {
    assertMapContents(createTestMap());
  }

  @Test
  public void keysCaseSensitive() throws Exception {
    Map<String, Object> map = createTestMap();

    assertEquals(2, map.keySet().size());
    assertTrue(map.keySet().toArray()[0].equals("FOO") || map.keySet().toArray()[0].equals("doo"));
    assertTrue(map.keySet().toArray()[1].equals("FOO") || map.keySet().toArray()[1].equals("doo"));
  }

  @Test
  public void caseInsensitiveDelegate() throws Exception {
    assertMapContents(createTestMap().clone());
  }

  @Test
  public void caseInsensitiveCopiedDelegate() throws Exception {
    Map<String, Object> map = createTestMap().clone();
    map.put("new", "val");
    assertMapContents(map);
  }

  @Test
  public void serialize() throws Exception {
    assertMapContents(serializeAndDeserialize(createTestMap()));
  }

  @Test
  public void serializeDelegate() throws Exception {
    assertMapContents(serializeAndDeserialize(createTestMap().clone()));
  }

  @Test
  public void serializeCopiedDelegate() throws Exception {
    Map<String, Object> map = createTestMap().clone();
    map.put("new", "val");
    assertMapContents(serializeAndDeserialize(map));
  }

  protected Map<String, Object> serializeAndDeserialize(Map<String, Object> map) {
    byte[] bytes = SerializationUtils.serialize((Serializable) map);
    return (Map) SerializationUtils.deserialize(bytes);
  }

  /*
   * Assert that Map created with createTestMap() has both of the original properties (FOO and DOO) and that these can be accessed
   * using case-insensitive keys
   */
  protected void assertMapContents(Map<String, Object> map) {
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
  protected CopyOnWriteCaseInsensitiveMap<String, Object> createTestMap() {
    CopyOnWriteCaseInsensitiveMap<String, Object> map = new CopyOnWriteCaseInsensitiveMap<String, Object>();
    map.put(KEY1, "BAR");
    map.put(KEY2, Integer.valueOf(3));
    return map;
  }

  @Test
  public void entrySet() throws Exception {
    CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
    assertEquals(2, original.size());
    original.keySet().remove("FOO");
    assertEquals(1, original.size());
    original.keySet().clear();
    assertEquals(0, original.size());
  }

  @Test
  public void entrySetCloneMutateOriginal() throws Exception {
    CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
    Map<String, Object> copyOnWriteMap = original.clone();

    assertEquals(2, original.size());
    assertEquals(2, copyOnWriteMap.size());

    original.keySet().remove("FOO");
    assertEquals(1, original.size());
    assertEquals(2, copyOnWriteMap.size());

    original.keySet().clear();
    assertEquals(0, original.size());
    assertEquals(2, copyOnWriteMap.size());
  }

  @Test
  public void entrySetCloneMutateClone() throws Exception {
    CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
    Map<String, Object> copyOnWriteMap = original.clone();

    assertEquals(2, original.size());
    assertEquals(2, copyOnWriteMap.size());

    copyOnWriteMap.keySet().remove("FOO");
    assertEquals(2, original.size());
    assertEquals(1, copyOnWriteMap.size());

    copyOnWriteMap.keySet().clear();
    assertEquals(2, original.size());
    assertEquals(0, copyOnWriteMap.size());
  }

  @Test
  public void putClone() throws Exception {
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
  public void putAllClone() throws Exception {
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
  public void removeClone() throws Exception {
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
  public void clearOrignalClone() throws Exception {
    CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
    Map<String, Object> copyOnWriteMap = original.clone();

    original.clear();
    assertEquals(0, original.size());
    assertEquals(0, original.entrySet().size());
    assertEquals(2, copyOnWriteMap.size());
    assertEquals(2, copyOnWriteMap.entrySet().size());
  }

  @Test
  public void clearCopyClone() throws Exception {
    CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
    Map<String, Object> copyOnWriteMap = original.clone();

    copyOnWriteMap.clear();
    assertEquals(2, original.size());
    assertEquals(2, original.entrySet().size());
    assertEquals(0, copyOnWriteMap.size());
    assertEquals(0, copyOnWriteMap.entrySet().size());
  }

  @Test
  public void entrySetDeserializedMutateOriginal() throws Exception {
    CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
    Map<String, Object> copyOnWriteMap = serializeAndDeserialize(original);

    assertEquals(2, original.size());
    assertEquals(2, copyOnWriteMap.size());

    original.keySet().remove("FOO");
    assertEquals(1, original.size());
    assertEquals(2, copyOnWriteMap.size());

    original.keySet().clear();
    assertEquals(0, original.size());
    assertEquals(2, copyOnWriteMap.size());
  }

  @Test
  public void entrySetDeserializedMutateClone() throws Exception {
    CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
    Map<String, Object> copyOnWriteMap = serializeAndDeserialize(original);

    assertEquals(2, original.size());
    assertEquals(2, copyOnWriteMap.size());

    copyOnWriteMap.keySet().remove("FOO");
    assertEquals(2, original.size());
    assertEquals(1, copyOnWriteMap.size());

    copyOnWriteMap.keySet().clear();
    assertEquals(2, original.size());
    assertEquals(0, copyOnWriteMap.size());
  }

  @Test
  public void putDeserialized() throws Exception {
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
  public void putAllDeserialized() throws Exception {
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
  public void removeDeserialized() throws Exception {
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
  public void clearOrignalDeserialized() throws Exception {
    CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
    Map<String, Object> copyOnWriteMap = serializeAndDeserialize(original);

    original.clear();
    assertEquals(0, original.size());
    assertEquals(0, original.entrySet().size());
    assertEquals(2, copyOnWriteMap.size());
    assertEquals(2, copyOnWriteMap.entrySet().size());
  }

  @Test
  public void clearCopyDeserialized() throws Exception {
    CopyOnWriteCaseInsensitiveMap<String, Object> original = createTestMap();
    Map<String, Object> copyOnWriteMap = serializeAndDeserialize(original);

    copyOnWriteMap.clear();
    assertEquals(2, original.size());
    assertEquals(2, original.entrySet().size());
    assertEquals(0, copyOnWriteMap.size());
    assertEquals(0, copyOnWriteMap.entrySet().size());
  }

  @Test
  public void keySetGivesAllKeys() throws Exception {
    CopyOnWriteCaseInsensitiveMap<String, Object> map = createTestMap();
    Set<String> foundKeys = new HashSet<String>();
    for (String key : map.keySet()) {
      assertTrue(KEY1.equals(key) || KEY2.equals(key));
      assertFalse(foundKeys.contains(key));
      foundKeys.add(key);
    }
  }

  @Test
  public void removeKeySetItem() throws Exception {
    CopyOnWriteCaseInsensitiveMap<String, Object> map = createTestMap();
    Iterator<String> it = map.keySet().iterator();

    assertTrue(it.hasNext());
    String key = it.next();
    it.remove();

    assertFalse(map.keySet().contains(key));
    it = map.keySet().iterator();
    assertTrue(it.hasNext());
    String key2 = it.next();
    assertFalse(key.equals(key2));
    assertFalse(it.hasNext());
    it.remove();
    assertFalse(it.hasNext());

    assertTrue(map.keySet().isEmpty());
    it = map.keySet().iterator();
    assertFalse(it.hasNext());

    try {
      it.next();
      fail("Was expecting NoSuchElementException");
    } catch (NoSuchElementException e) {
      // happyness
    }
  }

  @Test(expected = NoSuchElementException.class)
  public void emptyMapKeySetIterator() throws Exception {
    Map<String, String> map = new CopyOnWriteCaseInsensitiveMap<String, String>();
    assertTrue(map.keySet().isEmpty());
    Iterator<String> iterator = map.keySet().iterator();
    assertFalse(iterator.hasNext());
    iterator.next();
  }

  @Test(expected = IllegalStateException.class)
  public void removeInKeySetIteratorBeforeAnyNext() throws Exception {
    createTestMap().keySet().iterator().remove();
  }

  @Test(expected = IllegalStateException.class)
  public void keySetIteratorWithTwoRemovesInTheSameNext() throws Exception {
    Iterator<String> iterator = createTestMap().keySet().iterator();
    iterator.next();
    iterator.remove();
    iterator.remove();
  }

  @Test
  public void removeShouldNotMoveForward() throws Exception {
    CopyOnWriteCaseInsensitiveMap<String, Object> map = createTestMap();

    // add a third element to spice things up
    final String EXTRA_KEY = "THIRD";
    map.put(EXTRA_KEY, new Object());

    // store iteration order
    List<String> keys = new ArrayList<String>();
    for (String key : map.keySet()) {
      keys.add(key);
    }

    // now remove second element and make sure that the next element is the third
    Iterator<String> iterator = map.keySet().iterator();
    iterator.next();
    iterator.next();
    iterator.remove();
    String key = iterator.next();
    assertEquals(keys.get(2), key);
    assertFalse(iterator.hasNext());
  }

  @Test
  public void asHashMap() {
    CopyOnWriteCaseInsensitiveMap<String, Object> map = createTestMap();
    Map<String, Object> regularMap = map.asHashMap();
    assertThat(regularMap, is(instanceOf(HashMap.class)));
    assertThat(regularMap.size(), is(map.size()));
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      assertThat(map.get(entry.getKey()), is(regularMap.get(entry.getKey())));
    }
  }
}
