/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.runtime.core.internal.util.ArrayUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.junit.Test;

@SmallTest
public class StringUtilsTestCase extends AbstractMuleTestCase {

  @Test
  public void testSplitAndTrim1() {
    String[] result = StringUtils.splitAndTrim(null, ",,");
    assertNull(result);

    result = StringUtils.splitAndTrim("", ",");
    assertNotNull(result);
    assertTrue(Arrays.equals(ArrayUtils.EMPTY_STRING_ARRAY, result));

    result = StringUtils.splitAndTrim(" ", ",");
    assertNotNull(result);
    assertTrue(Arrays.equals(ArrayUtils.EMPTY_STRING_ARRAY, result));
  }

  @Test
  public void testSplitAndTrim2() {
    String[] inputValues = new String[] {"foo", "bar", "baz", "kaboom"};
    String inputString = new StringBuilder(40).append(inputValues[0]).append(" ,").append(",  ").append(inputValues[1])
        .append(" ,").append(inputValues[2]).append("  ,  ").append(inputValues[3]).append(" ").toString();

    assertTrue(Arrays.equals(inputValues, StringUtils.splitAndTrim(inputString, ",")));
  }

  @Test
  public void testSplitAndTrim3() {
    String[] inputValues = new String[] {"foo", "bar", "baz", "kaboom"};
    String inputString = "foo,  bar,\nbaz,  \nkaboom";
    assertTrue(Arrays.equals(inputValues, StringUtils.splitAndTrim(inputString, ",")));
  }

  @Test
  public void testHexStringToByteArray() {
    assertNull(StringUtils.hexStringToByteArray(null));

    try {
      StringUtils.hexStringToByteArray("1");
      fail();
    } catch (IllegalArgumentException iex) {
      // OK
    }

    assertTrue(Arrays.equals(new byte[] {}, StringUtils.hexStringToByteArray("")));
    assertTrue(Arrays.equals(new byte[] {1}, StringUtils.hexStringToByteArray("01")));
    assertTrue(Arrays.equals(new byte[] {1, 2}, StringUtils.hexStringToByteArray("0102")));
    assertTrue(Arrays.equals(new byte[] {10, 14}, StringUtils.hexStringToByteArray("0A0E")));
    assertTrue(Arrays.equals(new byte[] {10, 14}, StringUtils.hexStringToByteArray("0a0e")));
    assertTrue(Arrays.equals(new byte[] {10, (byte) 0xff}, StringUtils.hexStringToByteArray("0AFF")));
    assertTrue(Arrays.equals(new byte[] {10, (byte) 0xff}, StringUtils.hexStringToByteArray("0aff")));
  }

  @Test
  public void testByteArrayToHexString() {
    assertNull(StringUtils.toHexString(null));
    assertEquals("", StringUtils.toHexString(new byte[] {}));
    assertEquals("01", StringUtils.toHexString(new byte[] {1}));
    assertEquals("0102", StringUtils.toHexString(new byte[] {1, 2}));
    assertEquals("0a0e", StringUtils.toHexString(new byte[] {10, 14}));
    assertEquals("0A0E", StringUtils.toHexString(new byte[] {10, 14}, true));
    assertEquals("0aff", StringUtils.toHexString(new byte[] {10, (byte) 0xff}));
    assertEquals("0AFF", StringUtils.toHexString(new byte[] {10, (byte) 0xff}, true));
  }

  @Test
  public void testMatch() {
    Pattern pattern = Pattern.compile("<<([\\w]*)>>");
    String value = "<<target>>";

    assertEquals(StringUtils.match(pattern, value, 1), "target");

    try {
      StringUtils.match(pattern, null, 1);
      fail("was expecting IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }

    try {
      StringUtils.match(null, value, 1);
      fail("was expecting IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }

    assertNull(StringUtils.match(pattern, "hello world!", 1));
  }

}
