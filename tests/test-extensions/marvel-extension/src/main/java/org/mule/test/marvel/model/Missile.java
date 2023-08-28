/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
