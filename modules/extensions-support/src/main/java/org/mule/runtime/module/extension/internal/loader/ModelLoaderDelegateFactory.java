/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader;

import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.delegate.ModelLoaderDelegate;

/**
 * {@link ModelLoaderDelegate} factory
 *
 * @since 4.1
 */
@FunctionalInterface
public interface ModelLoaderDelegateFactory {

  /**
   * Returns a new {@link ModelLoaderDelegate} instance based on the given {@link ExtensionElement} and version.
   *
   * @param extensionElement representing the extension class
   * @param version          the extension's version
   * @return a new {@link ModelLoaderDelegate}
   */
  ModelLoaderDelegate getLoader(ExtensionElement extensionElement, String version);

}
