/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.specific;

import org.mule.runtime.core.time.Time;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} which creates instances of {@link Time}
 */
public class TimeFactoryBean implements FactoryBean<Time> {

  private long frequency;
  private TimeUnit timeUnit;

  @Override
  public Time getObject() throws Exception {
    return new Time(frequency, timeUnit);
  }

  @Override
  public Class<?> getObjectType() {
    return Time.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  public void setFrequency(long frequency) {
    this.frequency = frequency;
  }

  public void setTimeUnit(TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
  }
}
