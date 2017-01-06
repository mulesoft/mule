/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp;

import org.mule.compatibility.transport.tcp.i18n.TcpMessages;
import org.mule.runtime.core.util.NetworkUtils;
import org.mule.runtime.core.util.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpServerSocketFactory implements SimpleServerSocketFactory {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public ServerSocket createServerSocket(URI uri, int backlog, Boolean reuse) throws IOException {
    String host = StringUtils.defaultIfEmpty(uri.getHost(), "localhost");
    InetAddress inetAddress = InetAddress.getByName(host);

    if ((inetAddress.equals(NetworkUtils.getLocalHost()) || host.trim().equals("localhost"))
        && TcpPropertyHelper.isBindingLocalhostToAllLocalInterfaces()) {
      logger.warn(TcpMessages.localhostBoundToAllLocalInterfaces().toString());
      return createServerSocket(uri.getPort(), backlog, reuse);
    } else {
      return createServerSocket(inetAddress, uri.getPort(), backlog, reuse);
    }
  }

  @Override
  public ServerSocket createServerSocket(InetAddress address, int port, int backlog, Boolean reuse) throws IOException {
    return configure(new ServerSocket(), reuse, new InetSocketAddress(address, port), backlog);
  }

  @Override
  public ServerSocket createServerSocket(int port, int backlog, Boolean reuse) throws IOException {
    return configure(new ServerSocket(), reuse, new InetSocketAddress(port), backlog);
  }

  protected ServerSocket configure(ServerSocket socket, Boolean reuse, InetSocketAddress address, int backlog)
      throws IOException {
    if (null != reuse && reuse.booleanValue() != socket.getReuseAddress()) {
      socket.setReuseAddress(reuse.booleanValue());
    }
    // bind *after* setting so_reuseaddress
    socket.bind(address, backlog);
    return socket;
  }

}
