/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static org.apache.commons.io.FileUtils.toFile;

import org.mule.runtime.config.api.dsl.model.ResourceProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

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
    URL resourceUrl = classLoader.getResource(uri);
    if (resourceUrl == null) {
      return null;
    }

    File resourceFile = toFile(resourceUrl);
    if (!resourceFile.exists()) {
      return null;
    }

    try {
      return new FileInputStream(resourceFile);
    } catch (FileNotFoundException e) {
      return null;
    }
  }
}
