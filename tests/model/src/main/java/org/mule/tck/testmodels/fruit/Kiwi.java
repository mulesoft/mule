/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
