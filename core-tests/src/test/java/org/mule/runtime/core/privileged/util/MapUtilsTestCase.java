/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util;

import static java.lang.System.lineSeparator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.junit.Test;

@SmallTest
public class MapUtilsTestCase extends AbstractMuleTestCase {

  @Test
  public void testMapCreationNullClass() {
    try {
      MapUtils.mapWithKeysAndValues(null, (String[]) null, (String[]) null);
      fail();
    } catch (IllegalArgumentException ex) {
      // expected
    }
  }

  @Test
  public void testMapCreationWithoutElements() {
    Map m = MapUtils.mapWithKeysAndValues(HashMap.class, (List) null, (List) null);
    assertTrue(m.isEmpty());
  }

  @Test
  public void testCaseInsensitiveMapCreation() {
    List strings = Arrays.asList(new String[] {"foo"});

    Map m = MapUtils.mapWithKeysAndValues(CaseInsensitiveMap.class, strings.iterator(), strings.iterator());

    assertEquals("foo", m.get("foo"));
    assertEquals("foo", m.get("Foo"));
    assertEquals("foo", m.get("FOO"));
  }

  @Test
  public void testToStringNull() throws Exception {
    Map props = null;
    assertEquals("{}", MapUtils.toString(props, false));
    assertEquals("{}", MapUtils.toString(props, true));
  }

  @Test
  public void testToStringEmpty() throws Exception {
    Map props = new HashMap();
    assertEquals("{}", MapUtils.toString(props, false));
    assertEquals("{}", MapUtils.toString(props, true));
  }

  @Test
  public void testToStringSingleElement() throws Exception {
    Map props = MapUtils.mapWithKeysAndValues(HashMap.class, new Object[] {"foo"}, new Object[] {"bar"});

    assertEquals("{foo=bar}", MapUtils.toString(props, false));
    assertEquals("{" + lineSeparator() + "foo=bar" + lineSeparator() + "}", MapUtils.toString(props, true));
  }

  @Test
  public void testToStringMultipleElements() throws Exception {
    Map props = MapUtils.mapWithKeysAndValues(HashMap.class, new Object[] {"foo", "foozle"}, new Object[] {"bar", "doozle"});

    String result = MapUtils.toString(props, false);
    assertTrue(result.indexOf("foo=bar") != -1);
    assertTrue(result.indexOf("foozle=doozle") != -1);

    result = MapUtils.toString(props, true);
    assertTrue(result.startsWith("{" + lineSeparator()));
    assertTrue(result.indexOf("foo=bar") != -1);
    assertTrue(result.indexOf("foozle=doozle") != -1);
    assertTrue(result.endsWith(lineSeparator() + "}"));
  }

}
