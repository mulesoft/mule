/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.app.internal.loader;

import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

public class AppExtensionModelLoader extends ExtensionModelLoader {

  public static final String APP_EXTENSION_LOADER_ID = "app";

  @Override
  public String getId() {
    return APP_EXTENSION_LOADER_ID;
  }

  @Override
  protected void declareExtension(ExtensionLoadingContext context) {

  }
}
