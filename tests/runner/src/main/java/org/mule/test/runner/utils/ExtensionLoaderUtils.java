/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.utils;

import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

import java.util.Collection;

public class ExtensionLoaderUtils {

  public static ExtensionModelLoader getLoaderById(String id) {
    final SpiServiceRegistry spiServiceRegistry = new SpiServiceRegistry();
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    final Collection<ExtensionModelLoader> extensionModelLoaders =
        spiServiceRegistry.lookupProviders(ExtensionModelLoader.class, classLoader);
    return extensionModelLoaders.stream()
        .filter(extensionModelLoader -> extensionModelLoader.getId().equals(id))
        .findAny()
        .orElseThrow(() -> new RuntimeException("No loader found for id:{" + id + "}"));
  }

}
