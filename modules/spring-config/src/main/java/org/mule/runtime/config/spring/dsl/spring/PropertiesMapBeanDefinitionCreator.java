/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_PROPERTIES_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SPRING_ENTRY_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SPRING_LIST_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SPRING_MAP_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SPRING_VALUE_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.VALUE_ATTRIBUTE;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.BEAN_REF_ATTRIBUTE;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.KEY_ELEMENT;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.KEY_REF_ATTRIBUTE;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.MAP_ELEMENT;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.VALUE_REF_ATTRIBUTE;

import java.util.HashMap;

import org.mule.runtime.config.spring.dsl.model.ComponentModel;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;

public class PropertiesMapBeanDefinitionCreator extends BeanDefinitionCreator {

  @Override
  boolean handleRequest(CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    ComponentModel componentModel = createBeanDefinitionRequest.getComponentModel();
    if (componentModel.getIdentifier().equals(SPRING_ENTRY_IDENTIFIER)
        || componentModel.getIdentifier().equals(SPRING_LIST_IDENTIFIER)
        || componentModel.getIdentifier().equals(SPRING_MAP_IDENTIFIER)
        || componentModel.getIdentifier().equals(SPRING_VALUE_IDENTIFIER)) {
      return true;
    }
    if (componentModel.getIdentifier().equals(MULE_PROPERTIES_IDENTIFIER)
        || componentModel.getIdentifier().equals(MULE_PROPERTY_IDENTIFIER)) {
      ManagedMap<Object, Object> managedMap;
      if (componentModel.getIdentifier().equals(MULE_PROPERTIES_IDENTIFIER)) {
        managedMap = createManagedMapFromEntries(componentModel);
      } else {
        managedMap = new ManagedMap<>();
        ComponentModel parentComponentModel = componentModel.getParent();
        parentComponentModel.getInnerComponents()
            .stream()
            .filter(childComponentModel -> childComponentModel.getIdentifier().equals(MULE_PROPERTY_IDENTIFIER))
            .forEach(childComponentModel -> {
              processAndAddMapProperty(childComponentModel, managedMap);
            });
      }
      BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(HashMap.class);
      componentModel.setBeanDefinition(beanDefinitionBuilder
          .addConstructorArgValue(managedMap)
          .getBeanDefinition());
      return true;
    }
    return false;
  }

  private ManagedMap<Object, Object> createManagedMapFromEntries(ComponentModel componentModel) {
    ManagedMap<Object, Object> managedMap;
    managedMap = new ManagedMap<>();
    for (ComponentModel innerComponent : componentModel.getInnerComponents()) {
      processAndAddMapProperty(innerComponent, managedMap);
    }
    return managedMap;
  }

  private void processAndAddMapProperty(ComponentModel componentModel, ManagedMap<Object, Object> managedMap) {
    Object key =
        resolveValue(componentModel.getParameters().get(KEY_ELEMENT), componentModel.getParameters().get(KEY_REF_ATTRIBUTE));
    Object value = resolveValue(componentModel.getParameters().get(VALUE_ATTRIBUTE),
                                componentModel.getParameters().get(VALUE_REF_ATTRIBUTE));
    if (value == null) {
      value = resolveValueFromChild(componentModel.getInnerComponents().get(0));
    }
    managedMap.put(key, value);
  }

  private Object resolveValueFromChild(ComponentModel componentModel) {
    if (componentModel.getIdentifier().getName().equals(MAP_ELEMENT)) {
      return createManagedMapFromEntries(componentModel);
    } else {
      return createManagedListFromItems(componentModel);
    }
  }

  private Object createManagedListFromItems(ComponentModel componentModel) {
    ManagedList<Object> managedList = new ManagedList<>();
    componentModel.getInnerComponents().forEach(childComponent -> {
      if (childComponent.getIdentifier().getName().equals(VALUE_ATTRIBUTE)) {
        managedList.add(childComponent.getTextContent());
      } else {
        managedList.add(new RuntimeBeanReference(childComponent.getParameters().get(BEAN_REF_ATTRIBUTE)));
      }
    });
    return managedList;
  }

  private Object resolveValue(String value, String reference) {
    if (reference != null) {
      return new RuntimeBeanReference(reference);
    }
    return value;
  }
}
