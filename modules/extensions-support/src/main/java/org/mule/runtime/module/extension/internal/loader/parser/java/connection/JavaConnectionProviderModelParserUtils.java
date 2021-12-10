/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.connection;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.module.extension.api.loader.java.type.ConnectionProviderElement;

public final class JavaConnectionProviderModelParserUtils {

  public static boolean isPoolingConnectionProvider(ConnectionProviderElement element) {
    return element.isAssignableTo(PoolingConnectionProvider.class)
        || element.isAssignableTo(org.mule.sdk.api.connectivity.PoolingConnectionProvider.class);
  }

  public static boolean isCachedConnectionProvider(ConnectionProviderElement element) {
    return element.isAssignableTo(CachedConnectionProvider.class)
        || element.isAssignableTo(org.mule.sdk.api.connectivity.CachedConnectionProvider.class);
  }

  private JavaConnectionProviderModelParserUtils() {}
}
