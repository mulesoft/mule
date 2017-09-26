/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.el.context.AbstractArtifactContext;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.el.context.AbstractMapContext;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link AbstractArtifactContext} for exposing MVEL EL artifact context.
 *
 * @since 4.0
 */
public class MVELArtifactContext extends AbstractArtifactContext {

  public MVELArtifactContext(MuleContext muleContext) {
    super(muleContext, null);
  }

  @Override
  public Map<String, Object> getRegistry() {
    return new RegistryWrapperMap(((MuleContextWithRegistries) muleContext).getRegistry());
  }

  @Override
  protected Map<String, Object> createRegistry(Registry registry) {
    return new RegistryWrapperMap(((MuleContextWithRegistries) muleContext).getRegistry());
  }

  protected static class RegistryWrapperMap extends AbstractMapContext<Object> {

    private MuleRegistry registry;

    public RegistryWrapperMap(MuleRegistry registry) {
      this.registry = registry;
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {
      return get(key) != null;
    }

    @Override
    public Object doGet(String key) {
      return registry.get(key);
    }

    @Override
    public void doPut(String key, Object value) {
      try {
        registry.registerObject(key, value);
      } catch (RegistrationException e) {
        throw new MuleRuntimeException(e);
      }
    }

    @Override
    public void doRemove(String key) {
      Object value = registry.lookupObject(key);
      if (value != null) {
        try {
          registry.unregisterObject(key);
        } catch (RegistrationException e) {
          throw new MuleRuntimeException(e);
        }
      }
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public Set<String> keySet() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
      throw new UnsupportedOperationException();
    }

  }

}
