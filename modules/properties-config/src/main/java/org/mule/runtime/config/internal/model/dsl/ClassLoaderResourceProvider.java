/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.model.dsl;

import static org.mule.runtime.core.api.util.ClassUtils.getResourceOrFail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.properties.api.ResourceProvider;

/**
 * Resource provider implementation that delegates to an actual class loader.
 */
public class ClassLoaderResourceProvider implements ResourceProvider {

  private ClassLoader classLoader;

  public ClassLoaderResourceProvider(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public InputStream getResourceAsStream(String uri) {
    URL resource = getResourceOrFail(uri, classLoader, true);
    try {
      return resource.openStream();
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

}
