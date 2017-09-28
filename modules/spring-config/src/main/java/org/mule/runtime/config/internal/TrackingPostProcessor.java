/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.core.api.util.StringUtils.ifNotBlank;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * {@link BeanPostProcessor} that keeps track of the beans created after a certain point in order to later
 * allow them to be disposed (in the right dependency order).
 */
public class TrackingPostProcessor implements BeanPostProcessor {

  private List<String> trackingList = new ArrayList<>();
  private boolean tracking = false;

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (tracking) {
      ifNotBlank(beanName, value -> {
        if (!trackingList.contains(value)) {
          trackingList.add(value);
        }
      });
    }
    return bean;
  }

  public List<String> getBeansTracked() {
    return ImmutableList.copyOf(trackingList);
  }

  public void startTracking() {
    tracking = true;
    trackingList.clear();
  }

  public void stopTracking() {
    tracking = false;
  }

  public void intersection(List<String> beanNames) {
    trackingList.removeIf(name -> !beanNames.contains(name));
  }
}
