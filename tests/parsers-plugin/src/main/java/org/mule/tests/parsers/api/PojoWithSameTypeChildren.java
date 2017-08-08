/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.parsers.api;

public class PojoWithSameTypeChildren {

  private ParsersTestObject elementTypeA;
  private ParsersTestObject anotherElementTypeA;

  public void setElementTypeA(ParsersTestObject elementTypeA) {
    this.elementTypeA = elementTypeA;
  }

  public void setAnotherElementTypeA(ParsersTestObject anotherElementTypeA) {
    this.anotherElementTypeA = anotherElementTypeA;
  }

  public ParsersTestObject getAnotherElementTypeA() {
    return anotherElementTypeA;
  }

  public ParsersTestObject getElementTypeA() {
    return elementTypeA;
  }
}
