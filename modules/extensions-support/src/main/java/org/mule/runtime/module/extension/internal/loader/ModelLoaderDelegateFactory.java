/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
@Deprecated
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
