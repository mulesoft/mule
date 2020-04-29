/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTIES_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.VALUE_ATTRIBUTE;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.BEAN_REF_ATTRIBUTE;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.KEY_ELEMENT;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.KEY_REF_ATTRIBUTE;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.MAP_ELEMENT;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.VALUE_REF_ATTRIBUTE;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.model.ComponentModel;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;

class PropertiesMapBeanDefinitionCreator extends BeanDefinitionCreator {

  @Override
  boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels,
                        CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    ComponentAst componentModel = createBeanDefinitionRequest.getComponentModel();
    if (componentModel.getIdentifier().equals(MULE_PROPERTIES_IDENTIFIER)
        || componentModel.getIdentifier().equals(MULE_PROPERTY_IDENTIFIER)) {
      ManagedMap<Object, Object> managedMap;
      if (componentModel.getIdentifier().equals(MULE_PROPERTIES_IDENTIFIER)) {
        managedMap = createManagedMapFromEntries(componentModel);
      } else {
        managedMap = new ManagedMap<>();
        ComponentAst parentComponentModel = createBeanDefinitionRequest.getParentComponentModel();
        parentComponentModel.directChildrenStream()
            .filter(childComponentModel -> childComponentModel.getIdentifier().equals(MULE_PROPERTY_IDENTIFIER))
            .forEach(childComponentModel -> processAndAddMapProperty(childComponentModel, managedMap));
      }
      BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(HashMap.class);
      createBeanDefinitionRequest.getSpringComponentModel().setBeanDefinition(beanDefinitionBuilder
          .addConstructorArgValue(managedMap)
          .getBeanDefinition());
      return true;
    }
    return false;
  }

  private ManagedMap<Object, Object> createManagedMapFromEntries(ComponentAst componentModel) {
    ManagedMap<Object, Object> managedMap;
    managedMap = new ManagedMap<>();
    componentModel.directChildrenStream()
        .forEach(innerComponent -> processAndAddMapProperty(innerComponent, managedMap));
    return managedMap;
  }

  private void processAndAddMapProperty(ComponentAst componentModel, ManagedMap<Object, Object> managedMap) {
    Object key =
        resolveValue(componentModel.getRawParameterValue(KEY_ELEMENT).orElse(null),
                     componentModel.getRawParameterValue(KEY_REF_ATTRIBUTE).orElse(null));
    Object value = resolveValue(componentModel.getRawParameterValue(VALUE_ATTRIBUTE).orElse(null),
                                componentModel.getRawParameterValue(VALUE_REF_ATTRIBUTE).orElse(null));
    if (value == null) {
      value = resolveValueFromChild(componentModel.directChildrenStream().findFirst().get());
    }
    managedMap.put(key, value);
  }

  private Object resolveValueFromChild(ComponentAst componentModel) {
    if (componentModel.getIdentifier().getName().equals(MAP_ELEMENT)) {
      return createManagedMapFromEntries(componentModel);
    } else {
      return createManagedListFromItems(componentModel);
    }
  }

  private Object createManagedListFromItems(ComponentAst componentModel) {
    ManagedList<Object> managedList = new ManagedList<>();
    componentModel.directChildrenStream().forEach(childComponent -> {
      if (childComponent.getIdentifier().getName().equals(VALUE_ATTRIBUTE)) {
        managedList.add(((ComponentModel) childComponent).getTextContent());
      } else {
        managedList.add(new RuntimeBeanReference(childComponent.getRawParameterValue(BEAN_REF_ATTRIBUTE).orElse(null)));
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
