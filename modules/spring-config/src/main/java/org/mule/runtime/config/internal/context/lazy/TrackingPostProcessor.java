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

  // It is important that the actual Set implementations retain the insertion order in order to satisfy the getBeansTrackedInOrder
  // contract.
  // We could also use a List but that would increase the complexity of registering each bean to avoid duplicates.
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

  /**
   * @return The full set of beans that have been created while tracking was enabled and that have been committed using
   *         {@link #commitOnly(Collection)}.
   * @see #getBeansTrackedInOrder()
   */
  public Set<String> getBeansTracked() {
    return ImmutableSet.copyOf(trackingOrderedSet);
  }

  /**
   * @return The full list of beans that have been created while tracking was enabled and that have been committed using
   *         {@link #commitOnly(Collection)}.
   *         <p>
   *         The bean names are given in the same order they were created.
   * @see #getBeansTracked()
   */
  public List<String> getBeansTrackedInOrder() {
    return ImmutableList.copyOf(trackingOrderedSet);
  }

  /**
   * Starts tracking beans created from this point and until either {@link #commitOnly(Collection)} or {@link #stopTracking()} are
   * called.
   */
  public void startTracking() {
    tracking = true;
    currentTrackingOrderedSet.clear();
  }

  /**
   * Stops tracking beans.
   * <p>
   * Calling this method will also discard any bean names being recorded since the last call to {@link #startTracking()}.
   *
   * @see #commitOnly(Collection)
   */
  public void stopTracking() {
    tracking = false;
    currentTrackingOrderedSet.clear();
  }

  /**
   * Stops tracking beans and adds the ones that had been tracked since the last call to {@link #startTracking()} to the
   * remembered list.
   *
   * @param beanNames A {@link Collection} of bean names that we are interested in remembering from the last batch. Other created
   *                  beans will not be added to the remembered list.
   */
  public void commitOnly(Collection<String> beanNames) {
    tracking = false;
    currentTrackingOrderedSet.stream()
        .filter(beanNames::contains)
        .forEach(trackingOrderedSet::add);
    currentTrackingOrderedSet.clear();
  }

  /**
   * Clears all the state, reverting to how it was upon instance creation.
   */
  public void reset() {
    trackingOrderedSet.clear();
    currentTrackingOrderedSet.clear();
    tracking = false;
  }

}
