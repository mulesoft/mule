/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.util;

import static junit.framework.Assert.assertEquals;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

@SmallTest
public class OrderedPropertiesTestCase {

  private final int count = 100;

  private Properties properties;
  private List<String> keys;
  private List<String> values;

  @Before
  public void setUp() {
    this.properties = new OrderedProperties();
    this.keys = new ArrayList<>();
    this.values = new ArrayList<>();

    for (int i = 0; i < count; i++) {
      String key = this.random();
      String value = this.random();
      this.keys.add(key);
      this.values.add(value);

      this.properties.setProperty(key, value);
    }
  }

  @Test
  public void orderedEntrySet() {
    int i = 0;
    for (Map.Entry<Object, Object> entry : this.properties.entrySet()) {
      assertEquals(this.keys.get(i), entry.getKey());
      assertEquals(this.values.get(i), entry.getValue());
      i++;
    }
  }

  @Test
  public void orderedPropertyNames() {
    int i = 0;
    Enumeration<?> propertyNames = this.properties.propertyNames();
    while (propertyNames.hasMoreElements()) {
      Object key = propertyNames.nextElement();
      assertEquals(this.keys.get(i), key);
      i++;
    }
  }

  private String random() {
    return RandomStringUtils.randomAlphabetic(10);
  }

}
