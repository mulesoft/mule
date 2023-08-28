/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.connection.PoolingListener;

/**
 * Implementation of the Null Object design pattern for the {@link PoolingListener} interface.
 *
 * @param <Connection> the generic type for the pooled connection
 * @since 4.0
 */
final class NullPoolingListener<Connection> implements PoolingListener<Connection> {

  /**
   * Does nothing
   */
  @Override
  public void onBorrow(Connection connection) {

  }

  /**
   * Does nothing
   */
  @Override
  public void onReturn(Connection connection) {

  }
}
