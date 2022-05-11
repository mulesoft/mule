/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

public class ConnectionProperties {

  private String connectionDescription;
  private ConnectionType connectionType;

  public ConnectionProperties() {}

  public ConnectionProperties(String connectionDescription, ConnectionType connectionType) {
    this.connectionDescription = connectionDescription;
    this.connectionType = connectionType;
  }

  public String getConnectionDescription() {
    return connectionDescription;
  }

  public ConnectionType getConnectionType() {
    return connectionType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ConnectionProperties that = (ConnectionProperties) o;

    if (connectionDescription != null ? !connectionDescription.equals(that.connectionDescription)
        : that.connectionDescription != null)
      return false;
    return connectionType == that.connectionType;
  }

  @Override
  public int hashCode() {
    int result = connectionDescription != null ? connectionDescription.hashCode() : 0;
    result = 31 * result + (connectionType != null ? connectionType.hashCode() : 0);
    return result;
  }
}
