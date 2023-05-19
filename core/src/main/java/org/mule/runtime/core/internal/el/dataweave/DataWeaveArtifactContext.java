/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.dataweave;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.el.context.AbstractArtifactContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.DefaultMapEntry;

/**
 * Implementation of {@link AbstractArtifactContext} for exposing DataWeave EL artifact context.
 *
 * @since 4.0
 */
public class DataWeaveArtifactContext extends AbstractArtifactContext {

  public DataWeaveArtifactContext(MuleContext muleContext, Registry registry) {
    super(muleContext, registry);
  }

  @Override
  protected Map<String, Object> createRegistry(Registry registry) {
    return new RegistryWrapperMap(registry);
  }

  /**
   * Context for exposing the {@link Registry} in DataWeave EL.
   */
  protected static class RegistryWrapperMap implements Map<String, Object> {

    private Registry registry;

    public RegistryWrapperMap(Registry registry) {
      this.registry = registry;
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException("Registry does not support clear");
    }

    @Override
    public boolean containsKey(Object key) {
      return get(key) != null;
    }

    @Override
    public Object get(Object key) {
      if (!(key instanceof String)) {
        return null;
      }
      Object value = null;
      try {
        value = registry.lookupByName((String) key).orElse(null);
      } catch (NoSuchElementException nsse) {
        // Ignore
      }
      return value;
    }

    @Override
    public Object put(String key, Object value) {
      throw new UnsupportedOperationException("Registry does not support put");
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
      for (Entry<? extends String, ? extends Object> entry : m.entrySet()) {
        put(entry.getKey(), entry.getValue());
      }
    }

    @Override
    public Object remove(Object key) {
      throw new UnsupportedOperationException("Registry does not support remove");
    }

    @Override
    public Collection<Object> values() {
      List<Object> values = new ArrayList<>(size());
      for (String key : keySet()) {
        values.add(get(key));
      }
      return values;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Entry<String, Object>> entrySet() {
      Set<Entry<String, Object>> entrySet = new HashSet<>();
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

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public Set<String> keySet() {
      throw new UnsupportedOperationException("Registry does not support keySet");
    }

    @Override
    public int size() {
      throw new UnsupportedOperationException("Registry does not support size");
    }

  }

}
