/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static org.apache.commons.lang3.StringUtils.split;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;

import org.junit.Test;

@SmallTest
public class ObjectUtilsTestCase extends AbstractMuleTestCase {

  @Test
  public void testIdentityToShortString() {
    assertEquals("null", ObjectUtils.identityToShortString(null));

    String source = "foo";
    String description = ObjectUtils.identityToShortString(source);
    String[] components = split(description, '@');

    assertNotNull(components);
    assertEquals(2, components.length);
    assertEquals("String", components[0]);
    assertEquals(Integer.toHexString(System.identityHashCode(source)), components[1]);
  }

  @Test
  public void testBooleanConversion() throws Exception {
    Object value = "true";
    assertTrue(ObjectUtils.getBoolean(value, false));

    value = "xyz";
    assertFalse(ObjectUtils.getBoolean(value, false));

    value = new Integer(6);
    assertTrue(ObjectUtils.getBoolean(value, false));

    value = new Integer(1);
    assertTrue(ObjectUtils.getBoolean(value, false));

    value = new Integer(0);
    assertFalse(ObjectUtils.getBoolean(value, false));

    value = new Integer(-41);
    assertFalse(ObjectUtils.getBoolean(value, false));

    value = null;
    assertFalse(ObjectUtils.getBoolean(value, false));
  }

  @Test
  public void testShortConversion() throws Exception {
    Object value = "123";
    assertEquals((short) 123, ObjectUtils.getShort(value, (short) -1));

    value = "xyz";
    assertEquals((short) -1, ObjectUtils.getShort(value, (short) -1));

    value = new Integer(6);
    assertEquals((short) 6, ObjectUtils.getShort(value, (short) -1));

    value = new Double(63.4);
    assertEquals((short) 63, ObjectUtils.getShort(value, (short) -1));

    value = new Float(-163.2);
    assertEquals((short) -163, ObjectUtils.getShort(value, (short) -1));

    value = null;
    assertEquals((short) -1, ObjectUtils.getShort(value, (short) -1));

  }

  @Test
  public void testByteConversion() throws Exception {
    Object value = "123";
    assertEquals((byte) 123, ObjectUtils.getByte(value, (byte) -1));

    value = "xyz";
    assertEquals((byte) -1, ObjectUtils.getByte(value, (byte) -1));

    value = new Integer(6);
    assertEquals((byte) 6, ObjectUtils.getByte(value, (byte) -1));

    value = new Double(63.4);
    assertEquals((byte) 63, ObjectUtils.getByte(value, (byte) -1));

    value = new Float(-163.2);
    assertEquals((byte) -163, ObjectUtils.getByte(value, (byte) -1));

    value = null;
    assertEquals((byte) -1, ObjectUtils.getByte(value, (byte) -1));
  }

  @Test
  public void testIntConversion() throws Exception {
    Object value = "123";
    assertEquals(123, ObjectUtils.getInt(value, -1));

    value = "xyz";
    assertEquals(-1, ObjectUtils.getInt(value, -1));

    value = new Integer(6);
    assertEquals(6, ObjectUtils.getInt(value, -1));

    value = new Double(63.4);
    assertEquals(63, ObjectUtils.getInt(value, -1));

    value = new Float(-163.2);
    assertEquals(-163, ObjectUtils.getInt(value, -1));

    value = null;
    assertEquals(-1, ObjectUtils.getInt(value, -1));
  }

  @Test
  public void testLongConversion() throws Exception {
    Object value = "123";
    assertEquals(123, ObjectUtils.getLong(value, -1l));

    value = "xyz";
    assertEquals(-1, ObjectUtils.getLong(value, -1l));

    value = new Integer(6);
    assertEquals(6, ObjectUtils.getLong(value, -1l));

    value = new Double(63.4);
    assertEquals(63, ObjectUtils.getLong(value, -1l));

    value = new Float(-163.2);
    assertEquals(-163, ObjectUtils.getLong(value, -1l));

    value = null;
    assertEquals(-1, ObjectUtils.getLong(value, -1l));
  }

  @Test
  public void testFloatConversion() throws Exception {
    Object value = "123.34";
    assertEquals(123.34, ObjectUtils.getFloat(value, -1f), 0.1f);

    value = "xyz";
    assertEquals(-1, ObjectUtils.getFloat(value, -1f), 0.1f);

    value = new Integer(6);
    assertEquals(6, ObjectUtils.getFloat(value, -1f), 0.1f);

    value = new Double(63.4);
    assertEquals(63.4, ObjectUtils.getFloat(value, -1f), 0.1f);

    value = new Float(-163.2);
    assertEquals(-163.2, ObjectUtils.getFloat(value, -1f), 0.1f);

    value = null;
    assertEquals(-1, ObjectUtils.getFloat(value, -1f), 0.1f);
  }

  @Test
  public void testDoubleConversion() throws Exception {
    Object value = "123.34";
    assertEquals(123.34, ObjectUtils.getDouble(value, -1.0), 0.1d);

    value = "xyz";
    assertEquals(-1, ObjectUtils.getDouble(value, -1.0), 0.1d);

    value = new Integer(6);
    assertEquals(6, ObjectUtils.getDouble(value, -1.0), 0.1d);

    value = new Double(63.4);
    assertEquals(63.4, ObjectUtils.getDouble(value, -1.0), 0.1d);

    value = new Float(-163.2);
    assertEquals(-163.2, ObjectUtils.getDouble(value, -1.0), 0.1d);

    value = null;
    assertEquals(-1, ObjectUtils.getDouble(value, -1.0), 0.1d);
  }

  @Test
  public void testStringConversion() throws Exception {
    Object value = "hello";
    assertEquals("hello", ObjectUtils.getString(value, "x"));

    value = new HashMap();
    assertEquals(new HashMap().toString(), ObjectUtils.getString(value, "x"));

    value = null;
    assertEquals("x", ObjectUtils.getString(value, "x"));
  }
}
