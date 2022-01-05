/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader;

import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

public class ExtensionModelLoaderDecorator extends ExtensionModelLoader {

  private final ExtensionModelLoader delegate;

  public ExtensionModelLoaderDecorator(ExtensionModelLoader delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getId() {
    return delegate.getId();
  }


  @Override
  protected void declareExtension(ExtensionLoadingContext context) {
    delegate.declareExtension(context);
  }
}
