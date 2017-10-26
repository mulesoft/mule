/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;

import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Generates the minimal required component set to create a configuration component (i.e.: file:config, ftp:connection, a flow
 * MP). This set is defined by the component dependencies.
 * <p/>
 * Based on the requested component, the {@link ComponentModel} configuration associated is introspected to find it dependencies
 * based on it's {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition}. This process is recursively done for each
 * of the dependencies in order to find all the required {@link ComponentModel}s that must be created for the requested
 * {@link ComponentModel} to work properly.
 *
 * @since 4.0
 */
// TODO MULE-9688 - refactor this class when the ComponentModel becomes immutable
public class MinimalApplicationModelGenerator {

  private ConfigurationDependencyResolver dependencyResolver;

  /**
   * Creates a new instance.
   * 
   * @param dependencyResolver a {@link ConfigurationDependencyResolver} associated with an {@link ApplicationModel}
   */
  public MinimalApplicationModelGenerator(ConfigurationDependencyResolver dependencyResolver) {
    this.dependencyResolver = dependencyResolver;
  }

  /**
   * Resolves the minimal set of {@link ComponentModel componentModels} for the components that pass the
   * {@link LazyComponentInitializer.ComponentLocationFilter}.
   *
   * @param filter to select the {@link ComponentModel componentModels} to be enabled.
   * @return the generated {@link ApplicationModel} with the minimal set of {@link ComponentModel}s required.
   */
  public ApplicationModel getMinimalModel(LazyComponentInitializer.ComponentLocationFilter filter) {
    List<ComponentModel> required = dependencyResolver.findRequiredComponentModels(filter);

    required.stream().forEach(componentModel -> {
      final DefaultComponentLocation componentLocation = componentModel.getComponentLocation();
      if (componentLocation != null) {
        enableComponentDependencies(componentModel);
      }
    });
    return dependencyResolver.getApplicationModel();
  }

  /**
   * Resolves the minimal set of {@link ComponentModel componentModels} for the component.
   *
   * @param location {@link Location} for the requested component to be enabled.
   * @return the generated {@link ApplicationModel} with the minimal set of {@link ComponentModel}s required.
   * @throws NoSuchComponentModelException if the location doesn't match to a component.
   */
  public ApplicationModel getMinimalModel(Location location) {
    ComponentModel requestedComponentModel = dependencyResolver.findRequiredComponentModel(location);
    enableComponentDependencies(requestedComponentModel);
    return dependencyResolver.getApplicationModel();
  }

  /**
   * Enables the {@link ComponentModel} and its dependencies in the {@link ApplicationModel}.
   *
   * @param requestedComponentModel the requested {@link ComponentModel} to be enabled.
   */
  private void enableComponentDependencies(ComponentModel requestedComponentModel) {
    final String requestComponentModelName = requestedComponentModel.getNameAttribute();
    final Set<String> componentDependencies = dependencyResolver.resolveComponentDependencies(requestedComponentModel);
    final Set<String> alwaysEnabledComponents = dependencyResolver.resolveAlwaysEnabledComponents();
    final ImmutableSet.Builder<String> otherRequiredGlobalComponentsSetBuilder =
        ImmutableSet.<String>builder().addAll(componentDependencies).addAll(alwaysEnabledComponents);
    if (requestComponentModelName != null
        && dependencyResolver.getApplicationModel().findTopLevelNamedElement(requestComponentModelName).isPresent()) {
      otherRequiredGlobalComponentsSetBuilder.add(requestComponentModelName);
    }

    Set<String> allRequiredComponentModels = resolveDependencies(otherRequiredGlobalComponentsSetBuilder.build());

    Iterator<ComponentModel> iterator =
        dependencyResolver.getApplicationModel().getRootComponentModel().getInnerComponents().iterator();
    while (iterator.hasNext()) {
      ComponentModel componentModel = iterator.next();
      if (componentModel.getNameAttribute() != null && allRequiredComponentModels.contains(componentModel.getNameAttribute())) {
        componentModel.setEnabled(true);
        componentModel.executedOnEveryInnerComponent(component -> component.setEnabled(true));
      }
    }

    ComponentModel parentModel = requestedComponentModel.getParent();
    while (parentModel != null && parentModel.getParent() != null) {
      parentModel.setEnabled(true);
      parentModel = parentModel.getParent();
    }

    // Finally we set the requested componentModel as enabled as it could have been disabled when traversing dependencies
    requestedComponentModel.setEnabled(true);
    requestedComponentModel.executedOnEveryInnerComponent(componentModel -> componentModel.setEnabled(true));
    // Mule root component model has to be enabled too
    this.dependencyResolver.getApplicationModel().getRootComponentModel().setEnabled(true);
  }

  /**
   * Resolve all the dependencies for an initial components set.
   *
   * @param initialComponents {@ling Set} of initial components to retrieve their dependencies
   * @return a new {@ling Set} with all the dependencies needed to run all the initial components
   */
  private Set<String> resolveDependencies(Set<String> initialComponents) {
    Set<String> difference = initialComponents;
    Set<String> allRequiredComponentModels = new HashSet<>(initialComponents);

    // While there are new dependencies resolved, calculate their dependencies
    // This fixes bugs related to not resolving dependencies of dependencies, such as a config for a config
    // e.g. tlsContext for http request, or a flow-ref inside a flow that is being referenced in another flow.
    while (difference.size() > 0) {
      // Only calculate the dependencies for the difference, to avoid recalculating
      Set<String> newDependencies = dependencyResolver.findComponentModelsDependencies(difference);
      newDependencies.removeAll(allRequiredComponentModels);
      allRequiredComponentModels.addAll(newDependencies);
      difference = newDependencies;
    }
    return allRequiredComponentModels;
  }

}
