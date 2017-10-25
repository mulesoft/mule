/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.fruit;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.util.concurrent.Latch;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class Seed implements Serializable, Initialisable {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -7631993371500076921L;

  private Latch initLatch = new Latch();

  private Fruit fruit;

  public Fruit getFruit() {
    return fruit;
  }

  public void setFruit(Fruit fruit) {
    this.fruit = fruit;
  }

  @Override
  public void initialise() throws InitialisationException {
    initLatch.countDown();
  }

  public void awaitInitialize(long timeout, TimeUnit unit) throws InterruptedException {
    initLatch.await(timeout, unit);
  }
}
