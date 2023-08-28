/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.subtypes.extension;

import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.Objects;

public class NoGlobalPojo {

  @Parameter
  private String name;

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

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    NoGlobalPojo that = (NoGlobalPojo) o;
    return number == that.number &&
        Objects.equals(name, that.name) &&
        Objects.equals(string, that.string);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, number, string);
  }
}
