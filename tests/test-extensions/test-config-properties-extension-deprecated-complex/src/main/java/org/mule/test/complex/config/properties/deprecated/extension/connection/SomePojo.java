/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.complex.config.properties.deprecated.extension.connection;

import java.util.Objects;

public class SomePojo {

  private String textValue;

  public String getTextValue() {
    return textValue;
  }

  public void setTextValue(String textValue) {
    this.textValue = textValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    SomePojo that = (SomePojo) o;
    return Objects.equals(textValue, that.textValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(textValue);
  }
}
