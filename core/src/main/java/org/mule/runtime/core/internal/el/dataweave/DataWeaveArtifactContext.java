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
import org.mule.runtime.core.privileged.el.context.AbstractMapContext;

import java.util.Map;
import java.util.Set;

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
  protected static class RegistryWrapperMap extends AbstractMapContext<Object> {

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
    public Object doGet(String key) {
      return registry.lookupByName(key);
    }

    @Override
    public void doPut(String key, Object value) {
      throw new UnsupportedOperationException("Registry does not support put");
    }

    @Override
    public void doRemove(String key) {
      throw new UnsupportedOperationException("Registry does not support remove");
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
