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
 * @deprecated Use {@link org.mule.runtime.module.artifact.activation.api.extension.ExtensionModelLoaderRepository} instead.
 */
@Deprecated
@FunctionalInterface
public interface ExtensionModelLoaderRepository
    extends org.mule.runtime.module.artifact.activation.api.extension.ExtensionModelLoaderRepository {

  /**
   * Retrieves the {@link ExtensionModelLoader} for the given {@link LoaderDescriber}.
   * 
   * @param loaderDescriber {@link LoaderDescriber} describes the loader needed.
   * @return {@link ExtensionModelLoader} for the given {@link LoaderDescriber} or {@link Optional#empty()}.
   */
  Optional<ExtensionModelLoader> getExtensionModelLoader(LoaderDescriber loaderDescriber);

}
