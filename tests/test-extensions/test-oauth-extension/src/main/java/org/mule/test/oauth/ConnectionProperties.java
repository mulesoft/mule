/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.test.values.extension.MyPojo;

import java.time.ZonedDateTime;

import jakarta.inject.Inject;

public class ConnectionProperties {

  @Inject
  private final ExtensionManager extensionManager = null;

  private String connectionDescription;
  private ConnectionType connectionType;
  private Literal<String> connectionPropertyGrade;
  private ZonedDateTime connectionTime;
  private MyPojo importedPojo;

  public ConnectionProperties() {}

  public ConnectionProperties(String connectionDescription, ConnectionType connectionType,
                              Literal<String> connectionPropertyGrade, ZonedDateTime connectionTime, MyPojo importedPojo) {
    this.connectionDescription = connectionDescription;
    this.connectionType = connectionType;
    this.connectionPropertyGrade = connectionPropertyGrade;
    this.connectionTime = connectionTime;
    this.importedPojo = importedPojo;
  }

  public Literal<String> getConnectionPropertyGrade() {
    return connectionPropertyGrade;
  }

  public String getConnectionDescription() {
    return connectionDescription;
  }

  public void setConnectionDescription(String connectionDescription) {
    this.connectionDescription = connectionDescription;
  }

  public ConnectionType getConnectionType() {
    return connectionType;
  }

  public void setConnectionType(ConnectionType connectionType) {
    this.connectionType = connectionType;
  }

  public ExtensionManager getExtensionManager() {
    return extensionManager;
  }

  public ZonedDateTime getConnectionTime() {
    return connectionTime;
  }

  public MyPojo getImportedPojo() {
    return importedPojo;
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
    if (connectionType != that.connectionType)
      return false;
    if (connectionPropertyGrade != null && that.connectionPropertyGrade != null) {
      if (!connectionPropertyGrade.getLiteralValue().equals(that.connectionPropertyGrade.getLiteralValue()))
        return false;
    } else {
      if (connectionPropertyGrade != that.connectionPropertyGrade) {
        return false;
      }
    }
    if (importedPojo != null ? !importedPojo.equals(that.importedPojo)
        : that.importedPojo != null)
      return false;
    return connectionTime != null ? connectionTime.equals(that.connectionTime) : that.connectionTime == null;
  }

  @Override
  public int hashCode() {
    int result = connectionDescription != null ? connectionDescription.hashCode() : 0;
    result = 31 * result + (connectionType != null ? connectionType.hashCode() : 0);
    result =
        31 * result + (connectionPropertyGrade != null ? connectionPropertyGrade.getLiteralValue().orElse("").hashCode() : 0);
    result = 31 * result + (connectionTime != null ? connectionTime.hashCode() : 0);
    return result;
  }
}
