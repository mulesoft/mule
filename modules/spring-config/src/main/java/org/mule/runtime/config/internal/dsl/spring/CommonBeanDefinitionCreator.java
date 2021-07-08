/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory.SPRING_PROTOTYPE_OBJECT;
import static org.mule.runtime.config.internal.dsl.spring.PropertyComponentUtils.getPropertyValueFromPropertyComponent;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTIES_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;
import static org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoader.resolveContextArtifactPluginClassLoaders;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.privileged.dsl.BeanDefinitionPostProcessor;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import java.util.Map;
import java.util.ServiceConfigurationError;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * Processor in the chain of responsibility that knows how to handle a generic {@code ComponentBuildingDefinition}.
 *
 * @since 4.0
 */
class CommonBeanDefinitionCreator extends BaseCommonBeanDefinitionCreator<CreateComponentBeanDefinitionRequest> {

  private final BeanDefinitionPostProcessor beanDefinitionPostProcessor;

  public CommonBeanDefinitionCreator(ObjectFactoryClassRepository objectFactoryClassRepository) {
    super(objectFactoryClassRepository);

    this.beanDefinitionPostProcessor = resolvePostProcessor();
  }

  private BeanDefinitionPostProcessor resolvePostProcessor() {
    for (ClassLoader classLoader : resolveContextArtifactPluginClassLoaders()) {
      try {
        final BeanDefinitionPostProcessor foundProvider =
            new SpiServiceRegistry().lookupProvider(BeanDefinitionPostProcessor.class, classLoader);
        if (foundProvider != null) {
          return foundProvider;
        }
      } catch (Exception | ServiceConfigurationError e) {
        // Nothing to do, we just don't have compatibility plugin in the app
      }
    }
    return (componentModel, helper) -> {
    };
  }

  @Override
  protected void processComponentDefinitionModel(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                                 final CreateComponentBeanDefinitionRequest request,
                                                 ComponentBuildingDefinition componentBuildingDefinition,
                                                 final BeanDefinitionBuilder beanDefinitionBuilder) {
    final ComponentAst componentModel = request.getComponentModel();

    processObjectConstructionParameters(springComponentModels, componentModel, componentModel, request,
                                        componentBuildingDefinition,
                                        new BeanDefinitionBuilderHelper(beanDefinitionBuilder));
    processMuleProperties(componentModel, beanDefinitionBuilder, beanDefinitionPostProcessor);
    if (componentBuildingDefinition.isPrototype()) {
      beanDefinitionBuilder.setScope(SPRING_PROTOTYPE_OBJECT);
    }

    request.getSpringComponentModel().setBeanDefinition(beanDefinitionBuilder.getBeanDefinition());
  }

  static void processMuleProperties(ComponentAst componentModel, BeanDefinitionBuilder beanDefinitionBuilder,
                                    BeanDefinitionPostProcessor beanDefinitionPostProcessor) {
    if (componentModel == null) {
      return;
    }

    // for now we skip custom-transformer since requires injection by the object factory.
    if (beanDefinitionPostProcessor != null && beanDefinitionPostProcessor.getGenericPropertiesCustomProcessingIdentifiers()
        .contains(componentModel.getIdentifier())) {
      return;
    }
    componentModel.directChildrenStream()
        .filter(innerComponent -> {
          ComponentIdentifier identifier = innerComponent.getIdentifier();
          return identifier.equals(MULE_PROPERTY_IDENTIFIER)
              || identifier.equals(MULE_PROPERTIES_IDENTIFIER);
        })
        .forEach(propertyComponentModel -> {
          Pair<String, Object> propertyValue = getPropertyValueFromPropertyComponent(propertyComponentModel);
          beanDefinitionBuilder.addPropertyValue(propertyValue.getFirst(), propertyValue.getSecond());
        });
  }

  public static boolean areMatchingTypes(Class<?> superType, Class<?> childType) {
    return superType.isAssignableFrom(childType);
  }

}
