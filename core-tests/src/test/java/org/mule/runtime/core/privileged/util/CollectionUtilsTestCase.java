/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util;

import static java.lang.System.lineSeparator;
import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

@SmallTest
public class CollectionUtilsTestCase extends AbstractMuleTestCase {

  @Test
  public void testToStringNull() throws Exception {
    Collection<?> c = null;
    assertEquals("[]", CollectionUtils.toString(c, false));
    assertEquals("[]", CollectionUtils.toString(c, true));
  }

  @Test
  public void testToStringEmpty() throws Exception {
    Collection<?> c = new ArrayList<>();
    assertEquals("[]", CollectionUtils.toString(c, false));
    assertEquals("[]", CollectionUtils.toString(c, true));
  }

  @Test
  public void testToStringSingleElement() throws Exception {
    Collection<String> c = Arrays.asList("foo");

    assertEquals("[foo]", CollectionUtils.toString(c, false));
    assertEquals("[" + lineSeparator() + "foo" + lineSeparator() + "]", CollectionUtils.toString(c, true));
  }

  @Test
  public void testToStringMultipleElements() throws Exception {
    Collection<Serializable> c = Arrays.asList("foo", this.getClass());

    assertEquals("[foo, " + this.getClass().getName() + "]", CollectionUtils.toString(c, false));

    assertEquals("[" + lineSeparator() + "foo" + lineSeparator() + this.getClass().getName()
        + lineSeparator() + "]", CollectionUtils.toString(c, true));
  }

  @Test
  public void testToStringTooManyElements() {
    Collection<Number> test = new ArrayList<>(100);
    for (int i = 0; i < 100; i++) {
      test.add(new Integer(i));
    }

    // the String will contain not more than exactly MAX_ARRAY_LENGTH elements
    String result = CollectionUtils.toString(test, 10);
    assertTrue(result.endsWith("[..]]"));
    assertEquals(9, countMatches(result, ","));
  }
}
