/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.api.dsl.processor.AbstractAttributeDefinitionVisitor;
import org.mule.runtime.config.internal.BeanDependencyResolver;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair;

import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationDependencyResolver implements BeanDependencyResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationDependencyResolver.class);

  private final ApplicationModel applicationModel;
  private final ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry;

  /**
   * Creates a new instance associated to a complete {@link ApplicationModel}.
   *
   * @param applicationModel the artifact {@link ApplicationModel}.
   * @param componentBuildingDefinitionRegistry the registry to find the
   *        {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition}s associated to each {@link ComponentModel} that
   *        must be resolved.
   */
  public ConfigurationDependencyResolver(ApplicationModel applicationModel,
                                         ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry) {
    this.applicationModel = applicationModel;
    this.componentBuildingDefinitionRegistry = componentBuildingDefinitionRegistry;
  }

  private Set<String> resolveComponentModelDependencies(ComponentModel componentModel) {
    final Set<String> otherRequiredGlobalComponents = resolveComponentDependencies(componentModel);
    return findComponentModelsDependencies(otherRequiredGlobalComponents);
  }

  protected Set<String> resolveComponentDependencies(ComponentModel requestedComponentModel) {
    Set<String> otherDependencies = new HashSet<>();
    requestedComponentModel.getInnerComponents()
        .stream().forEach(childComponent -> otherDependencies.addAll(resolveComponentDependencies(childComponent)));
    final Set<String> parametersReferencingDependencies = new HashSet<>();
    componentBuildingDefinitionRegistry.getBuildingDefinition(requestedComponentModel.getIdentifier())
        .ifPresent(buildingDefinition -> buildingDefinition.getAttributesDefinitions()
            .stream().forEach(attributeDefinition -> {
              attributeDefinition.accept(new AbstractAttributeDefinitionVisitor() {

                @Override
                public void onMultipleValues(KeyAttributeDefinitionPair[] definitions) {
                  stream(definitions)
                      .forEach(keyAttributeDefinitionPair -> keyAttributeDefinitionPair.getAttributeDefinition().accept(this));
                }

                @Override
                public void onReferenceSimpleParameter(String reference) {
                  parametersReferencingDependencies.add(reference);
                }
              });
            }));

    for (String parametersReferencingDependency : parametersReferencingDependencies) {
      if (requestedComponentModel.getParameters().containsKey(parametersReferencingDependency)) {
        appendDependency(otherDependencies, requestedComponentModel, parametersReferencingDependency);
      }
    }

    // Special cases for flow-ref and configuration
    if (isCoreComponent(requestedComponentModel.getIdentifier(), "flow-ref")) {
      appendDependency(otherDependencies, requestedComponentModel, "name");
    } else if (isCoreComponent(requestedComponentModel.getIdentifier(), "configuration")) {
      appendDependency(otherDependencies, requestedComponentModel, "defaultErrorHandler-ref");
    }

    return otherDependencies;
  }

  protected Set<String> findComponentModelsDependencies(Set<String> componentModelNames) {
    Set<String> componentsToSearchDependencies = new HashSet<>(componentModelNames);
    Set<String> foundDependencies = new LinkedHashSet<>();
    Set<String> alreadySearchedDependencies = new HashSet<>();
    do {
      componentsToSearchDependencies.addAll(foundDependencies);
      for (String componentModelName : componentsToSearchDependencies) {
        if (!alreadySearchedDependencies.contains(componentModelName)) {
          alreadySearchedDependencies.add(componentModelName);
          foundDependencies.addAll(resolveComponentDependencies(findRequiredComponentModel(componentModelName)));
        }
      }
      foundDependencies.addAll(componentModelNames);

    } while (!foundDependencies.containsAll(componentsToSearchDependencies));
    return foundDependencies;
  }

  private void appendDependency(Set<String> otherDependencies, ComponentModel requestedComponentModel,
                                String parametersReferencingDependency) {
    String name = requestedComponentModel.getParameters().get(parametersReferencingDependency);
    if (applicationModel.findTopLevelNamedElement(name).isPresent()) {
      otherDependencies.add(name);
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(String.format("Ignoring dependency %s because it does not exists", name));
      }
    }
  }

  private boolean isCoreComponent(ComponentIdentifier componentIdentifier, String name) {
    return componentIdentifier.getNamespace().equals(CORE_PREFIX) && componentIdentifier.getName().equals(name);
  }

  private ComponentModel findRequiredComponentModel(String name) {
    return applicationModel.findTopLevelNamedComponent(name)
        .orElseThrow(() -> new NoSuchComponentModelException(createStaticMessage("No named component with name " + name)));
  }

  protected ComponentModel findRequiredComponentModel(Location location) {
    final Reference<ComponentModel> foundComponentModelReference = new Reference<>();
    Optional<ComponentModel> globalComponent = applicationModel.findTopLevelNamedComponent(location.getGlobalName());
    globalComponent.ifPresent(componentModel -> findComponentWithLocation(componentModel, location)
        .ifPresent(foundComponentModel -> foundComponentModelReference.set(foundComponentModel)));
    if (foundComponentModelReference.get() == null) {
      throw new NoSuchComponentModelException(createStaticMessage("No object found at location " + location.toString()));
    }
    return foundComponentModelReference.get();
  }

  private Optional<ComponentModel> findComponentWithLocation(ComponentModel componentModel, Location location) {
    if (componentModel.getComponentLocation().getLocation().equals(location.toString())) {
      return of(componentModel);
    }
    for (ComponentModel childComponent : componentModel.getInnerComponents()) {
      Optional<ComponentModel> foundComponent = findComponentWithLocation(childComponent, location);
      if (foundComponent.isPresent()) {
        return foundComponent;
      }
    }
    return empty();
  }

  /**
   * @param componentName the name attribute value of the component
   * @return the dependencies of the component with component name {@code #componentName}. An empty collection if there is no
   *         component with such name.
   */
  public Collection<String> resolveComponentDependencies(String componentName) {
    try {
      ComponentModel requiredComponentModel = findRequiredComponentModel(componentName);
      return resolveComponentModelDependencies(requiredComponentModel);
    } catch (NoSuchComponentModelException e) {
      return emptyList();
    }
  }

  public ApplicationModel getApplicationModel() {
    return applicationModel;
  }

  @Override
  public Collection<Object> resolveBeanDependencies(Set<String> beanNames) {
    return null;
  }

  public List<ComponentModel> findRequiredComponentModels(LazyComponentInitializer.ComponentLocationFilter filter) {
    List<ComponentModel> components = new ArrayList<>();
    applicationModel.executeOnEveryComponentTree(componentModel -> {
      if (componentModel.getComponentLocation() != null) {
        if (filter.accept(componentModel.getComponentLocation())) {
          components.add(componentModel);
        }
      }
    });
    return components;
  }

  /**
   * @return the set of component names that must always be enabled.
   */
  public Set<String> resolveAlwaysEnabledComponents() {
    ImmutableSet.Builder<String> namesBuilder = ImmutableSet.builder();
    this.applicationModel.executeOnEveryRootElement(componentModel -> {
      Optional<ComponentBuildingDefinition<?>> buildingDefinition =
          this.componentBuildingDefinitionRegistry.getBuildingDefinition(componentModel.getIdentifier());
      buildingDefinition.ifPresent(definition -> {
        if (definition.isAlwaysEnabled()) {
          if (componentModel.getNameAttribute() != null) {
            namesBuilder.add(componentModel.getNameAttribute());
          }
        }
      });
    });
    return namesBuilder.build();
  }

}
