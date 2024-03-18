/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.collections4.keyvalue.DefaultMapEntry;

public class OrderedProperties extends Properties {

  private static final long serialVersionUID = -3611415251568805458L;

  private final Vector<Object> keys = new Vector<>();

  @Override
  @SuppressWarnings("unchecked")
  public Set<Map.Entry<Object, Object>> entrySet() {
    Set<Map.Entry<Object, Object>> entries = new LinkedHashSet<>();
    Enumeration<?> keys = this.propertyNames();

    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      entries.add(new DefaultMapEntry(key, this.getProperty((String) key)));
    }

    return entries;
  }

  @Override
  public Enumeration<?> propertyNames() {
    return this.keys.elements();
  }

  @Override
  public Object put(Object key, Object value) {
    if (this.keys.contains(key)) {
      this.keys.remove(key);
    }

    this.keys.add(key);

    return super.put(key, value);
  }

  @Override
  public Object remove(Object key) {
    this.keys.remove(key);
    return super.remove(key);
  }
}
