/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.connection.tcp;

import static java.lang.String.format;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.extension.socket.api.connection.AbstractSocketConnection;
import org.mule.extension.socket.api.ConnectionSettings;
import org.mule.extension.socket.api.exceptions.UnresolvableHostException;
import org.mule.extension.socket.api.socket.tcp.TcpProtocol;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.serialization.ObjectSerializer;

import java.net.InetSocketAddress;

import javax.inject.Inject;

/**
 * Contains behaviour and attributes proper of TCP connections
 *
 * @since 4.0
 */
abstract class AbstractTcpConnection extends AbstractSocketConnection implements Initialisable {

  @Inject
  private MuleContext muleContext;

  /**
   * {@link TcpProtocol}
   */
  protected final TcpProtocol protocol;

  public AbstractTcpConnection(ConnectionSettings connectionSettings, TcpProtocol protocol) {
    super(connectionSettings);
    this.protocol = protocol;
  }

  /**
   * Injection of the {@link ObjectSerializer} is needed for {@link TcpProtocol}
   */
  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(protocol, true, muleContext);
  }


  /**
   * Creates an {@link InetSocketAddress} with the host and port information.
   *
   * @param failOnUnresolvedHost whether a {@link UnresolvableHostException} should be thrown if the address couldn't be resolved.
   * @return an {@link InetSocketAddress} configured with the host and port received
   */
  protected InetSocketAddress getSocketAddress(ConnectionSettings connectionSettings, boolean failOnUnresolvedHost)
      throws ConnectionException {

    InetSocketAddress address = connectionSettings.getInetSocketAddress();
    if (address.isUnresolved() && failOnUnresolvedHost) {
      throw new ConnectionException(format("Host '%s' could not be resolved", connectionSettings.getHost()));
    }
    return address;
  }
}
