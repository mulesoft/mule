/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.RegistrationException;

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
public class AppContext {

  protected MuleContext muleContext;

  public AppContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  public String getName() {
    return muleContext.getConfiguration().getId();
  }

  public String getWorkDir() {
    return muleContext.getConfiguration().getWorkingDirectory();
  }

  public String getEncoding() {
    return muleContext.getConfiguration().getDefaultEncoding();
  }

  public boolean isStandalone() {
    return muleContext.getConfiguration().isStandalone();
  }

  public Map<String, Object> getRegistry() {
    return new RegistryWrapperMap(muleContext.getRegistry());
  }

  private static class RegistryWrapperMap extends AbstractMapContext<Object> {

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
