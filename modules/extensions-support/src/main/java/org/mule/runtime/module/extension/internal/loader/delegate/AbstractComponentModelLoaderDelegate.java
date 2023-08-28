/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
