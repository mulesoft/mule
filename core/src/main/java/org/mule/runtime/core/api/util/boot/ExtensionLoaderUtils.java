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

import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * Utilities for loading Extensions
 *
 * @since 4.5.0
 */
public final class ExtensionLoaderUtils {

  /**
   * @return a {@link Stream} with all the {@link ExtensionModelLoader} available on the current context {@link ClassLoader}
   */
  public static Stream<ExtensionModelLoader> lookupExtensionModelLoaders() {
    return lookupExtensionModelLoaders(currentThread().getContextClassLoader());
  }

  /**
   * @param classLoader the classloader owning the search space
   * @return a {@link Stream} with all the {@link ExtensionModelLoader} available to the given {@code classloader}
   */
  public static Stream<ExtensionModelLoader> lookupExtensionModelLoaders(ClassLoader classLoader) {
    return new SpiServiceRegistry().lookupProviders(ExtensionModelLoaderProvider.class, classLoader)
        .stream()
        .flatMap(p -> p.getExtensionModelLoaders().stream());
  }

  /**
   * Finds an {@link ExtensionModelLoader} with a matching {@code id} using the current context {@link ClassLoader}
   *
   * @param id the wanted loader's id
   * @return the found {@link ExtensionModelLoader}
   * @throws NoSuchElementException if no matching loader is found
   */
  public static ExtensionModelLoader getLoaderById(String id) {
    return getLoaderById(currentThread().getContextClassLoader(), id);
  }

  /**
   * Finds an {@link ExtensionModelLoader} with a matching {@code id} in the context of the given {@link ClassLoader}
   *
   * @param classLoader the classloader owning the search space
   * @param id          the wanted loader's id
   * @return the found {@link ExtensionModelLoader}
   * @throws NoSuchElementException if no matching loader is found
   */
  public static ExtensionModelLoader getLoaderById(ClassLoader classLoader, String id) {
    return lookupExtensionModelLoaders(classLoader)
        .filter(extensionModelLoader -> extensionModelLoader.getId().equals(id))
        .findAny()
        .orElseThrow(() -> new NoSuchElementException("No loader found for id:{" + id + "}"));
  }

  private ExtensionLoaderUtils() {}
}
