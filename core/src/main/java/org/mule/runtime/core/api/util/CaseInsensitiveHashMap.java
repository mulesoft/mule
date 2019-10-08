/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static java.util.Collections.unmodifiableMap;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.util.CaseInsensitiveMapWrapper;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A case-insensitive <code>Map</code>.
 * <p/>
 * As entries are added to the map, keys hash values are lowercase hash codes of the key. the Real key case is preserved.
 * <p/>
 * <p/>
 * The <code>keySet()</code> method returns all keys in their original case
 * <p/>
 * <strong>Note that CaseInsensitiveMap is not synchronized and is not thread-safe.</strong> If you wish to use this map from
 * multiple threads concurrently, you must use appropriate synchronization. The simplest approach is to wrap this map using
 * {@link java.util.Collections#synchronizedMap(Map)}. This class may throw exceptions when accessed by concurrent threads without
 * synchronization.
 *
 * @since 3.0.0
 */
@NoExtend
public class CaseInsensitiveHashMap<K, V> implements Map<K, V>, Serializable {

  /**
   * Serialisation version
   */
  private static final long serialVersionUID = -7074633917369299456L;

  @SuppressWarnings("rawtypes")
  private static final CaseInsensitiveHashMap EMPTY_MAP = new CaseInsensitiveHashMap<>().toImmutableCaseInsensitiveMap();

  /**
   * Returns an empty CaseInsensitiveHashMap (immutable). This map is serializable.
   *
   * <p>
   * This example illustrates the type-safe way to obtain an empty map:
   *
   * <pre>
   *
   * CaseInsensitiveHashMap&lt;String, String&gt; s = CaseInsensitiveHashMap.emptyCaseInsensitiveMap();
   * </pre>
   *
   * @param <K> the class of the map keys
   * @param <V> the class of the map values
   * @return an empty multi-map
   * @since 1.1.1
   */
  @SuppressWarnings("unchecked")
  public static <K, V> CaseInsensitiveHashMap<K, V> emptyCaseInsensitiveMap() {
    return EMPTY_MAP;
  }

  protected Map<K, V> delegate;

  /**
   * Constructs a new empty map with default size and load factor.
   */
  public CaseInsensitiveHashMap() {
    delegate = new CaseInsensitiveMapWrapper();
  }

  /**
   * Constructor copying elements from another map.
   * <p/>
   * Keys will be converted to lower case strings, which may cause some entries to be removed (if string representation of keys
   * differ only by character case).
   *
   * @param map the map to copy
   * @throws NullPointerException if the map is null
   */
  public CaseInsensitiveHashMap(Map map) {
    delegate = new CaseInsensitiveMapWrapper();
    delegate.putAll(map);
  }

  // -----------------------------------------------------------------------

  /**
   * Clones the map without cloning the keys or values.
   *
   * @return a shallow clone
   */
  @Override
  public Object clone() {
    return new CaseInsensitiveHashMap<K, V>(delegate);
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return delegate.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return delegate.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return delegate.get(key);
  }

  @Override
  public V put(K key, V value) {
    return delegate.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return delegate.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> values) {
    delegate.putAll(values);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public Set<K> keySet() {
    return delegate.keySet();
  }

  @Override
  public Collection<V> values() {
    return delegate.values();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return delegate.entrySet();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  /**
   * @return an immutable version of this map.
   *
   * @since 4.1.5
   */
  public CaseInsensitiveHashMap<K, V> toImmutableCaseInsensitiveMap() {
    if (this.isEmpty() && EMPTY_MAP != null) {
      return EMPTY_MAP;
    }
    return new ImmutableCaseInsensitiveHashMap<>(this);
  }

  private static class ImmutableCaseInsensitiveHashMap<K, V> extends CaseInsensitiveHashMap<K, V> {

    private ImmutableCaseInsensitiveHashMap(CaseInsensitiveHashMap<K, V> caseInsensitiveHashMap) {
      super(caseInsensitiveHashMap);
      this.delegate = unmodifiableMap(delegate);
    }

    @Override
    public CaseInsensitiveHashMap<K, V> toImmutableCaseInsensitiveMap() {
      return this;
    }
  }
}
