/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
