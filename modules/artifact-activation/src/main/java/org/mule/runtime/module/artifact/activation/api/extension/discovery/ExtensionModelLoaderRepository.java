/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.extension.discovery;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.artifact.activation.internal.extension.discovery.DefaultExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.api.plugin.LoaderDescriber;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Provides access to the {@link ExtensionModelLoader} available in the container.
 *
 * @since 4.0, moved to api in 4.5
 */
@NoImplement
public interface ExtensionModelLoaderRepository {

  /**
   * @return a repository that manages the lifecycle of the {@link ExtensionModelLoader} available in the
   *         {@link ExtensionModelLoaderRepository}.
   * @since 4.5
   */
  static ExtensionModelLoaderRepository getExtensionModelLoaderManager() {
    return new DefaultExtensionModelLoaderRepository();
  }

  /**
   * @return a repository that manages the lifecycle of the {@link ExtensionModelLoader} available in the
   *         {@link ExtensionModelLoaderRepository}.
   * @since 4.5
   */
  static ExtensionModelLoaderRepository getExtensionModelLoaderManager(Supplier<Collection<ExtensionModelLoader>> extModelLoadersLookup) {
    DefaultExtensionModelLoaderRepository muleExtensionModelLoaderManager = new DefaultExtensionModelLoaderRepository();
    muleExtensionModelLoaderManager.setExtensionModelLoadersLookup(extModelLoadersLookup);
    return muleExtensionModelLoaderManager;
  }

  /**
   * Retrieves the {@link ExtensionModelLoader} for the given {@link LoaderDescriber}.
   *
   * @param loaderDescriber {@link LoaderDescriber} describes the loader needed.
   * @return {@link ExtensionModelLoader} for the given {@link LoaderDescriber} or {@link Optional#empty()}.
   */
  Optional<ExtensionModelLoader> getExtensionModelLoader(LoaderDescriber loaderDescriber);

}
