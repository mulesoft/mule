/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal.dsl.spring;

import static org.mule.runtime.config.spring.api.dsl.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.spring.api.dsl.model.ApplicationModel.PROPERTIES_ELEMENT;
import static org.mule.runtime.config.spring.internal.dsl.spring.CommonBeanDefinitionCreator.areMatchingTypes;
import static org.mule.runtime.config.spring.internal.dsl.spring.CommonBeanDefinitionCreator.getPropertyValueFromPropertiesComponent;
import static org.mule.runtime.config.spring.internal.dsl.spring.PropertyComponentUtils.getPropertyValueFromPropertyComponent;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

import org.mule.runtime.config.spring.api.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.spring.privileged.dsl.PostProcessorIocHelper;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ObjectFactory;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;

class SpringPostProcessorIocHelper implements PostProcessorIocHelper {

  private ObjectFactoryClassRepository objectFactoryClassRepository;
  private AbstractBeanDefinition beanDefinition;

  public SpringPostProcessorIocHelper(ObjectFactoryClassRepository objectFactoryClassRepository,
                                      AbstractBeanDefinition beanDefinition) {
    this.objectFactoryClassRepository = objectFactoryClassRepository;
    this.beanDefinition = beanDefinition;
  }

  @Override
  public void replaceDefinitionWithRoot(Class beanClass, boolean wrap, Object... constructorArgs) {
    BeanDefinitionBuilder builder = rootBeanDefinition(beanClass);
    if (wrap) {
      builder = builder.addConstructorArgValue(beanDefinition);
    }
    for (Object constructorArg : constructorArgs) {
      builder = builder.addConstructorArgValue(constructorArg);
    }
    beanDefinition = builder.getBeanDefinition();
  }

  @Override
  public void addReferenceConstructorArg(String beanName) {
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(beanName));
  }

  @Override
  public void addPropertyValue(String propertyName, Object propertyValue) {
    beanDefinition.getPropertyValues().addPropertyValue(propertyName, propertyValue);
  }

  @Override
  public void addReferencesPropertyValue(String propertyName, String[] refBeanNames) {
    ManagedList managedList = new ManagedList();
    for (String ref : refBeanNames) {
      managedList.add(new RuntimeBeanReference(ref));
    }
    beanDefinition.getPropertyValues().addPropertyValue(propertyName, managedList);
  }

  @Override
  public void addDefinitionPropertyValue(String propertyName, Class beanClass, Map<String, Object> properties, boolean addContext,
                                         Object... constructorArgs) {
    BeanDefinitionBuilder builder;
    if (ObjectFactory.class.isAssignableFrom(beanClass)) {
      final ComponentBuildingDefinition definition = new ComponentBuildingDefinition.Builder()
          .withTypeDefinition(fromType(Object.class))
          .withObjectFactoryType(beanClass)
          .withNamespace("helper")
          .withIdentifier(propertyName)
          .build();
      builder = genericBeanDefinition(objectFactoryClassRepository.getObjectFactoryClass(definition, beanClass, Object.class,
                                                                                         () -> true, b -> {
                                                                                         }));
    } else {
      builder = genericBeanDefinition(beanClass);
    }


    for (Object constructorArg : constructorArgs) {
      builder = builder.addConstructorArgValue(constructorArg);
    }

    if (addContext) {
      builder = builder.addConstructorArgReference(OBJECT_MULE_CONTEXT);
    }

    for (Entry<String, Object> propertyEntry : properties.entrySet()) {
      builder = builder.addPropertyValue(propertyEntry.getKey(), propertyEntry.getValue());
    }

    beanDefinition.getPropertyValues().addPropertyValue(propertyName, builder.getBeanDefinition());
  }

  @Override
  public void removePropertyValue(String propertyName) {
    beanDefinition.getPropertyValues().removePropertyValue(propertyName);
  }

  @Override
  public Object getPropertyValue(String propertyName) {
    return beanDefinition.getPropertyValues().get(propertyName);
  }

  @Override
  public List toBeanDefinitions(List<ComponentModel> components) {
    ManagedList responseMessageProcessorsBeanList = new ManagedList();
    components.forEach(responseProcessorComponentModel -> {
      BeanDefinition beanDefinition = ((SpringComponentModel) responseProcessorComponentModel).getBeanDefinition();
      responseMessageProcessorsBeanList.add(beanDefinition != null ? beanDefinition
          : ((SpringComponentModel) responseProcessorComponentModel).getBeanReference());
    });

    return responseMessageProcessorsBeanList;
  }

  @Override
  public Map<String, Object> toBeanDefinitionsProperties(List<ComponentModel> components) {
    ManagedMap<String, Object> propertiesMap = new ManagedMap<>();
    components.stream()
        .filter(innerComponent -> innerComponent.getIdentifier().equals(MULE_PROPERTY_IDENTIFIER))
        .forEach(propertyComponentModel -> {
          Pair<String, Object> propertyValue = getPropertyValueFromPropertyComponent(propertyComponentModel);
          removePropertyValue(propertyValue.getFirst());
          propertiesMap.put(propertyValue.getFirst(), propertyValue.getSecond());
        });
    components.stream()
        .filter(innerComponent -> innerComponent.getIdentifier().getName().equals(PROPERTIES_ELEMENT)).findFirst()
        .ifPresent(propertiesComponent -> {
          getPropertyValueFromPropertiesComponent(propertiesComponent).stream().forEach(propertyValue -> {
            propertiesMap.put(propertyValue.getFirst(), propertyValue.getSecond());
          });
        });

    return propertiesMap;
  }

  @Override
  public boolean isDefinitionFor(Object definition, Class beanClass) {
    return definition instanceof AbstractBeanDefinition
        && areMatchingTypes(beanClass, ((AbstractBeanDefinition) definition).getBeanClass());
  }

  public AbstractBeanDefinition getBeanDefinition() {
    return beanDefinition;
  }

}
