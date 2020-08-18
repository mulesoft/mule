/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.api.component.Component.ANNOTATIONS_PROPERTY_NAME;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;

public class ComponentModelHelper {

  public static boolean isAnnotatedObject(SpringComponentModel springComponentModel) {
    return isOfType(springComponentModel, Component.class)
        // ValueResolver end up generating pojos from the extension whose class is enhanced to have annotations
        || isOfType(springComponentModel, ValueResolver.class);
  }

  private static boolean isOfType(SpringComponentModel springComponentModel, Class type) {
    Class<?> componentModelType = springComponentModel.getType();
    if (componentModelType == null) {
      return false;
    }
    return CommonBeanDefinitionCreator.areMatchingTypes(type, componentModelType);
  }

  public static void addAnnotation(QName annotationKey, Object annotationValue, SpringComponentModel springComponentModel) {
    // TODO MULE-10970 - remove condition once everything is AnnotatedObject.
    if (!ComponentModelHelper.isAnnotatedObject(springComponentModel)
        && !springComponentModel.getComponent().getIdentifier().getName().equals("flow-ref")) {
      return;
    }
    BeanDefinition beanDefinition = springComponentModel.getBeanDefinition();
    if (beanDefinition == null) {
      // This is the case of components that are references
      return;
    }
    updateAnnotationValue(annotationKey, annotationValue, beanDefinition);
  }

  public static void updateAnnotationValue(QName annotationKey, Object annotationValue, BeanDefinition beanDefinition) {
    PropertyValue propertyValue =
        beanDefinition.getPropertyValues().getPropertyValue(ANNOTATIONS_PROPERTY_NAME);
    Map<QName, Object> annotations;
    if (propertyValue == null) {
      annotations = new HashMap<>();
      propertyValue = new PropertyValue(ANNOTATIONS_PROPERTY_NAME, annotations);
      beanDefinition.getPropertyValues().addPropertyValue(propertyValue);
    } else {
      annotations = (Map<QName, Object>) propertyValue.getValue();
    }
    annotations.put(annotationKey, annotationValue);
  }

}
