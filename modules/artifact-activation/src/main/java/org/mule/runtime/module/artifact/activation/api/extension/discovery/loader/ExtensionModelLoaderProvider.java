/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.extension.discovery.loader;

import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

import java.util.Set;

/**
 * Contract intended for the discovery of the available {@link ExtensionModelLoader}.
 * <p>
 * This contract was originally design with SPI discovery in mind, but could also be used in different contexts.
 *
 * @since 4.5.0
 */
public interface ExtensionModelLoaderProvider {

  /**
   * @return a {@link Set} of available {@link ExtensionModelLoader} instances.
   */
  Set<ExtensionModelLoader> getExtensionModelLoaders();
}
