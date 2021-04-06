/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connectivity.platform.schema;

import java.util.Objects;

public class DocumentRoot {

  private String encodes;

  public String getEncodes() {
    return encodes;
  }

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
    DocumentRoot that = (DocumentRoot) o;
    return Objects.equals(encodes, that.encodes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(encodes);
  }
}
