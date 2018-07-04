/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static org.mule.runtime.core.api.util.ClassUtils.getResourceOrFail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;

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
