/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader;

import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;

/**
 * Contract for classes that creates an {@link ExtensionDeclarer} from a {@link ExtensionLoadingContext}.
 *
 * @since 4.0
 */
public interface ModelLoaderDelegate {

  /**
   * Creates and populates an {@link ExtensionDeclarer} from a {@link ExtensionLoadingContext}.
   *
   * @param context an {@link ExtensionLoadingContext} instance.
   * @return a built {@link ExtensionDeclarer}.
   */
  ExtensionDeclarer declare(ExtensionLoadingContext context);
}
