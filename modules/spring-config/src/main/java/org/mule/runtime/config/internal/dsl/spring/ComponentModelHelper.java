/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.component.Component.ANNOTATIONS_PROPERTY_NAME;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ERROR_HANDLER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ON_ERROR;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ON_ERROR_CONTINE_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ON_ERROR_PROPAGATE_IDENTIFIER;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.config.internal.dsl.model.ComponentLocationVisitor;
import org.mule.runtime.config.internal.dsl.model.ExtensionModelHelper;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.exception.ErrorHandler;
import org.mule.runtime.core.internal.exception.TemplateOnErrorHandler;
import org.mule.runtime.core.internal.routing.AbstractSelectiveRouter;
import org.mule.runtime.core.privileged.processor.Router;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;

public class ComponentModelHelper {

  /**
   * Resolves the {@link org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType} from a {@link ComponentModel}.
   *
   * @param componentModel a {@link ComponentModel} that represents a component in the configuration.
   * @param extensionModelHelper helper to access components in extension model
   * @return the componentModel type.
   */
  public static TypedComponentIdentifier.ComponentType resolveComponentType(ComponentModel componentModel,
                                                                            ExtensionModelHelper extensionModelHelper) {
    if (componentModel.getIdentifier().equals(ON_ERROR_CONTINE_IDENTIFIER)
        || componentModel.getIdentifier().equals(ON_ERROR_PROPAGATE_IDENTIFIER)) {
      return ON_ERROR;
    }
    return extensionModelHelper.findComponentType(componentModel);
  }

  public static boolean isAnnotatedObject(ComponentModel componentModel) {
    return isOfType(componentModel, Component.class);
  }

  public static boolean isProcessor(ComponentModel componentModel) {
    return isOfType(componentModel, Processor.class)
        || isOfType(componentModel, InterceptingMessageProcessor.class)
        || componentModel.getComponentType().map(type -> type.equals(OPERATION)).orElse(false)
        || componentModel.getComponentType().map(type -> type.equals(ROUTER)).orElse(false)
        || componentModel.getComponentType().map(type -> type.equals(SCOPE)).orElse(false);
  }

  public static boolean isMessageSource(ComponentModel componentModel) {
    return isOfType(componentModel, MessageSource.class)
        || componentModel.getComponentType().map(type -> type.equals(SOURCE)).orElse(false);
  }

  public static boolean isErrorHandler(ComponentModel componentModel) {
    return isOfType(componentModel, ErrorHandler.class)
        || componentModel.getComponentType().map(type -> type.equals(ERROR_HANDLER)).orElse(false);
  }

  public static boolean isTemplateOnErrorHandler(ComponentModel componentModel) {
    return isOfType(componentModel, TemplateOnErrorHandler.class)
        || componentModel.getComponentType().map(type -> type.equals(ON_ERROR)).orElse(false);
  }

  private static boolean isOfType(ComponentModel componentModel, Class type) {
    Class<?> componentModelType = componentModel.getType();
    if (componentModelType == null) {
      return false;
    }
    return CommonBeanDefinitionCreator.areMatchingTypes(type, componentModelType);
  }

  public static void addAnnotation(QName annotationKey, Object annotationValue, SpringComponentModel componentModel) {
    // TODO MULE-10970 - remove condition once everything is AnnotatedObject.
    if (!ComponentModelHelper.isAnnotatedObject(componentModel) && !componentModel.getIdentifier().getName().equals("flow-ref")) {
      return;
    }
    BeanDefinition beanDefinition = componentModel.getBeanDefinition();
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

  public static <T> Optional<T> getAnnotation(QName annotationKey, SpringComponentModel componentModel) {
    if (componentModel.getBeanDefinition() == null) {
      return empty();
    }
    PropertyValue propertyValue =
        componentModel.getBeanDefinition().getPropertyValues().getPropertyValue(ANNOTATIONS_PROPERTY_NAME);
    Map<QName, Object> annotations;
    if (propertyValue == null) {
      return empty();
    } else {
      annotations = (Map<QName, Object>) propertyValue.getValue();
      return ofNullable((T) annotations.get(annotationKey));
    }
  }

  public static boolean isRouter(ComponentModel componentModel) {
    return isOfType(componentModel, Router.class) || isOfType(componentModel, AbstractSelectiveRouter.class)
        || ComponentLocationVisitor.BATCH_JOB_COMPONENT_IDENTIFIER.equals(componentModel.getIdentifier())
        || ComponentLocationVisitor.BATCH_PROCESSS_RECORDS_COMPONENT_IDENTIFIER.equals(componentModel.getIdentifier())
        || componentModel.getComponentType().map(type -> type.equals(ROUTER)).orElse(false);
  }
}
