/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.fruit;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Callable;

import org.mule.tck.testmodels.fruit.peel.ApplePeel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Apple implements Fruit, Callable, MuleContextAware {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -7631993371500076921L;

  /**
   * logger used by this class
   */
  private static final Logger logger = LoggerFactory.getLogger(Apple.class);

  private boolean bitten = false;
  private boolean washed = false;

  private FruitCleaner appleCleaner;

  private Seed seed;

  private MuleContext muleContext;

  private ApplePeel peel;

  public Apple() {}

  public Apple(boolean bitten) {
    this.bitten = bitten;
  }

  public void wash() {
    if (appleCleaner != null) {
      appleCleaner.wash(this);
    }
    washed = true;
  }

  public void polish() {
    appleCleaner.polish(this);
  }

  public boolean isWashed() {
    return washed;
  }

  @Override
  public void bite() {
    bitten = true;
  }

  @Override
  public boolean isBitten() {
    return bitten;
  }

  public Seed getSeed() {
    return seed;
  }

  public void setSeed(Seed seed) {
    this.seed = seed;
  }

  @Override
  public Object onCall(MuleEventContext context) throws MuleException {
    logger.debug("Apple received an event in Callable.onEvent! MuleEvent says: " + context.getMessageAsString(muleContext));
    wash();
    return null;
  }


  public FruitCleaner getAppleCleaner() {
    return appleCleaner;
  }

  public void setAppleCleaner(FruitCleaner cleaner) {
    this.appleCleaner = cleaner;
  }

  public Object methodReturningNull() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final Apple apple = (Apple) o;

    if (bitten != apple.bitten) {
      return false;
    }
    if (washed != apple.washed) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    result = (bitten ? 1 : 0);
    result = 29 * result + (washed ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Just an apple.";
  }

  public void setBitten(boolean bitten) {
    this.bitten = bitten;
  }

  public void setWashed(boolean washed) {
    this.washed = washed;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }

  public ApplePeel getPeel() {
    return peel;
  }
}
