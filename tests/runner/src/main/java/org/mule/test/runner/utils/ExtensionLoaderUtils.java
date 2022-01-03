/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.utils;

import static java.lang.Thread.currentThread;

import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

import java.util.ArrayList;
import java.util.List;

public class ExtensionLoaderUtils {

  public static ExtensionModelLoader getLoaderById(String id) {
    final SpiServiceRegistry registry = new SpiServiceRegistry();
    final List<ExtensionModelLoader> extensionModelLoaders = new ArrayList<>(5);

    extensionModelLoaders
        .addAll(registry.lookupProviders(ExtensionModelLoader.class, ExtensionModelLoader.class.getClassLoader()));
    extensionModelLoaders.addAll(registry.lookupProviders(ExtensionModelLoader.class, currentThread().getContextClassLoader()));

    return extensionModelLoaders.stream()
        .filter(extensionModelLoader -> extensionModelLoader.getId().equals(id))
        .findAny()
        .orElseThrow(() -> new RuntimeException("No loader found for id:{" + id + "}"));
  }
}
