/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import org.mule.metadata.api.ClassTypeLoader;

/**
 * Base class for sub delegates of {@link JavaModelLoaderDelegate}
 *
 * @since 4.0
 */
abstract class AbstractModelLoaderDelegate {

  protected final JavaModelLoaderDelegate loader;

  AbstractModelLoaderDelegate(JavaModelLoaderDelegate loader) {
    this.loader = loader;
  }

  protected Class<?> getExtensionType() {
    return loader.getExtensionType();
  }

  ConfigModelLoaderDelegate getConfigLoaderDelegate() {
    return loader.getConfigLoaderDelegate();
  }

  OperationModelLoaderDelegate getOperationModelLoaderDelegate() {
    return loader.getOperationLoaderDelegate();
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

  ClassTypeLoader getTypeLoader() {
    return loader.getTypeLoader();
  }
}
