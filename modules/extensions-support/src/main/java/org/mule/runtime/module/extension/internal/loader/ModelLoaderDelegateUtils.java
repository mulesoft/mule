/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader;

import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.SourceModelParser;

public final class ModelLoaderDelegateUtils {

  public ModelLoaderDelegateUtils() {}

  public static boolean requiresConfig(SourceModelParser parser) {
    return parser.hasConfig() || parser.isConnected();
  }

  public static boolean requiresConfig(OperationModelParser parser) {
    return parser.hasConfig() || parser.isConnected() || parser.isAutoPaging();
  }
}
