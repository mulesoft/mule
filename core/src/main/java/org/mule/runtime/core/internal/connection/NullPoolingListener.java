/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
