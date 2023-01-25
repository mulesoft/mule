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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Representation of the current initialization state, including only things that are relevant in the context of lazy
 * initialization of components.
 */
class ComponentInitializationState {

  private final ConfigurationComponentLocator componentLocator;
  private final TrackingPostProcessor trackingPostProcessor = new TrackingPostProcessor();
  private final Set<String> currentComponentLocationsRequested = new HashSet<>();
  private final List<ConfigurableObjectProvider> objectProvidersToConfigure = new ArrayList<>();
  private boolean isApplyStartPhaseRequested = false;
  private boolean isInitializationAlreadyDone = false;

  /**
   * Creates the instance.
   *
   * @param componentLocator A {@link ConfigurationComponentLocator} to help in determining if a component is already initialized.
   */
  ComponentInitializationState(ConfigurationComponentLocator componentLocator) {
    this.componentLocator = componentLocator;
  }

  /**
   * @return Whether it has been requested to also apply the start lifecycle phase to the components.
   */
  public boolean isApplyStartPhaseRequested() {
    return isApplyStartPhaseRequested;
  }

  /**
   * @return Whether some initialization has already been done (even if not fully compatible with the current request).
   */
  public boolean isInitializationAlreadyDone() {
    return isInitializationAlreadyDone;
  }

  /**
   * Registers the {@link TrackingPostProcessor} with the given {@link ConfigurableListableBeanFactory}.
   *
   * @param beanFactory The {@link ConfigurableListableBeanFactory} to register the bean post processor with.
   */
  public void registerTrackingPostProcessor(ConfigurableListableBeanFactory beanFactory) {
    beanFactory.addBeanPostProcessor(trackingPostProcessor);
  }

  /**
   * Starts tracking beans created from this point and until either {@link #commitTrackedBeansContainedIn(Collection)} or
   * {@link #clear()} are called.
   */
  public void startTrackingBeans() {
    trackingPostProcessor.startTracking();
  }

  /**
   * Stops tracking beans and adds the ones that had been tracked since the last call to {@link #startTrackingBeans()} to the
   * remembered list.
   *
   * @param beanNames A {@link Collection} of bean names that we are interested in remembering from the last batch. Other created
   *                  beans will not be added to the remembered list.
   */
  public void commitTrackedBeansContainedIn(Collection<String> beanNames) {
    trackingPostProcessor.commitOnly(beanNames);
  }

  /**
   * @return The full list of beans that have been created while tracking was enabled and that have been committed using
   *         {@link #commitTrackedBeansContainedIn(Collection)}.
   *         <p>
   *         The bean names are given in the same order they were created.
   */
  public List<String> getTrackedBeansInOrder() {
    return trackingPostProcessor.getBeansTrackedInOrder();
  }

  /**
   * Registers a discovered {@link ConfigurableObjectProvider} so that it can be eventually configured.
   *
   * @param objectProvider The {@link ConfigurableObjectProvider} to register.
   * @see #takeObjectProvidersToConfigure()
   */
  public void registerObjectProviderToConfigure(ConfigurableObjectProvider objectProvider) {
    objectProvidersToConfigure.add(objectProvider);
  }

  /**
   * @return The full list of {@link ConfigurableObjectProvider}s that had been registered to be configured.
   *         <p>
   *         This operation also removes the returned providers from the registration list, so they don't get returned again.
   * @see #registerObjectProviderToConfigure(ConfigurableObjectProvider)
   */
  public List<ConfigurableObjectProvider> takeObjectProvidersToConfigure() {
    List<ConfigurableObjectProvider> returnValue = new ArrayList<>(objectProvidersToConfigure);
    objectProvidersToConfigure.clear();
    return returnValue;
  }

  /**
   * @param componentInitializationRequest A {@link ComponentInitializationRequest}.
   * @return Whether the given {@code componentInitializationRequest} is satisfied by the current state.
   */
  public boolean isRequestSatisfied(ComponentInitializationRequest componentInitializationRequest) {
    return areLocationsSatisfied(componentInitializationRequest)
        && isApplyStartPhaseRequested == componentInitializationRequest.isApplyStartPhaseRequested();
  }

  /**
   * @param componentAst A {@link ComponentAst} to test.
   * @return Whether a particular {@code componentAst} has already been initialized.
   */
  public boolean isComponentAlreadyInitialized(ComponentAst componentAst) {
    return componentAst.getLocation() == null || componentLocator.find(getLocation(componentAst)).isPresent();
  }

  /**
   * Updates the current state assuming the given {@code componentInitializationRequest} has been just processed.
   *
   * @param componentInitializationRequest A {@link ComponentInitializationRequest}.
   */
  public void update(ComponentInitializationRequest componentInitializationRequest) {
    isInitializationAlreadyDone = true;
    if (!componentInitializationRequest.isKeepPreviousRequested()) {
      currentComponentLocationsRequested.clear();
      trackingPostProcessor.reset();
    }
    currentComponentLocationsRequested.addAll(componentInitializationRequest.getRequestedLocations());
    isApplyStartPhaseRequested = componentInitializationRequest.isApplyStartPhaseRequested();
  }

  /**
   * Clears all the state, reverting to how it was upon instance creation.
   * <p>
   * Only state that is owned by this instance will be affected. As such, the {@link ConfigurationComponentLocator} given in the
   * {@link #ComponentInitializationState constructor} is external and will not be affected.
   */
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
