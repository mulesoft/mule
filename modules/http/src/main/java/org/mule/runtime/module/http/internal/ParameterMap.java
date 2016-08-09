/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal;

import static java.util.Collections.unmodifiableMap;

import org.mule.runtime.module.http.api.HttpParameters;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link HttpParameters} that allows the aggregation of keys and access to the aggregated list or a single
 * value (the last).
 */
public class ParameterMap implements HttpParameters, Serializable {

  protected Map<String, LinkedList<String>> paramsMap;

  public ParameterMap(final Map paramsMap) {
    this.paramsMap = unmodifiableMap(paramsMap);
  }

  public ParameterMap() {
    this.paramsMap = new LinkedHashMap();
  }

  public ParameterMap toImmutableParameterMap() {
    return new ParameterMap(this.paramsMap);
  }

  @Override
  public int size() {
    return paramsMap.size();
  }

  @Override
  public boolean isEmpty() {
    return paramsMap.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return paramsMap.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return paramsMap.containsValue(value);
  }

  @Override
  public String get(Object key) {
    final Object value = paramsMap.get(key);
    if (value != null) {
      LinkedList<String> values = (LinkedList<String>) value;
      return values.getLast();
    }
    return null;
  }

  public List<String> getAll(String key) {
    return paramsMap.containsKey(key) ? Collections.unmodifiableList(paramsMap.get(key)) : Collections.<String>emptyList();
  }

  @Override
  public String put(String key, String value) {
    LinkedList<String> previousValue = paramsMap.get(key);
    LinkedList<String> newValue = previousValue;
    if (previousValue != null) {
      previousValue = new LinkedList<>(previousValue);
    } else {
      newValue = new LinkedList<>();
    }
    newValue.add(value);
    paramsMap.put(key, newValue);
    if (previousValue == null || previousValue.isEmpty()) {
      return null;
    }
    return previousValue.getFirst();
  }

  public void put(String key, Collection<String> values) {
    LinkedList<String> newValue = paramsMap.get(key);
    if (newValue == null) {
      newValue = new LinkedList<>();
    }
    newValue.addAll(values);
    paramsMap.put(key, newValue);
  }

  public String remove(Object key) {
    Collection<String> values = paramsMap.remove(key);
    if (values != null) {
      return values.iterator().next();
    }
    return null;
  }

  @Override
  public void putAll(Map<? extends String, ? extends String> aMap) {
    for (String key : aMap.keySet()) {
      LinkedList<String> values = new LinkedList<>();
      values.add(aMap.get(key));
      paramsMap.put(key, values);
    }
  }

  @Override
  public void clear() {
    paramsMap.clear();
  }

  @Override
  public Set<String> keySet() {
    return paramsMap.keySet();
  }

  @Override
  public Collection<String> values() {
    ArrayList<String> values = new ArrayList<>();
    for (String key : paramsMap.keySet()) {
      values.add(paramsMap.get(key).getLast());
    }
    return values;
  }

  @Override
  public Set<Entry<String, String>> entrySet() {
    HashSet<Entry<String, String>> entries = new HashSet<>();
    for (String key : paramsMap.keySet()) {
      entries.add(new AbstractMap.SimpleEntry<>(key, paramsMap.get(key).getLast()));
    }
    return entries;
  }

  @Override
  public boolean equals(Object o) {
    return paramsMap.equals(o);
  }

  @Override
  public int hashCode() {
    return paramsMap.hashCode();
  }

  public Map<String, ? extends List<String>> toListValuesMap() {
    return unmodifiableMap(paramsMap);
  }

  @Override
  public String toString() {
    return "ParameterMap{" + Arrays.toString(paramsMap.entrySet().toArray()) + '}';
  }
}
