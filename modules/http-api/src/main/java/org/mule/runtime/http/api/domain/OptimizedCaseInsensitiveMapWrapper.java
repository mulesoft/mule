/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain;

import static java.util.stream.Collectors.toMap;

import org.mule.runtime.api.util.CaseInsensitiveMapWrapper;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Specialization of {@link CaseInsensitiveMapWrapper} where all map keys will be stored internally as lowercase. This means that
 * when calling {@link #keySet()} or other method that return the keys, they will be lowercase independently of the case they had
 * when they were put into the map.
 *
 * @param <T> The class of the values referenced in the map.
 *
 * @since 1.0
 */
public class OptimizedCaseInsensitiveMapWrapper<T> extends CaseInsensitiveMapWrapper<T> {

  /**
   * Creates a new instance using an existing map as backing map. Said map should be empty.
   *
   * @param map existing map
   */
  public OptimizedCaseInsensitiveMapWrapper(Map map) {
    super(map);
  }

  /**
   * Creates a new instance using a {@link HashMap} as backing map.
   */
  public OptimizedCaseInsensitiveMapWrapper() {
    super();
  }

  @Override
  public Set<String> keySet() {
    return new KeySet(baseMap.keySet());
  }

  @Override
  public Set<Entry<String, T>> entrySet() {
    return new EntrySet<>(baseMap.entrySet());
  }

  private static class KeySet extends AbstractConverterSet<CaseInsensitiveMapKey, String> {

    public KeySet(Set<CaseInsensitiveMapKey> keys) {
      super(keys);
    }

    @Override
    protected Iterator<String> createIterator(Set<CaseInsensitiveMapKey> keys) {
      return new KeyIterator(keys);
    }

    @Override
    protected CaseInsensitiveMapKey keyFor(Object o) {
      return CaseInsensitiveMapKey.keyFor(o);
    }
  }

  private static class EntrySet<T> extends AbstractConverterSet<Entry<CaseInsensitiveMapKey, T>, Entry<String, T>> {

    public EntrySet(Set<Map.Entry<CaseInsensitiveMapKey, T>> entries) {
      super(entries);
    }

    @Override
    protected Iterator<Entry<String, T>> createIterator(Set<Entry<CaseInsensitiveMapKey, T>> entries) {
      return new EntryIterator<>(entries);
    }

    @Override
    protected Entry<CaseInsensitiveMapKey, T> keyFor(Object o) {
      if (o instanceof Entry) {
        Entry<CaseInsensitiveMapKey, T> o2 = (Entry<CaseInsensitiveMapKey, T>) o;
        return new AbstractMap.SimpleImmutableEntry<>(CaseInsensitiveMapKey.keyFor(o2.getKey()), o2.getValue());
      }

      return null;
    }
  }

  private static class KeyIterator extends AbstractConverterIterator<CaseInsensitiveMapKey, String> {

    public KeyIterator(Set<CaseInsensitiveMapKey> keys) {
      super(keys);
    }

    @Override
    protected String convert(CaseInsensitiveMapKey next) {
      return next.getKeyLowerCase();
    }
  }

  private static class EntryIterator<T> extends AbstractConverterIterator<Entry<CaseInsensitiveMapKey, T>, Entry<String, T>> {

    public EntryIterator(Set<Map.Entry<CaseInsensitiveMapKey, T>> entries) {
      super(entries);
    }

    @Override
    protected Entry<String, T> convert(Entry<CaseInsensitiveMapKey, T> next) {
      return new AbstractMap.SimpleEntry<>(next.getKey().getKeyLowerCase(), next.getValue());
    }
  }

  private static abstract class AbstractConverterIterator<A, B> implements Iterator<B> {

    private final Iterator<A> aIterator;

    public AbstractConverterIterator(Set<A> set) {
      aIterator = set.iterator();
    }

    @Override
    public boolean hasNext() {
      return aIterator.hasNext();
    }

    @Override
    public final void remove() {
      aIterator.remove();
    }

    @Override
    public final B next() {
      return convert(aIterator.next());
    }

    protected abstract B convert(A next);
  }

  /**
   * Returns this map as a case sensitive map.
   *
   * @return case-sensitive map
   */
  @Override
  public Map<String, T> asCaseSensitiveMap() {
    return baseMap.entrySet().stream().collect(toMap(entry -> entry.getKey().getKey(), entry -> entry.getValue()));
  }

}
