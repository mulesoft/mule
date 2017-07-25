/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.MuleRegistry;

import java.util.Map;
import java.util.Set;

/**
 * Exposes information about the current Mule Application:
 * <li><b>encoding</b> <i>Application default encoding</i>
 * <li><b>name</b> <i>Application name</i>
 * <li><b>registry</b> <i>Mule registry (as a map)</i>
 * <li><b>standalone</b> <i>If Mule is running standalone</i>
 * <li><b>workdir</b> <i>Application work directory</i>
 */
public class DataWeaveAppContext extends AbstractAppContext {

  public DataWeaveAppContext(MuleContext muleContext) {
    super(muleContext);
  }

  @Override
  protected Map<String, Object> createRegistry(MuleRegistry registry) {
    return new RegistryWrapperMap(registry.get(OBJECT_REGISTRY));
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
