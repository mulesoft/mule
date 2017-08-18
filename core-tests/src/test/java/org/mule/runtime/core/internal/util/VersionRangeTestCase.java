/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.List;

import org.junit.Test;

public class VersionRangeTestCase extends AbstractMuleTestCase {

  @Test
  public void testSingleVersionRange() {
    VersionRange vr = new VersionRange("[1.5.0_11,1.6)");
    assertNotNull(vr);
    assertTrue(vr.isLowerBoundInclusive());
    assertFalse(vr.isUpperBoundInclusive());
    assertEquals("1.5.0_11", vr.getLowerVersion());
    assertEquals("1.6", vr.getUpperVersion());

    vr = new VersionRange("(1.5.0_11-b05,2.7.12]");
    assertNotNull(vr);
    assertFalse(vr.isLowerBoundInclusive());
    assertTrue(vr.isUpperBoundInclusive());
    assertEquals("1.5.0_11-b05", vr.getLowerVersion());
    assertEquals("2.7.12", vr.getUpperVersion());
  }

  @Test
  public void testCreateVersionRanges() {
    List<VersionRange> ranges = VersionRange.createVersionRanges("(,1.4.2),[1.5.0_11,1.6),[1.7,]");
    assertNotNull(ranges);
    assertEquals(3, ranges.size());

    VersionRange vr = ranges.get(0);
    assertFalse(vr.isLowerBoundInclusive());
    assertFalse(vr.isUpperBoundInclusive());
    assertEquals("", vr.getLowerVersion());
    assertEquals("1.4.2", vr.getUpperVersion());

    vr = ranges.get(1);
    assertTrue(vr.isLowerBoundInclusive());
    assertFalse(vr.isUpperBoundInclusive());
    assertEquals("1.5.0_11", vr.getLowerVersion());
    assertEquals("1.6", vr.getUpperVersion());

    vr = ranges.get(2);
    assertTrue(vr.isLowerBoundInclusive());
    assertTrue(vr.isUpperBoundInclusive());
    assertEquals("1.7", vr.getLowerVersion());
    assertEquals("", vr.getUpperVersion());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidDelimiter() {
    VersionRange range = new VersionRange("{1.3,1.4.2)");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingDelimiter() {
    VersionRange range = new VersionRange("1.3,1.4.2)");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidVersion() {
    VersionRange range = new VersionRange("[1.3,0,1.4.2)");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidCreateVersionRanges() {
    List<VersionRange> ranges = VersionRange.createVersionRanges("(,1.4.2),1.5.0_11,1.6),[1.7,]");
    for (VersionRange vr : ranges) {
      System.out.println(vr);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidDelimiterCreateVersionRanges() {
    List<VersionRange> ranges = VersionRange.createVersionRanges("(,1.4.2)|[1.5.0_11,1.6)|[1.7,]");
    for (VersionRange vr : ranges) {
      System.out.println(vr);
    }
  }
}
