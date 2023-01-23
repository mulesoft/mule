/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.lazy;

import static org.mule.runtime.core.api.util.StringUtils.ifNotBlank;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * {@link BeanPostProcessor} that keeps track of the beans created after a certain point in order to later allow them to be
 * disposed (in the right dependency order).
 */
public class TrackingPostProcessor implements BeanPostProcessor {

  private final Set<String> trackingOrderedSet = new LinkedHashSet<>();
  private final Set<String> currentTrackingOrderedSet = new LinkedHashSet<>();
  private boolean tracking = false;

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (tracking) {
      ifNotBlank(beanName, currentTrackingOrderedSet::add);
    }
    return bean;
  }

  public Set<String> getBeansTracked() {
    return ImmutableSet.copyOf(trackingOrderedSet);
  }

  public List<String> getBeansTrackedInOrder() {
    return ImmutableList.copyOf(trackingOrderedSet);
  }

  public void startTracking() {
    tracking = true;
    currentTrackingOrderedSet.clear();
  }

  public void stopTracking() {
    tracking = false;
  }

  public void commitOnly(Collection<String> beanNames) {
    tracking = false;
    currentTrackingOrderedSet.stream()
        .filter(beanNames::contains)
        .forEach(trackingOrderedSet::add);
    currentTrackingOrderedSet.clear();
  }

  public void reset() {
    trackingOrderedSet.clear();
  }

}
