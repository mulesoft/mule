/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
