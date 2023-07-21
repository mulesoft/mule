/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.tcp;

/**
 * TCP client specific configuration.
 *
 * @since 4.0
 */
public interface TcpClientSocketProperties extends TcpSocketProperties {

  /**
   * @return a {@link TcpClientSocketPropertiesBuilder}.
   */
  static TcpClientSocketPropertiesBuilder builder() {
    return new TcpClientSocketPropertiesBuilder();
  }

  /**
   * Number of milliseconds to wait until an outbound connection to a remote server is successfully created. Defaults to 30
   * seconds.
   */
  Integer getConnectionTimeout();
}
