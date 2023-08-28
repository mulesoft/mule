/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
