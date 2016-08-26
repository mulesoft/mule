/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.config;

import org.mule.extension.socket.api.provider.tcp.TcpListenerProvider;
import org.mule.extension.socket.api.provider.udp.UdpListenerProvider;
import org.mule.extension.socket.api.source.SocketListener;
import org.mule.extension.socket.api.worker.SocketWorker;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connector.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

/**
 * Implementation of {@link AbstractSocketConfig} for listener sockets
 *
 * @since 4.0
 */
@Configuration(name = "listener-config")
@ConnectionProviders({TcpListenerProvider.class, UdpListenerProvider.class})
@Sources({SocketListener.class})
public class ListenerConfig extends AbstractSocketConfig {

  /**
   * Used by the {@link SocketListener} for the creation and execution of the {@link SocketWorker}
   */
  @Parameter
  @Optional
  @Summary("Defines a threading profile configuration")
  private ThreadingProfile threadingProfile;

  public ThreadingProfile getThreadingProfile() {
    return threadingProfile;
  }
}
