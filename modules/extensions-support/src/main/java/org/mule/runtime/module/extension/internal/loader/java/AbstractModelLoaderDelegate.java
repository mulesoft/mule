/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;

import java.util.Optional;

/**
 * Base class for sub delegates of {@link DefaultJavaModelLoaderDelegate}
 *
 * @since 4.0
 */
abstract class AbstractModelLoaderDelegate {

  protected final DefaultJavaModelLoaderDelegate loader;

  AbstractModelLoaderDelegate(DefaultJavaModelLoaderDelegate loader) {
    this.loader = loader;
  }

  protected ClassLoader getExtensionClassLoader() {
    return getExtensionType()
        .map(Class::getClassLoader)
        .orElseGet(ExtensionModel.class::getClassLoader);
  }

  protected Optional<Class<?>> getExtensionType() {
    return Optional.ofNullable(loader.getExtensionType());
  }

  protected ExtensionElement getExtensionElement() {
    return loader.getExtensionElement();
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
}
