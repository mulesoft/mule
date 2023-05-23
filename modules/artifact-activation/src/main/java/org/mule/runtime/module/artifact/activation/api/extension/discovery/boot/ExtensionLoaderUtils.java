/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.extension.discovery.boot;

import static org.mule.runtime.api.util.MuleSystemProperties.PARALLEL_EXTENSION_MODEL_LOADING_PROPERTY;
import static org.mule.runtime.extension.internal.spi.ExtensionsApiSpiUtils.loadExtensionModelLoaderProviders;

import static java.lang.Boolean.getBoolean;
import static java.lang.Thread.currentThread;

import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

import java.util.NoSuchElementException;
import java.util.Optional;
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
    return loadExtensionModelLoaderProviders(classLoader)
        .flatMap(p -> p.getExtensionModelLoaders().stream());
  }

  /**
   * Finds an {@link ExtensionModelLoader} with a matching {@code id} using the current context {@code classLoader}
   *
   * @param id the wanted loader's id
   * @return the found {@link ExtensionModelLoader}
   * @throws NoSuchElementException if no matching loader is found
   * @since 4.5.0
   */
  public static ExtensionModelLoader getLoaderById(String id) {
    return getLoaderById(currentThread().getContextClassLoader(), id);
  }

  /**
   * Finds an {@link ExtensionModelLoader} with a matching {@code id} in the context of the given {@code classLoader}
   *
   * @param classLoader the classloader owning the search space
   * @param id          the wanted loader's id
   * @return the found {@link ExtensionModelLoader}
   * @throws NoSuchElementException if no matching loader is found
   * @since 4.5.0
   */
  public static ExtensionModelLoader getLoaderById(ClassLoader classLoader, String id) {
    return getOptionalLoaderById(classLoader, id)
        .orElseThrow(() -> new NoSuchElementException("No loader found for id:{" + id + "}"));
  }

  /**
   * Looks for an {@link ExtensionModelLoader} with a matching {@code id} in the context of the given {@code classLoader}
   *
   * @param classLoader the classloader owning the search space
   * @param id          the wanted loader's id
   * @return An optional {@link ExtensionModelLoader}
   * @since 4.5.0
   */
  public static Optional<ExtensionModelLoader> getOptionalLoaderById(ClassLoader classLoader, String id) {
    return lookupExtensionModelLoaders(classLoader)
        .filter(extensionModelLoader -> extensionModelLoader.getId().equals(id))
        .findFirst();
  }

  public static boolean isParallelExtensionModelLoadingEnabled() {
    return getBoolean(PARALLEL_EXTENSION_MODEL_LOADING_PROPERTY);
  }

  private ExtensionLoaderUtils() {}
}
