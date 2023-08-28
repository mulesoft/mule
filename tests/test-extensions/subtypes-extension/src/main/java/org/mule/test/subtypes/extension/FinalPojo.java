/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.subtypes.extension;

import org.mule.runtime.api.component.AbstractComponent;

import java.util.Objects;

public final class FinalPojo extends AbstractComponent {

  private String someString;

  public String getSomeString() {
    return someString;
  }

  public void setSomeString(String someString) {
    this.someString = someString;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    FinalPojo finalPojo = (FinalPojo) o;
    return Objects.equals(someString, finalPojo.someString);
  }

  @Override
  public int hashCode() {
    return Objects.hash(someString);
  }
}
