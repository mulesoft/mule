/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.sdk.api.connectivity.XATransactionalConnection;
import org.mule.sdk.api.connectivity.XATransactionalConnectionProvider;

import javax.transaction.xa.XAResource;

import jakarta.transaction.TransactionManager;

/**
 * Allows for {@link XATransactionalConnection#getXAResource() XA resources} to be managed with a {@link TransactionManager}.
 *
 * @see XATransactionalConnectionProvider
 * 
 * @see XAResource
 * 
 * @since 4.10
 */
public interface XAConnectionManagementStrategyFactory {

  /**
   * Wraps the given {@code poolingStrategy} with XA capabilities, by registering the pooled connections into a
   * {@link TransactionManager}.
   * 
   * @param <C>                the actual type of the connection that supports XA transactions.
   * @param poolingStrategy    the pooling that manages the connections to be used by the {@link TransactionManager}.
   * @param connectionProvider the actual provider for connections to be managed by the {@link TransactionManager}.
   * @return a new strategy that handles XA transactions for the provided connection provider.
   */
  <C extends XATransactionalConnection> ConnectionManagementStrategy<C> manageForXa(ConnectionManagementStrategy<C> poolingStrategy,
                                                                                    ConnectionProvider<C> connectionProvider);

}
