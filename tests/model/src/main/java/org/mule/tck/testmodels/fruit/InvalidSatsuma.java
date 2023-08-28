/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.testmodels.fruit;

/**
 * <code>InvalidSatsuma</code> has no discoverable methods
 */
public class InvalidSatsuma implements Fruit {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -6328691504772842584L;

  private boolean bitten = false;

  public void bite() {
    bitten = true;

  }

  public boolean isBitten() {
    return bitten;
  }
}
