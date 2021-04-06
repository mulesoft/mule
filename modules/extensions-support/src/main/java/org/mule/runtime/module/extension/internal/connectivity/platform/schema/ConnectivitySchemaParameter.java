/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connectivity.platform.schema;

import java.util.Objects;

/**
 * Models a parameter within a {@link ConnectivitySchema}
 *
 * @since 4.4.0
 */
public class ConnectivitySchemaParameter {

  private String propertyTerm;
  private String range;
  private boolean mandatory;

  public String getPropertyTerm() {
    return propertyTerm;
  }

  public String getRange() {
    return range;
  }

  public boolean isMandatory() {
    return mandatory;
  }

  public void setPropertyTerm(String propertyTerm) {
    this.propertyTerm = propertyTerm;
  }

  public void setRange(String range) {
    this.range = range;
  }

  public void setMandatory(boolean mandatory) {
    this.mandatory = mandatory;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectivitySchemaParameter that = (ConnectivitySchemaParameter) o;
    return mandatory == that.mandatory && Objects.equals(propertyTerm, that.propertyTerm) && Objects.equals(range, that.range);
  }

  @Override
  public int hashCode() {
    return Objects.hash(propertyTerm, range, mandatory);
  }
}
