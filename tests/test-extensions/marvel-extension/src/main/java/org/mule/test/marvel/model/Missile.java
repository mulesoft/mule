/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.model;

import org.mule.runtime.api.connection.ConnectionException;

public class Missile {

  private boolean armed = true;

  public String fireAt(Villain target) {
    if (!isArmed()) {
      throw new RuntimeException(new ConnectionException("Disarmed missile"));
    }
    return target.takeHit(this);
  }

  public boolean isArmed() {
    return armed;
  }

  public void setArmed(boolean armed) {
    this.armed = armed;
  }
}
