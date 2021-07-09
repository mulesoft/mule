/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.ast.api.ComponentAst.BODY_RAW_PARAM_NAME;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;

class PropertiesMapBeanDefinitionCreator extends BeanDefinitionCreator<CreateComponentBeanDefinitionRequest> {

  @Override
  boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels,
                        CreateComponentBeanDefinitionRequest createBeanDefinitionRequest) {
    ComponentAst component = createBeanDefinitionRequest.getComponent();
    if (component != null
        && (component.getIdentifier().equals(MULE_PROPERTIES_IDENTIFIER)
            || component.getIdentifier().equals(MULE_PROPERTY_IDENTIFIER))) {
      ManagedMap<Object, Object> managedMap;
      if (component.getIdentifier().equals(MULE_PROPERTIES_IDENTIFIER)) {
        managedMap = createManagedMapFromEntries(component);
      } else {
        managedMap = new ManagedMap<>();
        final List<ComponentAst> hierarchy = createBeanDefinitionRequest.getComponentHierarchy();
        ComponentAst parentComponentModel = hierarchy.isEmpty() ? null : hierarchy.get(hierarchy.size() - 1);
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

  private ManagedMap<Object, Object> createManagedMapFromEntries(ComponentAst component) {
    ManagedMap<Object, Object> managedMap;
    managedMap = new ManagedMap<>();
    component.directChildrenStream()
        .forEach(innerComponent -> processAndAddMapProperty(innerComponent, managedMap));
    return managedMap;
  }

  private void processAndAddMapProperty(ComponentAst component, ManagedMap<Object, Object> managedMap) {
    Object key =
        resolveValue(component.getRawParameterValue(KEY_ELEMENT).orElse(null),
                     component.getRawParameterValue(KEY_REF_ATTRIBUTE).orElse(null));
    Object value = resolveValue(component.getRawParameterValue(VALUE_ATTRIBUTE).orElse(null),
                                component.getRawParameterValue(VALUE_REF_ATTRIBUTE).orElse(null));
    if (value == null) {
      value = component.directChildrenStream().findFirst()
          .map(this::resolveValueFromChild)
          .orElse(null);
    }
    managedMap.put(key, value);
  }

  private Object resolveValueFromChild(ComponentAst component) {
    if (component.getIdentifier().getName().equals(MAP_ELEMENT)) {
      return createManagedMapFromEntries(component);
    } else {
      return createManagedListFromItems(component);
    }
  }

  private Object createManagedListFromItems(ComponentAst component) {
    ManagedList<Object> managedList = new ManagedList<>();
    component.directChildrenStream().forEach(childComponent -> {
      if (childComponent.getIdentifier().getName().equals(VALUE_ATTRIBUTE)) {
        managedList.add(childComponent.getRawParameterValue(BODY_RAW_PARAM_NAME).orElse(null));
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
