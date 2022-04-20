/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.api.artifact.extension;

import org.mule.runtime.deployment.model.internal.artifact.extension.MuleExtensionModelLoaderManager;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.plugin.LoaderDescriber;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Provides access to the {@link ExtensionModelLoader} available in the container.
 *
 * @since 4.0, moved to api in 4.5
 * @deprecated Use {@link org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository}
 *             instead.
 */
@Deprecated
// TODO W-10928152: remove this interface when migrating to use the new extension model loading API.
@FunctionalInterface
public interface ExtensionModelLoaderRepository
    extends org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository {

  /**
   * @return a repository that manages the lifecycle of the {@link ExtensionModelLoader} available in the
   *         {@link ExtensionModelLoaderRepository}.
   * @since 4.5
   */
  public static ExtensionModelLoaderRepository getExtensionModelLoaderManager(ArtifactClassLoader containerClassLoader) {
    return new MuleExtensionModelLoaderManager(containerClassLoader);
  }

  /**
   * @return a repository that manages the lifecycle of the {@link ExtensionModelLoader} available in the
   *         {@link ExtensionModelLoaderRepository}.
   * @since 4.5
   */
  public static ExtensionModelLoaderRepository getExtensionModelLoaderManager(ArtifactClassLoader containerClassLoader,
                                                                              Supplier<Collection<ExtensionModelLoader>> extModelLoadersLookup) {
    MuleExtensionModelLoaderManager muleExtensionModelLoaderManager = new MuleExtensionModelLoaderManager(containerClassLoader);
    muleExtensionModelLoaderManager.setExtensionModelLoadersLookup(extModelLoadersLookup);
    return muleExtensionModelLoaderManager;
  }

  /**
   * Retrieves the {@link ExtensionModelLoader} for the given {@link LoaderDescriber}.
   * 
   * @param loaderDescriber {@link LoaderDescriber} describes the loader needed.
   * @return {@link ExtensionModelLoader} for the given {@link LoaderDescriber} or {@link Optional#empty()}.
   */
  @Override
  Optional<ExtensionModelLoader> getExtensionModelLoader(LoaderDescriber loaderDescriber);

}
