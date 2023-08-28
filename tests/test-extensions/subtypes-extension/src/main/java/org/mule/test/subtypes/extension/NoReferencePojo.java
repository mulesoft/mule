/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.subtypes.extension;

import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.Objects;

public class NoReferencePojo {

  @Parameter
  private int number;

  @Parameter
  private String string;

  public int getNumber() {
    return number;
  }

  public String getString() {
    return string;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    NoReferencePojo that = (NoReferencePojo) o;
    return number == that.number &&
        Objects.equals(string, that.string);
  }

  @Override
  public int hashCode() {
    return Objects.hash(number, string);
  }
}
