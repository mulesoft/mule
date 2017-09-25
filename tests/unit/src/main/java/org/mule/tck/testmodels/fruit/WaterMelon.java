/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.fruit;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class WaterMelon implements Fruit, Startable, Stoppable, Disposable {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -8860598811203869100L;

  /**
   * logger used by this class
   */
  private static final Logger logger = LoggerFactory.getLogger(WaterMelon.class);

  private boolean bitten = false;
  private Integer seeds = new Integer(100);
  private Double radius = new Double(4.34);
  private String brand;
  private String state = "void";

  public WaterMelon() {
    super();
  }

  public WaterMelon(HashMap props) throws MuleException {
    logger.info("Initialisaing Water melon with hashmap constructor");
    setBrand((String) props.get("namespace.brand"));
    setRadius((Double) props.get("another.namespace.radius"));
    setSeeds((Integer) props.get("seeds"));
    state = "initialised";
  }

  @Override
  public void bite() {
    bitten = true;
  }

  @Override
  public boolean isBitten() {
    return bitten;
  }

  public void myEventHandler(CoreEvent event, MuleContext muleContext) throws MuleException {
    logger.debug("Water Melon received an event in MyEventHandler! MuleEvent says: "
        + ((PrivilegedEvent) event).getMessageAsString(null));
    bite();
  }

  public String getBrand() {
    return brand;
  }

  public Integer getSeeds() {
    return seeds;
  }

  public Double getRadius() {
    return radius;
  }

  public void setBrand(String string) {
    brand = string;
  }

  public void setSeeds(Integer integer) {
    seeds = integer;
  }

  public void setRadius(Double double1) {
    radius = double1;
  }

  public String getState() {
    return state;
  }

  @Override
  public void start() {
    state = "started";
  }

  @Override
  public void stop() {
    state = "stopped";
  }

  @Override
  public void dispose() {
    state = "disposed";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof WaterMelon) {
      WaterMelon melon = (WaterMelon) obj;
      return (getBrand().equals(melon.getBrand()) && getRadius().equals(melon.getRadius()) && getSeeds().equals(melon.getSeeds())
          && getState().equals(getState()));
    }

    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    int result;
    result = (bitten ? 1 : 0);
    result = 31 * result + seeds.hashCode();
    result = 31 * result + radius.hashCode();
    result = 31 * result + (brand != null ? brand.hashCode() : 0);
    result = 31 * result + state.hashCode();
    return result;
  }
}
