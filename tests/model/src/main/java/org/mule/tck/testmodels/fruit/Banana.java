/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.fruit;

import java.util.EventObject;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Banana implements Fruit {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -1371515374040436874L;

  /**
   * logger used by this class
   */
  private static final Logger logger = LoggerFactory.getLogger(Banana.class);

  private boolean peeled = false;
  private boolean bitten = false;

  private String origin;

  public void peel() {
    peeled = true;
  }

  public void peelEvent(EventObject e) {
    logger.debug("Banana got peel event in peelEvent(EventObject)! MuleEvent says: " + e.getSource().toString());
    peel();
  }

  public boolean isPeeled() {
    return peeled;
  }

  @Override
  public void bite() {
    bitten = true;
  }

  @Override
  public boolean isBitten() {
    return bitten;
  }

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Banana) {
      Banana other = (Banana) o;
      return this.bitten == other.bitten && this.peeled == other.peeled
          && Objects.equals(origin, other.origin);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(peeled, bitten, origin);
  }
}
