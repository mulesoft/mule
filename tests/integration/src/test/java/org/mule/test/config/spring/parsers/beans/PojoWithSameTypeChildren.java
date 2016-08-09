/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers.beans;

public class PojoWithSameTypeChildren {

  private SimpleCollectionObject elementTypeA;
  private SimpleCollectionObject anotherElementTypeA;

  public void setElementTypeA(SimpleCollectionObject elementTypeA) {
    this.elementTypeA = elementTypeA;
  }

  public void setAnotherElementTypeA(SimpleCollectionObject anotherElementTypeA) {
    this.anotherElementTypeA = anotherElementTypeA;
  }

  public SimpleCollectionObject getAnotherElementTypeA() {
    return anotherElementTypeA;
  }

  public SimpleCollectionObject getElementTypeA() {
    return elementTypeA;
  }
}
