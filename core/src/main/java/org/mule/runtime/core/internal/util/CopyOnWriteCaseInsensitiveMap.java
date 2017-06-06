/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import org.mule.runtime.core.api.util.CaseInsensitiveHashMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Implementation of {@link Map} that provides copy on write semantics while providing the case-insensitivity of
 * {@link CaseInsensitiveHashMap}. <br>
 * <b>Note:</b> In this {@link Map} implementation {@link #values()} and {@link #entrySet()} return unmodifiable
 * {@link Collection}'s.<br>
 * This implementation is not thread-safe.
 */
public class CopyOnWriteCaseInsensitiveMap<K, V> implements Map<K, V>, Serializable {

  private static final long serialVersionUID = -2753436627413265538L;

  private Map<K, V> core;
  private transient Map<K, V> view;
  private transient boolean requiresCopy;
  private transient Set<K> keyset = new KeySet();

  @SuppressWarnings("unchecked")
  public CopyOnWriteCaseInsensitiveMap() {
    updateCore(new CaseInsensitiveHashMap());
  }

  public CopyOnWriteCaseInsensitiveMap(Map<K, V> that) {
    if (that instanceof CopyOnWriteCaseInsensitiveMap) {
      updateCore(((CopyOnWriteCaseInsensitiveMap) that).core);
    } else {
      updateCore(that);
    }
    this.requiresCopy = true;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public CopyOnWriteCaseInsensitiveMap<K, V> clone() {
    try {
      return new CopyOnWriteCaseInsensitiveMap(this);
    } finally {
      requiresCopy = true;
    }
  }

  @SuppressWarnings("unchecked")
  private void copy() {
    if (requiresCopy) {
      updateCore(new CaseInsensitiveHashMap(core));
      requiresCopy = false;
    }
  }

  @Override
  public int size() {
    return core.size();
  }

  @Override
  public boolean isEmpty() {
    return core.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return core.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return core.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return core.get(key);
  }

  @Override
  public V put(K key, V value) {
    copy();
    return core.put(key, value);
  }

  @Override
  public V remove(Object key) {
    copy();
    return core.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> t) {
    copy();
    core.putAll(t);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void clear() {
    updateCore(new CaseInsensitiveHashMap());
  }

  public Set<K> keySet() {
    return keyset;
  }

  /**
   * Returns a copy of {@code this} map which is case insensitive but doesn't have the copy-on-write functionality.
   * <p/>
   * This method is useful in which you need a copy of this map in which the assumption of reads vastly outnumbering writes is no
   * longer true
   *
   * @return a case insensitive {@link Map} with a copy of {@code this} map's entries
   */
  public Map<K, V> asHashMap() {
    return new HashMap<>(view);
  }

  private final class KeySet extends AbstractSet<K> {

    public Iterator<K> iterator() {
      return new KeyIterator();
    }

    public int size() {
      return CopyOnWriteCaseInsensitiveMap.this.size();
    }

    public boolean contains(Object o) {
      return containsKey(o);
    }

    public boolean remove(Object o) {
      return CopyOnWriteCaseInsensitiveMap.this.remove(o) != null;
    }

    public void clear() {
      CopyOnWriteCaseInsensitiveMap.this.clear();
    }
  }

  private final class KeyIterator implements Iterator<K> {

    private int current = -1;
    private int lastRemovalIndex = current;
    private K[] keyArray;

    @SuppressWarnings("unchecked")
    public KeyIterator() {
      keyArray = (K[]) core.keySet().toArray();
    }

    public boolean hasNext() {
      return current < keyArray.length - 1;
    }

    public K next() {
      try {
        return keyArray[++current];
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new NoSuchElementException();
      }
    }

    public void remove() {
      if (current == -1) {
        throw new IllegalStateException("Cannot remove element before first invoking next()");
      }

      if (current == lastRemovalIndex) {
        throw new IllegalStateException("Remove can only be called once per call to next()");
      }

      CopyOnWriteCaseInsensitiveMap.this.remove(keyArray[current]);
      lastRemovalIndex = current;
    }
  }

  @Override
  public Collection<V> values() {
    return view.values();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return view.entrySet();
  }

  @Override
  public String toString() {
    return core.toString();
  }

  private void updateCore(Map<K, V> core) {
    this.core = core;
    this.view = Collections.unmodifiableMap(core);
  }

  /**
   * After deserialization we can just use unserialized original map directly.
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    this.view = Collections.unmodifiableMap(core);
    this.keyset = new KeySet();
  }

}
