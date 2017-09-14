/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.connector;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

/**
 * Manages all the connections opened between the boundaries of an application.
 * <p>
 * It works under the model of one owner (from now on the 'config'), has a one-to-one relationship with a
 * {@link ConnectionProvider}. That relationship is declared through the {@link #bind(Object, ConnectionProvider)} method. That
 * relationship can be broken through the {@link #unbind(Object)} method.
 * <p>
 * Once the config-provider pair has been bound, connections can be obtained through the {@link #getConnection(Object)} method.
 * That method will yield a {@link ConnectionHandler} which will hide the details of how the connection is actually being managed
 * while also avoiding the need to pass around config, provider or even this manager.
 * <p>
 * All implementations are required to be thread-safe
 *
 * @since 4.0
 */
public interface ConnectionManager {

  /**
   * Binds the given {@code config} and {@code connectionProvider} so that the latter is used each time that the {@code config} is
   * supplied as an argument of the {@link #getConnection(Object)} method.
   * <p>
   * If a binding already exists for the {@code config} then this one replaces the previous one. All connections produced by the
   * previous binding are closed.
   *
   * @param config             the config that acts as the binding key
   * @param connectionProvider the {@link ConnectionProvider} that produces the connections
   * @param <C>       the generic type of the connections to be produced
   */
  <C> void bind(Object config, ConnectionProvider<C> connectionProvider);

  /**
   * @param config the config that acts as the binding key
   * @return whether the {@code config} is currently bound to a {@link ConnectionProvider}
   */
  boolean hasBinding(Object config);

  /**
   * Breaks the binding that was previously produced by invoking {@link #bind(Object, ConnectionProvider)} with the given
   * {@code config} on {@code this} instance.
   * <p>
   * All connections produced as a result of the broken binding are closed once they're released. Once all connections created
   * with the previous {@link ConnectionProvider} are closed, the stop and dispose lifecycle phases are applied over the provider
   * if necessary.
   * <p>
   * If no such binding exists no action is taken.
   *
   * @param config a config for which a binding is presumed to exists
   */
  void unbind(Object config);

  /**
   * Obtains a {@link ConnectionHandler} for a {@code config} which was previously bound through the
   * {@link #bind(Object, ConnectionProvider)} method.
   * <p>
   * This method does not guarantee if the returned connections are always different, reused, pooled, etc. That depends on the
   * implementation of this interface and the implementation of the bounded {@link ConnectionProvider}.
   * <p>
   * Once the requester has finished using the obtained connection, it is <b>MANDATORY</b> for it to invoke the
   * {@link ConnectionHandler#release()} method on it.
   *
   * @param config a config for which a binding has been established through {@link #bind(Object, ConnectionProvider)}
   * @param <C>    the generic type of the returned connection
   * @return a {@link ConnectionHandler} wrapping the produced connection
   * @throws ConnectionException if the conection could not be established or if no such binding exists for the {@code config}
   */
  <C> ConnectionHandler<C> getConnection(Object config) throws ConnectionException;

  /**
   * Tests connectivity of the given {@code connectionProvider}.
   * <p>
   * The {@code connectionProvider} is expected to be fully initialised and functional. However,
   * it is not required for it to have been registered through the {@link #bind(Object, ConnectionProvider)}
   * method.
   *
   * @param connectionProvider a {@link ConnectionProvider}
   * @param <C>                the generic type of the connections produced by the {@code connectionProvider}
   * @return a {@link  ConnectionValidationResult}
   */
  <C> ConnectionValidationResult testConnectivity(ConnectionProvider<C> connectionProvider);

  /**
   * Validates the given {@code connection}
   * <p>
   * The {@code connection} has to have been obtained through the given {@code connectionHandler}, and the handler
   * has to have been obtained through {@code this} instance
   *
   * @param connection the connection to validate
   *                   @param connectionHandler the connection's handler
   * @param <C>                the generic type of the validated connection
   * @return a {@link  ConnectionValidationResult}
   * @throws IllegalArgumentException if any of the preconditions are not met
   */
  <C> ConnectionValidationResult testConnectivity(C connection, ConnectionHandler<C> connectionHandler);

  /**
   * Tests connectivity for the given {@code configurationInstance}.
   * <p>
   * The {@code connectionProvider} is expected to be fully initialised and functional. It is not
   * required for the config wrapped by the {@code configurationInstance} to have been registered
   * through the {@link #bind(Object, ConnectionProvider)} method. However, if it has been, then
   * the test will be performed using the resources allocated by such registration.
   *
   * @param configurationInstance a {@link ConfigurationInstance}
   * @return a {@link ConnectionValidationResult}
   * @throws IllegalArgumentException if the {@code configurationInstance} doesn't have an associated {@link ConnectionProvider}
   */
  ConnectionValidationResult testConnectivity(ConfigurationInstance configurationInstance) throws IllegalArgumentException;
}
