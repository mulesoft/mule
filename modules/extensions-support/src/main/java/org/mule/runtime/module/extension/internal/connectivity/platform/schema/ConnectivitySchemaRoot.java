/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connectivity.platform.schema;

import java.util.Objects;

/**
 * Models the root element of the schema document
 *
 * @since 4.4.0
 */
public class ConnectivitySchemaRoot {

  private String encodes;

  /**
   * @return the entity type encoded in the document
   */
  public String getEncodes() {
    return encodes;
  }

  /**
   * Sets the entity type encoded in the document
   * @param encodes the entity type
   */
  void setEncodes(String encodes) {
    this.encodes = encodes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectivitySchemaRoot that = (ConnectivitySchemaRoot) o;
    return Objects.equals(encodes, that.encodes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(encodes);
  }
}
