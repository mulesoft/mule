/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.parameter.Literal;

public class ConnectionProfile {

  @Parameter
  @Optional
  private ConnectionProperties profileConnectionProperties;

  @Parameter
  @Optional
  private Integer profileLevel;

  @Parameter
  @Optional
  private Literal<String> profileDescription;

  public ConnectionProfile() {}

  public ConnectionProfile(ConnectionProperties profileConnectionProperties, Integer profileLevel,
                           Literal<String> profileDescription) {
    this.profileConnectionProperties = profileConnectionProperties;
    this.profileLevel = profileLevel;
    this.profileDescription = profileDescription;
  }

  public ConnectionProperties getProfileConnectionProperties() {
    return profileConnectionProperties;
  }

  public Integer getProfileLevel() {
    return profileLevel;
  }

  public Literal<String> getProfileDescription() {
    return profileDescription;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ConnectionProfile that = (ConnectionProfile) o;

    if (profileConnectionProperties != null ? !profileConnectionProperties.equals(that.profileConnectionProperties)
        : that.profileConnectionProperties != null)
      return false;
    if (profileLevel != null ? !profileLevel.equals(that.profileLevel) : that.profileLevel != null)
      return false;
    return profileDescription != null ? profileDescription.equals(that.profileDescription) : that.profileDescription == null;
  }

  @Override
  public int hashCode() {
    int result = profileConnectionProperties != null ? profileConnectionProperties.hashCode() : 0;
    result = 31 * result + (profileLevel != null ? profileLevel.hashCode() : 0);
    result = 31 * result + (profileDescription != null ? profileDescription.hashCode() : 0);
    return result;
  }
}
