/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.fruit;

/**
 * A test object not implementing Callable, but having a matching method accepting MuleEventContext.
 */
public class Kiwi implements Fruit {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -1468423665948468954L;

  private boolean bitten;

  @Override
  public void bite() {
    this.bitten = true;
  }

  @Override
  public boolean isBitten() {
    return this.bitten;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Kiwi)) {
      return false;
    }

    Kiwi kiwi = (Kiwi) o;

    if (bitten != kiwi.bitten) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return (bitten ? 1 : 0);
  }
}
