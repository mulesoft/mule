/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util;

import static org.mule.runtime.core.util.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

// @ThreadSafe
public class MapUtils extends org.apache.commons.collections.MapUtils {

  /**
   * Convenience method for CollectionUtil#mapWithKeysAndValues(Class, Iterator, Iterator); keys and values can be null or empty.
   */
  public static <K, V> Map<K, V> mapWithKeysAndValues(Class<? extends Map> mapClass, K[] keys, V[] values) {
    Collection<K> keyCollection = (keys != null ? Arrays.asList(keys) : Collections.EMPTY_LIST);
    Collection<V> valuesCollection = (values != null ? Arrays.asList(values) : Collections.EMPTY_LIST);
    return mapWithKeysAndValues(mapClass, keyCollection.iterator(), valuesCollection.iterator());
  }

  /**
   * Convenience method for CollectionUtil#mapWithKeysAndValues(Class, Iterator, Iterator); keys and values can be null or empty.
   */
  public static <K, V> Map<K, V> mapWithKeysAndValues(Class<? extends Map> mapClass, Collection<K> keys, Collection<V> values) {
    keys = (keys != null ? keys : Collections.EMPTY_LIST);
    values = (values != null ? values : Collections.EMPTY_LIST);
    return mapWithKeysAndValues(mapClass, keys.iterator(), values.iterator());
  }

  /**
   * Create & populate a Map of arbitrary class. Populating stops when either the keys or values iterator is null or exhausted.
   *
   * @param mapClass the Class of the Map to instantiate
   * @param keys iterator for Objects ued as keys
   * @param values iterator for Objects used as values
   * @return the instantiated Map
   */
  public static <K, V> Map<K, V> mapWithKeysAndValues(Class<? extends Map> mapClass, Iterator<K> keys, Iterator<V> values) {
    Map<K, V> m = null;

    if (mapClass == null) {
      throw new IllegalArgumentException("Map class must not be null!");
    }

    try {
      m = mapClass.newInstance();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    if (keys != null && values != null) {
      while (keys.hasNext() && values.hasNext()) {
        m.put(keys.next(), values.next());
      }
    }

    return m;
  }

  /**
   * Creates a String representation of the given Map, with optional newlines between elements.
   *
   * @param props the map to format
   * @param newline indicates whether elements are to be split across lines
   * @return the formatted String
   */
  public static String toString(Map props, boolean newline) {
    if (props == null || props.isEmpty()) {
      return "{}";
    }

    StringBuilder buf = new StringBuilder(props.size() * 32);
    buf.append('{');

    if (newline) {
      buf.append(SystemUtils.LINE_SEPARATOR);
    }

    Object[] entries = props.entrySet().toArray();
    int i;

    for (i = 0; i < entries.length - 1; i++) {
      Map.Entry<?, ?> property = (Map.Entry<?, ?>) entries[i];
      buf.append(property.getKey());
      buf.append('=');
      buf.append(PropertiesUtils.maskedPropertyValue(property));

      if (newline) {
        buf.append(SystemUtils.LINE_SEPARATOR);
      } else {
        buf.append(',').append(' ');
      }
    }

    // don't forget the last one
    Map.Entry<?, ?> lastProperty = (Map.Entry<?, ?>) entries[i];
    buf.append(lastProperty.getKey().toString());
    buf.append('=');
    buf.append(PropertiesUtils.maskedPropertyValue(lastProperty));

    if (newline) {
      buf.append(SystemUtils.LINE_SEPARATOR);
    }

    buf.append('}');
    return buf.toString();
  }

  /**
   * Puts the {@code key}/{@code value} pair into the given {@code map} only as long as the {@code key} is not already present on
   * the {@code map}. This method is not thread-safe per-se. If the {@code map} is a shared resource then it's up to the caller to
   * handle concurrency.
   *
   * @param map a {@link Map}
   * @param key the key
   * @param value the value
   * @param <K> the generic type of the key
   * @param <V> the generic type of the value
   * @throws IllegalStateException if the {@code map} already contains the {@code key}
   */
  public static <K, V> void idempotentPut(Map<K, V> map, K key, V value) {
    checkArgument(key != null, "key cannot be null");
    if (map.containsKey(key)) {
      throw new IllegalStateException(String.format("Key '%s' is already registered with value %s", key, map.get(key)));
    }

    map.put(key, value);
  }
}
