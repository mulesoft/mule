/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal.dsl.model;


import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.config.spring.api.dsl.model.ApplicationModel;
import org.mule.runtime.config.spring.api.dsl.model.ComponentModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(MinimalApplicationModelGenerator.class);

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
   * Resolves the minimal set of {@link ComponentModel}s for a component within a flow.
   *
   * @param location the component path in which the component is located.
   * @return the generated {@link ApplicationModel} with the minimal set of {@link ComponentModel}s required.
   * @throws NoSuchComponentModelException if the requested component does not exists.
   */
  public ApplicationModel getMinimalModel(Location location) {
    ComponentModel requestedComponentModel = dependencyResolver.findRequiredComponentModel(location);
    final Set<String> otherRequiredGlobalComponents = dependencyResolver.resolveComponentDependencies(requestedComponentModel);
    String requestComponentModelName = requestedComponentModel.getNameAttribute();
    if (requestComponentModelName != null
        && dependencyResolver.getApplicationModel().findTopLevelNamedElement(requestComponentModelName).isPresent()) {
      otherRequiredGlobalComponents.add(requestedComponentModel.getNameAttribute());
    }
    Set<String> allRequiredComponentModels = dependencyResolver.findComponentModelsDependencies(otherRequiredGlobalComponents);
    Iterator<ComponentModel> iterator =
        dependencyResolver.getApplicationModel().getRootComponentModel().getInnerComponents().iterator();
    while (iterator.hasNext()) {
      ComponentModel componentModel = iterator.next();
      if (componentModel.getNameAttribute() != null && allRequiredComponentModels.contains(componentModel.getNameAttribute())) {
        componentModel.setEnabled(true);
        componentModel.executedOnEveryInnerComponent(component -> component.setEnabled(true));
      }
    }
    ComponentModel currentComponentModel = requestedComponentModel;
    ComponentModel parentModel = currentComponentModel.getParent();
    while (parentModel != null && parentModel.getParent() != null) {
      parentModel.setEnabled(true);
      for (ComponentModel innerComponent : parentModel.getInnerComponents()) {
        if (!innerComponent.equals(currentComponentModel)) {
          innerComponent.setEnabled(false);
          innerComponent.executedOnEveryInnerComponent(component -> component.setEnabled(false));
        }
      }
      currentComponentModel = parentModel;
      parentModel = parentModel.getParent();
    }
    // Finally we set the requested componentModel as enabled as it could have been disabled when traversing dependencies
    requestedComponentModel.setEnabled(true);
    requestedComponentModel.executedOnEveryInnerComponent(componentModel -> componentModel.setEnabled(true));
    // mule root component model has to be enabled too
    this.dependencyResolver.getApplicationModel().getRootComponentModel().setEnabled(true);
    return dependencyResolver.getApplicationModel();
  }

}
