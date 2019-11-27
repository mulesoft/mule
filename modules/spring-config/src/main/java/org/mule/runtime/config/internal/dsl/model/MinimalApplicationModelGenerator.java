/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.config.internal.dsl.model.DependencyNode.Type.TOP_LEVEL;

import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableSet;

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
  private boolean ignoreAlwaysEnabled = false;

  /**
   * Creates a new instance.
   * 
   * @param dependencyResolver a {@link ConfigurationDependencyResolver} associated with an {@link ApplicationModel}
   */
  public MinimalApplicationModelGenerator(ConfigurationDependencyResolver dependencyResolver) {
    this(dependencyResolver, false);
  }

  /**
   * Creates a new instance of the minimal application generator.
   *
   * @param dependencyResolver a {@link ConfigurationDependencyResolver} associated with an {@link ApplicationModel}
   * @param ignoreAlwaysEnabled {@code true} if consider those components that will not be referenced and have to be enabled anyways.
   */
  public MinimalApplicationModelGenerator(ConfigurationDependencyResolver dependencyResolver, boolean ignoreAlwaysEnabled) {
    this.dependencyResolver = dependencyResolver;
    this.ignoreAlwaysEnabled = ignoreAlwaysEnabled;
  }

  /**
   * Resolves the minimal set of {@link ComponentModel componentModels} for the components requested.
   *
   * @param componentModels list of {@link ComponentModel componentModels} to be enabled.
   * @return the generated {@link ApplicationModel} with the minimal set of {@link ComponentModel}s required.
   */
  public ApplicationModel getMinimalModel(List<ComponentModel> componentModels) {
    componentModels.stream().forEach(componentModel -> {
      final DefaultComponentLocation componentLocation = componentModel.getComponentLocation();
      if (componentLocation != null) {
        enableComponentDependencies(componentModel);
      }
    });
    return dependencyResolver.getApplicationModel();
  }

  /**
   * Resolves list of {@link ComponentModel componentModels} for {@link LazyComponentInitializer.ComponentLocationFilter} given.
   *
   * @param predicate to select the {@link ComponentModel componentModels} to be enabled.
   * @return the filtered {@link ComponentModel} with the minimal set of {@link ComponentModel}s required.
   */
  public List<ComponentModel> getComponentModels(Predicate<ComponentModel> predicate) {
    return dependencyResolver.findRequiredComponentModels(predicate);
  }

  /**
   * Enables the {@link ComponentModel} and its dependencies in the {@link ApplicationModel}.
   *
   * @param requestedComponentModel the requested {@link ComponentModel} to be enabled.
   */
  private void enableComponentDependencies(ComponentModel requestedComponentModel) {
    final String requestComponentModelName = requestedComponentModel.getNameAttribute();
    final Set<DependencyNode> componentDependencies = dependencyResolver.resolveComponentDependencies(requestedComponentModel);
    final Set<DependencyNode> alwaysEnabledComponents =
        ignoreAlwaysEnabled ? emptySet() : dependencyResolver.resolveAlwaysEnabledComponents();
    final ImmutableSet.Builder<DependencyNode> otherRequiredGlobalComponentsSetBuilder =
        ImmutableSet.<DependencyNode>builder().addAll(componentDependencies)
            .addAll(alwaysEnabledComponents.stream().filter(dependencyNode -> dependencyNode.isTopLevel()).collect(toList()));
    if (requestComponentModelName != null
        && dependencyResolver.getApplicationModel().findTopLevelNamedComponent(requestComponentModelName).isPresent()) {
      otherRequiredGlobalComponentsSetBuilder.add(new DependencyNode(requestComponentModelName, TOP_LEVEL));
    }
    Set<DependencyNode> allRequiredComponentModels = resolveDependencies(otherRequiredGlobalComponentsSetBuilder.build());
    enableTopLevelElementDependencies(allRequiredComponentModels);
    enableInnerElementDependencies(allRequiredComponentModels);

    ComponentModel parentModel = requestedComponentModel.getParent();
    while (parentModel != null && parentModel.getParent() != null) {
      parentModel.setEnabled(true);
      parentModel = parentModel.getParent();
    }

    alwaysEnabledComponents.stream()
        .filter(dependencyNode -> dependencyNode.isUnnamedTopLevel() && dependencyNode.getComponentIdentifier().isPresent())
        .forEach(dependencyNode -> dependencyResolver.getApplicationModel()
            .findComponentDefinitionModels(dependencyNode.getComponentIdentifier().get())
            .forEach(componentModel -> {
              componentModel.setEnabled(true);
              componentModel.executedOnEveryInnerComponent(innerComponent -> innerComponent.setEnabled(true));
            }));


    // Finally we set the requested componentModel as enabled as it could have been disabled when traversing dependencies
    requestedComponentModel.setEnabled(true);
    requestedComponentModel.executedOnEveryInnerComponent(componentModel -> componentModel.setEnabled(true));
    enableParentComponentModels(requestedComponentModel);

    // Mule root component model has to be enabled too
    this.dependencyResolver.getApplicationModel().getRootComponentModel().setEnabled(true);
  }

  private void enableInnerElementDependencies(Set<DependencyNode> allRequiredComponentModels) {
    Set<String> noneTopLevelDendencyNames = allRequiredComponentModels.stream()
        .filter(dependencyNode -> !dependencyNode.isTopLevel())
        .map(dependencyNode -> dependencyNode.getComponentName())
        .collect(toSet());
    dependencyResolver.getApplicationModel().executeOnEveryComponentTree(componentModel -> {
      if (!componentModel.isEnabled() && componentModel.getNameAttribute() != null
          && noneTopLevelDendencyNames.contains(componentModel.getNameAttribute())) {
        componentModel.setEnabled(true);
        componentModel.executedOnEveryInnerComponent(component -> component.setEnabled(true));
        enableParentComponentModels(componentModel);
      }
    });
  }

  private void enableTopLevelElementDependencies(Set<DependencyNode> allRequiredComponentModels) {
    Set<String> topLevelDendencyNames = allRequiredComponentModels.stream()
        .filter(dependencyNode -> dependencyNode.isTopLevel())
        .map(dependencyNode -> dependencyNode.getComponentName())
        .collect(toSet());

    Iterator<ComponentModel> iterator =
        dependencyResolver.getApplicationModel().getRootComponentModel().getInnerComponents().iterator();
    while (iterator.hasNext()) {
      ComponentModel componentModel = iterator.next();
      if (componentModel.getNameAttribute() != null && topLevelDendencyNames.contains(componentModel.getNameAttribute())) {
        componentModel.setEnabled(true);
        componentModel.executedOnEveryInnerComponent(component -> component.setEnabled(true));
      }
    }
  }

  private void enableParentComponentModels(ComponentModel requestedComponentModel) {
    ComponentModel parentModel = requestedComponentModel.getParent();
    while (parentModel != null && parentModel.getParent() != null) {
      parentModel.setEnabled(true);
      parentModel = parentModel.getParent();
    }
  }

  /**
   * Resolve all the dependencies for an initial components set.
   *
   * @param initialComponents {@ling Set} of initial components to retrieve their dependencies
   * @return a new {@ling Set} with all the dependencies needed to run all the initial components
   */
  private Set<DependencyNode> resolveDependencies(Set<DependencyNode> initialComponents) {
    Set<DependencyNode> difference = initialComponents;
    Set<DependencyNode> allRequiredComponentModels = new HashSet<>(initialComponents);

    // While there are new dependencies resolved, calculate their dependencies
    // This fixes bugs related to not resolving dependencies of dependencies, such as a config for a config
    // e.g. tlsContext for http request, or a flow-ref inside a flow that is being referenced in another flow.
    while (difference.size() > 0) {
      // Only calculate the dependencies for the difference, to avoid recalculating
      Set<DependencyNode> newDependencies = dependencyResolver.findComponentModelsDependencies(difference);
      newDependencies.removeAll(allRequiredComponentModels);
      allRequiredComponentModels.addAll(newDependencies);
      difference = newDependencies;
    }
    return allRequiredComponentModels;
  }

}
