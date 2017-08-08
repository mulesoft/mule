/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.parsers.api;

public class ParameterAndChildElement {

  private SimplePojo simplePojo;

  public SimplePojo getSimplePojo() {
    return simplePojo;
  }

  public void setSimplePojo(SimplePojo simplePojo) {
    this.simplePojo = simplePojo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ParameterAndChildElement that = (ParameterAndChildElement) o;
    return simplePojo != null ? simplePojo.equals(that.simplePojo) : that.simplePojo == null;

  }

  @Override
  public int hashCode() {
    return simplePojo != null ? simplePojo.hashCode() : 0;
  }
}
