/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader;

import org.mule.runtime.module.extension.internal.loader.delegate.ModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.SourceModelParser;

/**
 * Utility methods for {@link ModelLoaderDelegate} implementations
 *
 * @since 4.5.0
 */
public final class ModelLoaderDelegateUtils {

  public ModelLoaderDelegateUtils() {}

  /**
   * @param parser a {@link SourceModelParser}
   * @return whether the given {@code parser} represents a source which requires a config to function
   */
  public static boolean requiresConfig(SourceModelParser parser) {
    return parser.hasConfig() || parser.isConnected();
  }

  /**
   * @param parser a {@link OperationModelParser}
   * @return whether the given {@code parser} represents an operation which requires a config to function
   */
  public static boolean requiresConfig(OperationModelParser parser) {
    return parser.hasConfig() || parser.isConnected() || parser.isAutoPaging();
  }
}
