/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.lazy;

import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.ioc.ConfigurableObjectProvider;
import org.mule.runtime.ast.api.ComponentAst;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

class ComponentInitializationState {

  private final ConfigurationComponentLocator componentLocator;
  private final TrackingPostProcessor trackingPostProcessor = new TrackingPostProcessor();
  private final Set<String> currentComponentLocationsRequested = new HashSet<>();
  private final List<ConfigurableObjectProvider> objectProvidersToConfigure = new ArrayList<>();
  private boolean isApplyStartPhaseRequested = false;
  private boolean isInitializationAlreadyDone = false;

  ComponentInitializationState(ConfigurationComponentLocator componentLocator) {
    this.componentLocator = componentLocator;
  }

  public boolean isApplyStartPhaseRequested() {
    return isApplyStartPhaseRequested;
  }

  public boolean isInitializationAlreadyDone() {
    return isInitializationAlreadyDone;
  }

  public TrackingPostProcessor getTrackingPostProcessor() {
    return trackingPostProcessor;
  }

  public void registerTrackingPostProcessor(ConfigurableListableBeanFactory beanFactory) {
    beanFactory.addBeanPostProcessor(trackingPostProcessor);
  }

  public void registerObjectProviderToConfigure(ConfigurableObjectProvider objectProvider) {
    objectProvidersToConfigure.add(objectProvider);
  }

  public List<ConfigurableObjectProvider> takeObjectProvidersToConfigure() {
    List<ConfigurableObjectProvider> returnValue = new ArrayList<>(objectProvidersToConfigure);
    objectProvidersToConfigure.clear();
    return returnValue;
  }

  public boolean isRequestSatisfied(ComponentInitializationRequest componentInitializationRequest) {
    return areLocationsSatisfied(componentInitializationRequest)
        && isApplyStartPhaseRequested == componentInitializationRequest.isApplyStartPhaseRequested();
  }

  public boolean isComponentAlreadyInitialized(ComponentAst componentAst) {
    return componentAst.getLocation() == null || componentLocator.find(getLocation(componentAst)).isPresent();
  }

  public void update(ComponentInitializationRequest componentInitializationRequest) {
    isInitializationAlreadyDone = true;
    if (!componentInitializationRequest.isKeepPreviousRequested()) {
      currentComponentLocationsRequested.clear();
      trackingPostProcessor.reset();
    }
    currentComponentLocationsRequested.addAll(componentInitializationRequest.getRequestedLocations());
    isApplyStartPhaseRequested = componentInitializationRequest.isApplyStartPhaseRequested();
  }

  public void clear() {
    trackingPostProcessor.stopTracking();
    trackingPostProcessor.reset();

    isApplyStartPhaseRequested = false;
    isInitializationAlreadyDone = false;
    currentComponentLocationsRequested.clear();
    objectProvidersToConfigure.clear();
  }

  private Location getLocation(ComponentAst componentAst) {
    return builderFromStringRepresentation(componentAst.getLocation().getLocation()).build();
  }

  private boolean areLocationsSatisfied(ComponentInitializationRequest componentInitializationRequest) {
    Set<String> requestedLocations = componentInitializationRequest.getRequestedLocations();
    return componentInitializationRequest.isKeepPreviousRequested()
        ? currentComponentLocationsRequested.containsAll(requestedLocations)
        : currentComponentLocationsRequested.equals(requestedLocations);
  }
}
