/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.boot;

import static java.lang.Thread.currentThread;

import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelLoaderProvider;

import java.util.stream.Stream;

public final class ExtensionLoaderUtils {

  public static Stream<ExtensionModelLoader> lookupExtensionModelLoaders() {
    return lookupExtensionModelLoaders(currentThread().getContextClassLoader());
  }

  public static Stream<ExtensionModelLoader> lookupExtensionModelLoaders(ClassLoader classLoader) {
    return new SpiServiceRegistry().lookupProviders(ExtensionModelLoaderProvider.class, classLoader)
        .stream()
        .flatMap(p -> p.getExtensionModelLoaders().stream());
  }

  public static ExtensionModelLoader getLoaderById(String id) {
    return getLoaderById(currentThread().getContextClassLoader(), id);
  }

  public static ExtensionModelLoader getLoaderById(ClassLoader classLoader, String id) {
    return lookupExtensionModelLoaders(classLoader)
        .filter(extensionModelLoader -> extensionModelLoader.getId().equals(id))
        .findAny()
        .orElseThrow(() -> new RuntimeException("No loader found for id:{" + id + "}"));
  }

  private ExtensionLoaderUtils() {}
}
