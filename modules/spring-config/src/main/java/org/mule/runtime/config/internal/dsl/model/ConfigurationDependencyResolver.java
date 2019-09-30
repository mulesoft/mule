/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.config.internal.dsl.model.DependencyNode.Type.INNER;
import static org.mule.runtime.config.internal.dsl.model.DependencyNode.Type.TOP_LEVEL;
import static org.mule.runtime.config.internal.dsl.model.DependencyNode.Type.UNNAMED_TOP_LEVEL;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isExpression;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.api.dsl.processor.AbstractAttributeDefinitionVisitor;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair;
import org.mule.runtime.dsl.api.component.TypeConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ConfigurationDependencyResolver {

  private final ApplicationModel applicationModel;
  private final ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry;
  private final List<DependencyNode> missingElementNames = new ArrayList<>();
  private final Set<DependencyNode> alwaysEnabledComponents = newHashSet();

  /**
   * Creates a new instance associated to a complete {@link ApplicationModel}.
   *
   * @param applicationModel the artifact {@link ApplicationModel}.
   * @param componentBuildingDefinitionRegistry the registry to find the
   *        {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition}s associated to each {@link ComponentModel} that
   *        must be resolved.
   */
  public ConfigurationDependencyResolver(ArtifactAst applicationModel,
                                         ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry) {
    this.applicationModel = (ApplicationModel) applicationModel;
    this.componentBuildingDefinitionRegistry = componentBuildingDefinitionRegistry;
    fillAlwaysEnabledComponents();
  }

  private Set<DependencyNode> resolveComponentModelDependencies(ComponentModel componentModel) {
    final Set<DependencyNode> otherRequiredGlobalComponents = resolveComponentDependencies(componentModel);
    return findComponentModelsDependencies(otherRequiredGlobalComponents);
  }

  protected Set<DependencyNode> resolveComponentDependencies(ComponentModel requestedComponentModel) {
    Set<DependencyNode> otherDependencies = new HashSet<>();
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

                @Override
                public void onSoftReferenceSimpleParameter(String softReference) {
                  parametersReferencingDependencies.add(softReference);
                }

                @Override
                public void onReferenceConfigurationParameter(String parameterName, Object defaultValue,
                                                              Optional<TypeConverter> typeConverter) {
                  if (requestedComponentModel.getParameters().containsKey(parameterName)
                      && !isExpression(requestedComponentModel.getParameters().get(parameterName))) {
                    parametersReferencingDependencies.add(parameterName);
                  }
                }

              });
            }));

    for (String parametersReferencingDependency : parametersReferencingDependencies) {
      if (requestedComponentModel.getParameters().containsKey(parametersReferencingDependency)) {
        appendTopLevelDependency(otherDependencies, requestedComponentModel, parametersReferencingDependency);
      }
    }

    // Special cases for flow-ref and configuration
    if (isCoreComponent(requestedComponentModel.getIdentifier(), "flow-ref")) {
      appendTopLevelDependency(otherDependencies, requestedComponentModel, "name");
    } else if (isAggregatorComponent(requestedComponentModel, "aggregatorName")) {
      // TODO (MULE-14429): use extensionModel to get the dependencies instead of ComponentBuildingDefinition to solve cases like this (flow-ref)
      String name = requestedComponentModel.getParameters().get("aggregatorName");
      DependencyNode dependency = new DependencyNode(name, INNER);
      if (applicationModel.findNamedElement(name).isPresent()) {
        otherDependencies.add(dependency);
      } else {
        missingElementNames.add(dependency);
      }
    } else if (isCoreComponent(requestedComponentModel.getIdentifier(), "configuration")) {
      appendTopLevelDependency(otherDependencies, requestedComponentModel, "defaultErrorHandler-ref");
    }

    return otherDependencies;
  }

  protected Set<DependencyNode> findComponentModelsDependencies(Set<DependencyNode> componentModelNames) {
    Set<DependencyNode> componentsToSearchDependencies = new HashSet<>(componentModelNames);
    Set<DependencyNode> foundDependencies = new LinkedHashSet<>();
    Set<DependencyNode> alreadySearchedDependencies = new HashSet<>();
    do {
      componentsToSearchDependencies.addAll(foundDependencies);
      for (DependencyNode dependencyNode : componentsToSearchDependencies) {
        if (!alreadySearchedDependencies.contains(dependencyNode)) {
          alreadySearchedDependencies.add(dependencyNode);
          foundDependencies.addAll(resolveComponentDependencies(findRequiredComponentModel(dependencyNode.getComponentName())));
        }
      }
      foundDependencies.addAll(componentModelNames);

    } while (!foundDependencies.containsAll(componentsToSearchDependencies));
    return foundDependencies;
  }

  private void appendTopLevelDependency(Set<DependencyNode> otherDependencies, ComponentModel requestedComponentModel,
                                        String parametersReferencingDependency) {
    DependencyNode dependency =
        new DependencyNode(requestedComponentModel.getParameters().get(parametersReferencingDependency), TOP_LEVEL);
    if (applicationModel.findTopLevelNamedComponent(dependency.getComponentName()).isPresent()) {
      otherDependencies.add(dependency);
    } else {
      missingElementNames.add(dependency);
    }
  }

  private boolean isCoreComponent(ComponentIdentifier componentIdentifier, String name) {
    return componentIdentifier.getNamespace().equals(CORE_PREFIX) && componentIdentifier.getName().equals(name);
  }

  private boolean isAggregatorComponent(ComponentModel componentModel, String referenceNameParameter) {
    return componentModel.getIdentifier().getNamespace().equals("aggregators")
        && componentModel.getParameters().containsKey(referenceNameParameter);
  }

  private ComponentModel findRequiredComponentModel(String name) {
    return applicationModel.findNamedElement(name)
        .orElseThrow(() -> new NoSuchComponentModelException(createStaticMessage("No named component with name " + name)));
  }

  /**
   * @param componentName the name attribute value of the component
   * @return the dependencies of the component with component name {@code #componentName}. An empty collection if there is no
   *         component with such name.
   */
  //TODO (MULE-14453: When creating ApplicationModel and ComponentModels inner beans should have a name so they can be later retrieved)
  public Collection<String> resolveComponentDependencies(String componentName) {
    try {
      ComponentModel requiredComponentModel = findRequiredComponentModel(componentName);
      return resolveComponentModelDependencies(requiredComponentModel)
          .stream()
          .filter(dependencyNode -> dependencyNode.isTopLevel())
          .map(dependencyNode -> dependencyNode.getComponentName())
          .collect(toList());
    } catch (NoSuchComponentModelException e) {
      return emptyList();
    }
  }

  /**
   * @return the set of component names that must always be enabled.
   */
  public Set<DependencyNode> resolveAlwaysEnabledComponents() {
    return alwaysEnabledComponents;
  }

  private void fillAlwaysEnabledComponents() {
    this.applicationModel.executeOnEveryRootElement(componentModel -> {
      Optional<ComponentBuildingDefinition<?>> buildingDefinition =
          this.componentBuildingDefinitionRegistry.getBuildingDefinition(componentModel.getIdentifier());
      buildingDefinition.ifPresent(definition -> {
        if (definition.isAlwaysEnabled()) {
          if (componentModel.getNameAttribute() != null) {
            alwaysEnabledComponents
                .add(new DependencyNode(componentModel.getNameAttribute(), componentModel.getIdentifier(), TOP_LEVEL));
          } else if (componentModel.isRoot()) {
            alwaysEnabledComponents.add(new DependencyNode(null, componentModel.getIdentifier(), UNNAMED_TOP_LEVEL));
          }
        }
      });
    });
  }

}
