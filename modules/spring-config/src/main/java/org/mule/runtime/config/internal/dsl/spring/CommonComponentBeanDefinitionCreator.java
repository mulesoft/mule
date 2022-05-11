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
import static org.mule.runtime.module.artifact.activation.internal.classloader.MuleApplicationClassLoader.resolveContextArtifactPluginClassLoaders;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.privileged.dsl.BeanDefinitionPostProcessor;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.privileged.processor.SecurityFilterMessageProcessor;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.stream.Stream;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Processor in the chain of responsibility that knows how to handle a generic {@code ComponentBuildingDefinition}.
 *
 * @since 4.0
 */
class CommonComponentBeanDefinitionCreator extends CommonBeanBaseDefinitionCreator<CreateComponentBeanDefinitionRequest> {

  private final BeanDefinitionPostProcessor beanDefinitionPostProcessor;

  public CommonComponentBeanDefinitionCreator(ObjectFactoryClassRepository objectFactoryClassRepository,
                                              boolean disableTrimWhitespaces, boolean enableByteBuddy) {
    super(objectFactoryClassRepository, disableTrimWhitespaces, enableByteBuddy);

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
                                                 CreateComponentBeanDefinitionRequest request,
                                                 ComponentBuildingDefinition componentBuildingDefinition,
                                                 BeanDefinitionBuilder beanDefinitionBuilder) {
    final ComponentAst component = request.getComponent();
    processObjectConstructionParameters(springComponentModels, component, component, request,
                                        new BeanDefinitionBuilderHelper(beanDefinitionBuilder));
    processMuleProperties(component, beanDefinitionBuilder, beanDefinitionPostProcessor);
    if (componentBuildingDefinition.isPrototype()) {
      beanDefinitionBuilder.setScope(SPRING_PROTOTYPE_OBJECT);
    }
    AbstractBeanDefinition originalBeanDefinition = beanDefinitionBuilder.getBeanDefinition();
    AbstractBeanDefinition wrappedBeanDefinition = adaptBeanDefinitionForSecurityFilter(originalBeanDefinition);
    if (originalBeanDefinition != wrappedBeanDefinition) {
      request.getSpringComponentModel().setType(wrappedBeanDefinition.getBeanClass());
    }
    request.getSpringComponentModel().setBeanDefinition(wrappedBeanDefinition);
  }

  private static void processMuleProperties(ComponentAst component, BeanDefinitionBuilder beanDefinitionBuilder,
                                            BeanDefinitionPostProcessor beanDefinitionPostProcessor) {
    // TODO (MULE-19608) remove this method, by having a component building definition that
    // allows to have the properties being set as any other component
    if (component == null) {
      return;
    }

    // for now we skip custom-transformer since requires injection by the object factory.
    if (beanDefinitionPostProcessor != null && beanDefinitionPostProcessor.getGenericPropertiesCustomProcessingIdentifiers()
        .contains(component.getIdentifier())) {
      return;
    }
    doProcessMuleProperties(beanDefinitionBuilder, component.directChildrenStream()
        .filter(innerComponent -> {
          ComponentIdentifier identifier = innerComponent.getIdentifier();
          return identifier.equals(MULE_PROPERTY_IDENTIFIER)
              || identifier.equals(MULE_PROPERTIES_IDENTIFIER);
        }));
  }

  static void doProcessMuleProperties(BeanDefinitionBuilder beanDefinitionBuilder, Stream<ComponentAst> properties) {
    properties
        .forEach(propertyComponentModel -> {
          Pair<String, Object> propertyValue = getPropertyValueFromPropertyComponent(propertyComponentModel);
          beanDefinitionBuilder.addPropertyValue(propertyValue.getFirst(), propertyValue.getSecond());
        });
  }

  private AbstractBeanDefinition adaptBeanDefinitionForSecurityFilter(AbstractBeanDefinition originalBeanDefinition) {
    if (areMatchingTypes(SecurityFilter.class, resolveBeanClass(originalBeanDefinition))) {
      return rootBeanDefinition(SecurityFilterMessageProcessor.class)
          .addConstructorArgValue(originalBeanDefinition)
          .getBeanDefinition();
    } else {
      return originalBeanDefinition;
    }
  }

  private Class resolveBeanClass(AbstractBeanDefinition originalBeanDefinition) {
    Class beanClass;
    if (originalBeanDefinition instanceof RootBeanDefinition) {
      beanClass = ((RootBeanDefinition) originalBeanDefinition).getBeanClass();
    } else {
      try {
        beanClass = originalBeanDefinition.getBeanClass();
      } catch (IllegalStateException e) {
        try {
          beanClass = org.apache.commons.lang3.ClassUtils.getClass(originalBeanDefinition.getBeanClassName());
        } catch (ClassNotFoundException e2) {
          throw new RuntimeException(e2);
        }
      }
    }
    return beanClass;
  }

  public static boolean areMatchingTypes(Class<?> superType, Class<?> childType) {
    return superType.isAssignableFrom(childType);
  }

}
