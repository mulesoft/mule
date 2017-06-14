/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

@SmallTest
public class CaseInsensitiveHashMapTestCase extends AbstractMuleTestCase {

  protected Map createTestMap() {
    Map map = new CaseInsensitiveHashMap();
    map.put("FOO", "BAR");
    map.put("doo", Integer.valueOf(3));
    return map;
  }

  @Test
  public void keysCaseSensitive() throws Exception {
    Map<String, Object> map = createTestMap();

    assertEquals(2, map.keySet().size());
    assertTrue(map.keySet().toArray()[0].equals("FOO") || map.keySet().toArray()[0].equals("doo"));
    assertTrue(map.keySet().toArray()[1].equals("FOO") || map.keySet().toArray()[1].equals("doo"));
  }

  @Test
  public void testMap() throws Exception {
    Map map = createTestMap();
    doTestMap(map);
  }

  @Test
  public void testMapSerialization() throws Exception {
    Map map = createTestMap();
    doTestMap(map);

    byte[] bytes = SerializationUtils.serialize((Serializable) map);
    Map resultMap = (Map) SerializationUtils.deserialize(bytes);
    doTestMap(resultMap);
  }

  public void doTestMap(Map map) throws Exception {
    assertEquals("BAR", map.get("FOO"));
    assertEquals("BAR", map.get("foo"));
    assertEquals("BAR", map.get("Foo"));

    assertEquals(Integer.valueOf(3), map.get("DOO"));
    assertEquals(Integer.valueOf(3), map.get("doo"));
    assertEquals(Integer.valueOf(3), map.get("Doo"));

  }
}
