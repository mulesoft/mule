/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.connection;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.module.extension.api.loader.java.type.ConnectionProviderElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.sdk.api.connectivity.ConnectionProvider;
import org.mule.sdk.api.connectivity.TransactionalConnection;

/**
 * Utilities for parsing Java defined {@link ConnectionProviderModel} instances
 *
 * @since 4.5.0
 */
public final class JavaConnectionProviderModelParserUtils {

  /**
   * @param element a {@link ConnectionProviderElement}
   * @return whether the {@code element} represents a pooling provider
   */
  public static boolean isPoolingConnectionProvider(ConnectionProviderElement element) {
    return element.isAssignableTo(PoolingConnectionProvider.class)
        || element.isAssignableTo(org.mule.sdk.api.connectivity.PoolingConnectionProvider.class);
  }

  /**
   * @param element a {@link ConnectionProviderElement}
   * @return whether the {@code element} represents a caching provider
   */
  public static boolean isCachedConnectionProvider(ConnectionProviderElement element) {
    return element.isAssignableTo(CachedConnectionProvider.class)
        || element.isAssignableTo(org.mule.sdk.api.connectivity.CachedConnectionProvider.class);
  }

  /**
   * @param element a {@link ConnectionProviderElement}
   * @return whether the {@code element} represents a provider defined through the sdk-api
   */
  public static boolean isDefinedThroughSdkApi(ConnectionProviderElement element) {
    return element.isAssignableTo(ConnectionProvider.class);
  }

  /**
   * @param connectionType the connection type
   * @return whether the given {@code connectionType} represents a transactional connection
   */
  public static boolean isTransactional(Type connectionType) {
    return connectionType.isAssignableTo(TransactionalConnection.class);
  }

  private JavaConnectionProviderModelParserUtils() {}
}
