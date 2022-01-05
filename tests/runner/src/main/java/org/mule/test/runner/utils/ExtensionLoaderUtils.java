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
import org.mule.runtime.extension.api.loader.ExtensionModelLoaderProvider;

public class ExtensionLoaderUtils {

  public static ExtensionModelLoader getLoaderById(String id) {
    return new SpiServiceRegistry().lookupProviders(ExtensionModelLoaderProvider.class, currentThread().getContextClassLoader())
        .stream()
        .flatMap(p -> p.getExtensionModelLoaders().stream())
        .filter(extensionModelLoader -> extensionModelLoader.getId().equals(id))
        .findAny()
        .orElseThrow(() -> new RuntimeException("No loader found for id:{" + id + "}"));
  }
}
