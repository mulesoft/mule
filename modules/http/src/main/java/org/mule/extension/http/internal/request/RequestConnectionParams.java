/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.extension.socket.api.socket.tcp.TcpClientSocketProperties;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.service.http.api.HttpConstants;

/**
 * Groups parameters related to a requester connection
 *
 * @since 4.0
 */
public final class RequestConnectionParams {

  /**
   * Protocol to use for communication. Valid values are HTTP and HTTPS. Default value is HTTP. When using HTTPS the HTTP
   * communication is going to be secured using TLS / SSL. If HTTPS was configured as protocol then the user can customize the
   * tls/ssl configuration by defining the tls:context child element of this listener-config. If not tls:context is defined then
   * the default JVM certificates are going to be used to establish communication.
   */
  @Parameter
  @Optional(defaultValue = "HTTP")
  @Expression(NOT_SUPPORTED)
  @Summary("Protocol to use for communication. Valid values are HTTP and HTTPS")
  @Placement(order = 1)
  private HttpConstants.Protocols protocol;

  /**
   * Host where the requests will be sent.
   */
  @Parameter
  @Optional
  @Example("www.somehost.com")
  @Placement(order = 2)
  private String host;

  /**
   * Port where the requests will be sent. If the protocol attribute is HTTP (default) then the default value is 80, if the
   * protocol attribute is HTTPS then the default value is 443.
   */
  @Parameter
  @Optional
  @Placement(order = 3)
  private Integer port;

  /**
   * If false, each connection will be closed after the first request is completed.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED_TAB, order = 1)
  private Boolean usePersistentConnections;

  /**
   * The maximum number of outbound connections that will be kept open at the same time. By default the number of connections is
   * unlimited.
   */
  @Parameter
  @Optional(defaultValue = "-1")
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED_TAB, order = 2)
  private Integer maxConnections;

  /**
   * The number of milliseconds that a connection can remain idle before it is closed. The value of this attribute is only used
   * when persistent connections are enabled.
   */
  @Parameter
  @Optional(defaultValue = "30000")
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED_TAB, order = 3)
  private Integer connectionIdleTimeout;

  @Parameter
  @Optional
  @NullSafe
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED_TAB, order = 4)
  private TcpClientSocketProperties clientSocketProperties;

  public HttpConstants.Protocols getProtocol() {
    return protocol;
  }

  public String getHost() {
    return host;
  }

  public Integer getPort() {
    return port;
  }

  public Boolean getUsePersistentConnections() {
    return usePersistentConnections;
  }

  public Integer getMaxConnections() {
    return maxConnections;
  }

  public Integer getConnectionIdleTimeout() {
    return connectionIdleTimeout;
  }

  public TcpClientSocketProperties getClientSocketProperties() {
    return clientSocketProperties;
  }

  public void setProtocol(HttpConstants.Protocols protocol) {
    this.protocol = protocol;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public void setUsePersistentConnections(Boolean usePersistentConnections) {
    this.usePersistentConnections = usePersistentConnections;
  }

  public void setMaxConnections(Integer maxConnections) {
    this.maxConnections = maxConnections;
  }

  public void setConnectionIdleTimeout(Integer connectionIdleTimeout) {
    this.connectionIdleTimeout = connectionIdleTimeout;
  }

  public void setClientSocketProperties(TcpClientSocketProperties clientSocketProperties) {
    this.clientSocketProperties = clientSocketProperties;
  }
}
