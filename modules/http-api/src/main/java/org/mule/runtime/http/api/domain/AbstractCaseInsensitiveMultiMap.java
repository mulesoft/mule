/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.http.api.domain;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.el.DataTypeAware;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.util.MultiMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@NoExtend
public abstract class AbstractCaseInsensitiveMultiMap extends MultiMap<String, String> implements DataTypeAware {

  protected static final DataType dataType = DataType.builder()
      .mapType(CaseInsensitiveMultiMap.class)
      .keyType(String.class)
      .valueType(String.class)
      .build();

  @Override
  public DataType getDataType() {
    return dataType;
  }

  @Override
  public abstract AbstractCaseInsensitiveMultiMap toImmutableMultiMap();

  public static class UnmodifiableCaseInsensitiveMultiMap extends AbstractCaseInsensitiveMultiMap {

    private static final long serialVersionUID = 6798199484376351419L;

    private final AbstractCaseInsensitiveMultiMap m;

    public UnmodifiableCaseInsensitiveMultiMap(AbstractCaseInsensitiveMultiMap m) {
      this.m = m;
    }

    @Override
    public AbstractCaseInsensitiveMultiMap toImmutableMultiMap() {
      return m.toImmutableMultiMap();
    }

    @Override
    public int size() {
      return m.size();
    }

    @Override
    public boolean isEmpty() {
      return m.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
      return m.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      return m.containsValue(value);
    }

    @Override
    public String get(Object key) {
      return m.get(key);
    }

    @Override
    public List<String> getAll(Object key) {
      return m.getAll(key);
    }

    @Override
    public String put(String key, String value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void put(String key, Collection<String> values) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String remove(Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<String> removeAll(Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> aMap) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(MultiMap<? extends String, ? extends String> aMultiMap) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keySet() {
      return unmodifiableSet(m.keySet());
    }

    @Override
    public Collection<String> values() {
      return unmodifiableCollection(m.values());
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
      return unmodifiableSet(m.entrySet());
    }

    @Override
    public List<Entry<String, String>> entryList() {
      return unmodifiableList(m.entryList());
    }

    @Override
    public boolean equals(Object o) {
      return m.equals(o);
    }

    @Override
    public int hashCode() {
      return m.hashCode();
    }

    @Override
    public Map<String, ? extends List<String>> toListValuesMap() {
      return m.toListValuesMap();
    }

    @Override
    public String toString() {
      return m.toString();
    }

    @Override
    public String getOrDefault(Object key, String defaultValue) {
      return m.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super String> action) {
      m.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super String, ? extends String> function) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String putIfAbsent(String key, String value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object key, Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(String key, String oldValue, String newValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String replace(String key, String value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String computeIfAbsent(String key, Function<? super String, ? extends String> mappingFunction) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String computeIfPresent(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String compute(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String merge(String key, String value,
                        BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
      throw new UnsupportedOperationException();
    }
  }
}
