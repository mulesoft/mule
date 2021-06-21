/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.config.internal.dsl.spring.CommonBeanDefinitionCreator.processMuleProperties;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.core.privileged.component.AnnotatedObjectInvocationHandler.addAnnotationsToClass;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.model.config.RuntimeConfigurationException;

import java.util.Map;
import java.util.function.Consumer;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * {@code BeanDefinitionCreator} that handles <object> elements
 * <p>
 * The value provided by the element will be the same object referenced.
 *
 * @since 4.0
 */
class ObjectBeanDefinitionCreator extends BeanDefinitionCreator {

  private static final String REF_PARAMETER = "ref";
  private static final String CLASS_PARAMETER = "class";

  @Override
  boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels,
                        CreateBeanDefinitionRequest createBeanDefinitionRequest,
                        Consumer<ComponentAst> nestedComponentParamProcessor,
                        Consumer<SpringComponentModel> componentBeanDefinitionHandler) {
    ComponentAst componentModel = createBeanDefinitionRequest.getComponentModel();
    if (componentModel == null || !componentModel.getIdentifier().equals(buildFromStringRepresentation("mule:object"))) {
      return false;
    }
    String refParameterValue = componentModel.getParameter(REF_PARAMETER).getResolvedRawValue();
    String classParameterValue = componentModel.getParameter(CLASS_PARAMETER).getResolvedRawValue();
    if (refParameterValue != null && classParameterValue != null) {
      throw new RuntimeConfigurationException(createStaticMessage(format("Object cannot contain both '%s' and '%s' parameters. Offending resource is '%s'",
                                                                         REF_PARAMETER, CLASS_PARAMETER,
                                                                         componentModel.getLocation())));
    }
    if (refParameterValue == null && classParameterValue == null) {
      throw new RuntimeConfigurationException(createStaticMessage(format("Object must contain '%s' or '%s' parameter. Offending resource is '%s'",
                                                                         REF_PARAMETER, CLASS_PARAMETER,
                                                                         componentModel.getLocation())));
    }

    if (refParameterValue != null) {
      createBeanDefinitionRequest.getSpringComponentModel().setBeanReference(new RuntimeBeanReference(refParameterValue));
    }
    if (classParameterValue != null) {
      BeanDefinitionBuilder beanDefinitionBuilder;
      final Class<?> classParameter;
      try {
        classParameter = loadClass(classParameterValue, currentThread().getContextClassLoader());
      } catch (ClassNotFoundException e) {
        throw new RuntimeConfigurationException(createStaticMessage(e.getMessage()), e);
      }

      beanDefinitionBuilder = rootBeanDefinition(addAnnotationsToClass(classParameter));
      processMuleProperties(componentModel, beanDefinitionBuilder, null);
      createBeanDefinitionRequest.getSpringComponentModel().setBeanDefinition(beanDefinitionBuilder.getBeanDefinition());
    }

    componentBeanDefinitionHandler.accept(createBeanDefinitionRequest.getSpringComponentModel());

    return true;
  }
}
