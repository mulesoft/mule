/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.delegate;

import org.mule.runtime.extension.api.loader.delegate.ModelLoaderDelegate;
import org.mule.runtime.extension.api.loader.delegate.ModelLoaderDelegateFactory;

/**
 * Default factory, available through SPI.
 *
 * @since 4.10.0
 */
public class DefaultExtensionModelLoaderDelegateFactory implements ModelLoaderDelegateFactory {

  @Override
  public ModelLoaderDelegate getLoader(String version, String loaderId) {
    return new DefaultExtensionModelLoaderDelegate(version);
  }
}
