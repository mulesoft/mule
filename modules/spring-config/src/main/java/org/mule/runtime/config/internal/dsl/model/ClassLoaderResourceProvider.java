/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static org.mule.runtime.core.api.util.ClassUtils.getResourceOrFail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.core.api.exception.ResourceNotFoundException;

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
    try {
      URL resource = getResourceOrFail(uri, classLoader);
      InputStream resourceAsStream;
      try {
        resourceAsStream = resource.openStream();
        if (resourceAsStream == null) {
          InputStream file = openAsAbsolutePath(uri);
          if (file != null) {
            return file;
          }
        }
        return resourceAsStream;
      } catch (IOException e) {
        throw new MuleRuntimeException(e);
      }
    } catch (ResourceNotFoundException e) {
      InputStream inputStream = openAsAbsolutePath(uri);
      if (inputStream == null) {
        throw e;
      }
      return inputStream;
    }
  }

  public InputStream openAsAbsolutePath(String uri) {
    File file = new File(uri);
    if (file.exists()) {
      try {
        return new FileInputStream(file);
      } catch (FileNotFoundException e) {
        throw new MuleRuntimeException(e);
      }
    }
    return null;
  }
}
