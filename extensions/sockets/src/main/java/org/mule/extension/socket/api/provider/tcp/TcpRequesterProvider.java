/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.provider.tcp;

import static org.mule.extension.socket.api.SocketsExtension.TLS;
import static org.mule.extension.socket.api.SocketsExtension.TLS_CONFIGURATION;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.CONNECTION;
import org.mule.extension.socket.api.ConnectionSettings;
import org.mule.extension.socket.api.SocketOperations;
import org.mule.extension.socket.api.connection.tcp.TcpRequesterConnection;
import org.mule.extension.socket.api.connection.tcp.protocol.SafeProtocol;
import org.mule.extension.socket.api.socket.factory.SimpleSocketFactory;
import org.mule.extension.socket.api.socket.factory.SslSocketFactory;
import org.mule.extension.socket.api.socket.factory.TcpSocketFactory;
import org.mule.extension.socket.api.socket.tcp.TcpClientSocketProperties;
import org.mule.extension.socket.api.socket.tcp.TcpProtocol;
import org.mule.extension.socket.internal.SocketUtils;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.net.Socket;

import javax.net.ssl.SSLSocket;

/**
 * A {@link ConnectionProvider} which provides instances of {@link TcpRequesterConnection} to be used by the
 * {@link SocketOperations}
 *
 * @since 4.0
 */
@Alias("tcp-requester")
public final class TcpRequesterProvider implements PoolingConnectionProvider<TcpRequesterConnection>, Initialisable {

  /**
   * Its presence will imply the use of {@link SSLSocket} instead of plain TCP {@link Socket} for establishing a connection over
   * SSL.
   */
  @Parameter
  @Optional
  @Placement(group = TLS_CONFIGURATION, tab = TLS)
  @DisplayName(TLS_CONFIGURATION)
  private TlsContextFactory tlsContext;

  /**
   * This configuration parameter refers to the address where the {@link Socket} should connect to.
   */
  @ParameterGroup
  private ConnectionSettings connectionSettings;

  /**
   * {@link Socket} configuration properties
   */
  @ParameterGroup
  private TcpClientSocketProperties tcpClientSocketProperties;

  /**
   * This configuration parameter refers to the address where the {@link Socket} should bind to.
   */
  @Parameter
  @Optional
  @Placement(group = "Local Address Settings")
  ConnectionSettings localAddressSettings = new ConnectionSettings();


  /**
   * {@link TcpProtocol} that knows how the data is going to be read and written. If not specified, the {@link SafeProtocol} will
   * be used.
   */
  @Parameter
  @Optional
  @Summary("TCP Protocol to use when doing requests")
  @Placement(group = CONNECTION, order = 3)
  private TcpProtocol protocol = new SafeProtocol();

  /**
   * {@inheritDoc}
   */
  @Override
  public TcpRequesterConnection connect() throws ConnectionException {

    SimpleSocketFactory simpleSocketFactory = null;

    try {
      simpleSocketFactory = tlsContext != null ? new SslSocketFactory(tlsContext) : new TcpSocketFactory();
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }

    TcpRequesterConnection connection = new TcpRequesterConnection(connectionSettings, localAddressSettings, protocol,
                                                                   tcpClientSocketProperties, simpleSocketFactory);
    connection.connect();
    return connection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disconnect(TcpRequesterConnection connection) {
    connection.disconnect();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult validate(TcpRequesterConnection connection) {
    return SocketUtils.validate(connection);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(tlsContext);
  }
}
