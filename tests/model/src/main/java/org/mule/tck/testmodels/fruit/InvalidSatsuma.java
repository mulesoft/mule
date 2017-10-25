/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
