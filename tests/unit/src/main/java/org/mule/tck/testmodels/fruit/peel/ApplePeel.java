/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.fruit.peel;

public class ApplePeel {

  private boolean eatable = true;

  private ApplePeel recursivePeel;

  public boolean isEatable() {
    return eatable;
  }

  public ApplePeel getRecursivePeel() {
    return recursivePeel;
  }
}
