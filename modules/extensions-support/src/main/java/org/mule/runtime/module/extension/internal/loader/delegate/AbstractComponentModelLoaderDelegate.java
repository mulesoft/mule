/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.delegate;

/**
 * Base class for sub delegates of {@link DefaultExtensionModelLoaderDelegate}
 *
 * @since 4.0
 */
abstract class AbstractComponentModelLoaderDelegate {

  protected final DefaultExtensionModelLoaderDelegate loader;

  AbstractComponentModelLoaderDelegate(DefaultExtensionModelLoaderDelegate loader) {
    this.loader = loader;
  }

  SourceModelLoaderDelegate getSourceModelLoaderDelegate() {
    return loader.getSourceModelLoaderDelegate();
  }

  ConnectionProviderModelLoaderDelegate getConnectionProviderModelLoaderDelegate() {
    return loader.getConnectionProviderModelLoaderDelegate();
  }

  OperationModelLoaderDelegate getOperationLoaderDelegate() {
    return loader.getOperationLoaderDelegate();
  }

  FunctionModelLoaderDelegate getFunctionModelLoaderDelegate() {
    return loader.getFunctionModelLoaderDelegate();
  }

  StereotypeModelLoaderDelegate getStereotypeModelLoaderDelegate() {
    return loader.getStereotypeModelLoaderDelegate();
  }
}
