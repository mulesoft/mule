/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.el.context;

import org.mule.api.annotation.NoExtend;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.DefaultMapEntry;

@NoExtend
public abstract class AbstractMapContext<V> implements Map<String, V> {

  @Override
  public V get(Object key) {
    if (!(key instanceof String)) {
      return null;
    }
    V value = null;
    try {
      value = doGet((String) key);
    } catch (NoSuchElementException nsse) {
      // Ignore
    }
    return value;
  }

  protected abstract V doGet(String key);

  @Override
  public V remove(Object key) {
    if (!(key instanceof String)) {
      return null;
    }
    V previousValue = get(key);
    doRemove((String) key);
    return previousValue;
  }

  protected abstract void doRemove(String key);

  @Override
  public V put(String key, V value) {
    V previousValue = null;
    try {
      previousValue = doGet(key);
    } catch (NoSuchElementException nsee) {
      // Ignore
    }
    doPut(key, value);
    return previousValue;
  }

  protected abstract void doPut(String key, V value);

  @Override
  public void putAll(Map<? extends String, ? extends V> m) {
    for (Entry<? extends String, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public int size() {
    return keySet().size();
  }

  @Override
  public boolean isEmpty() {
    return keySet().isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return keySet().contains(key);
  }

  @Override
  public Collection<V> values() {
    List<V> values = new ArrayList<>(size());
    for (String key : keySet()) {
      values.add(get(key));
    }
    return values;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<Entry<String, V>> entrySet() {
    Set<Entry<String, V>> entrySet = new HashSet<>();
    for (String key : keySet()) {
      entrySet.add(new DefaultMapEntry(key, get(key)));
    }
    return entrySet;
  }

  @Override
  public boolean containsValue(Object value) {
    for (String key : keySet()) {
      if (value.equals(get(key))) {
        return true;
      }
    }
    return false;
  }

}
