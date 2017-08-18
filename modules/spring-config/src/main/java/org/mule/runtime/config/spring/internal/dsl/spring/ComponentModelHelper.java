/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal.dsl.spring;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.config.spring.api.dsl.model.ApplicationModel.FLOW_IDENTIFIER;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.config.spring.api.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.Router;
import org.mule.runtime.core.api.processor.Scope;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.exception.ErrorHandler;
import org.mule.runtime.core.internal.exception.TemplateOnErrorHandler;
import org.mule.runtime.core.internal.routing.AbstractSelectiveRouter;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;

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
   * @return the componentModel type.
   */
  public static TypedComponentIdentifier.ComponentType resolveComponentType(ComponentModel componentModel) {
    if (isMessageSource(componentModel)) {
      return TypedComponentIdentifier.ComponentType.SOURCE;
    } else if (isErrorHandler(componentModel)) {
      return TypedComponentIdentifier.ComponentType.ERROR_HANDLER;
    } else if (isTemplateOnErrorHandler(componentModel)) {
      return TypedComponentIdentifier.ComponentType.ON_ERROR;
    } else if (isFlow(componentModel)) {
      return TypedComponentIdentifier.ComponentType.FLOW;
    } else if (isProcessor(componentModel)) {
      if (isOperationMessageProcessor(componentModel)) {
        return OPERATION;
      } else if (isInterceptingProcessor(componentModel)) {
        return TypedComponentIdentifier.ComponentType.INTERCEPTING;
      } else if (isRouterProcessor(componentModel)) {
        return TypedComponentIdentifier.ComponentType.ROUTER;
      } else if (isScopeProcessor(componentModel)) {
        return TypedComponentIdentifier.ComponentType.SCOPE;
      } else {
        return TypedComponentIdentifier.ComponentType.PROCESSOR;
      }
    }
    return TypedComponentIdentifier.ComponentType.UNKNOWN;
  }

  private static boolean isOperationMessageProcessor(ComponentModel componentModel) {
    return isOfType(componentModel, OperationMessageProcessor.class);
  }

  public static boolean isAnnotatedObject(ComponentModel componentModel) {
    return isOfType(componentModel, AnnotatedObject.class);
  }

  private static boolean isScopeProcessor(ComponentModel componentModel) {
    return isOfType(componentModel, Scope.class);
  }

  private static boolean isRouterProcessor(ComponentModel componentModel) {
    return isOfType(componentModel, Router.class);
  }

  private static boolean isInterceptingProcessor(ComponentModel componentModel) {
    return isOfType(componentModel, InterceptingMessageProcessor.class);
  }

  public static boolean isProcessor(ComponentModel componentModel) {
    return isOfType(componentModel, Processor.class) || isOfType(componentModel, InterceptingMessageProcessor.class);
  }

  public static boolean isMessageSource(ComponentModel componentModel) {
    return isOfType(componentModel, MessageSource.class);
  }

  public static boolean isErrorHandler(ComponentModel componentModel) {
    return isOfType(componentModel, ErrorHandler.class);
  }

  public static boolean isTemplateOnErrorHandler(ComponentModel componentModel) {
    return isOfType(componentModel, TemplateOnErrorHandler.class);
  }

  public static boolean isFlow(ComponentModel componentModel) {
    return componentModel.getIdentifier().equals(FLOW_IDENTIFIER);
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
        beanDefinition.getPropertyValues().getPropertyValue(AnnotatedObject.PROPERTY_NAME);
    Map<QName, Object> annotations;
    if (propertyValue == null) {
      annotations = new HashMap<>();
      propertyValue = new PropertyValue(AnnotatedObject.PROPERTY_NAME, annotations);
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
        componentModel.getBeanDefinition().getPropertyValues().getPropertyValue(AnnotatedObject.PROPERTY_NAME);
    Map<QName, Object> annotations;
    if (propertyValue == null) {
      return empty();
    } else {
      annotations = (Map<QName, Object>) propertyValue.getValue();
      return ofNullable((T) annotations.get(annotationKey));
    }
  }

  public static boolean isRouter(ComponentModel componentModel) {
    return isOfType(componentModel, Router.class) || isOfType(componentModel, AbstractSelectiveRouter.class);
  }
}
