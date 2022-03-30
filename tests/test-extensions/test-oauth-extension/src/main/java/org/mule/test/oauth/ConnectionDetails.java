/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class ConnectionDetails {

  @Parameter
  @Optional
  private ConnectionProperties anotherConnectionProperties;

  @Parameter
  @Optional
  private Integer detailsPriority;

  public ConnectionDetails() {}

  public ConnectionDetails(ConnectionProperties anotherConnectionProperties, Integer detailsPriority) {
    this.anotherConnectionProperties = anotherConnectionProperties;
    this.detailsPriority = detailsPriority;
  }

  public ConnectionProperties getAnotherConnectionProperties() {
    return anotherConnectionProperties;
  }

  public Integer getDetailsPriority() {
    return detailsPriority;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ConnectionDetails that = (ConnectionDetails) o;

    if (anotherConnectionProperties != null ? !anotherConnectionProperties.equals(that.anotherConnectionProperties)
        : that.anotherConnectionProperties != null)
      return false;
    return detailsPriority != null ? detailsPriority.equals(that.detailsPriority) : that.detailsPriority == null;
  }

  @Override
  public int hashCode() {
    int result = anotherConnectionProperties != null ? anotherConnectionProperties.hashCode() : 0;
    result = 31 * result + (detailsPriority != null ? detailsPriority.hashCode() : 0);
    return result;
  }

}
