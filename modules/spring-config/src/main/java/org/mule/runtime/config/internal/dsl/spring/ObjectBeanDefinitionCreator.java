/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.config.internal.dsl.spring.CommonComponentBeanDefinitionCreator.doProcessMuleProperties;
import static org.mule.runtime.config.internal.model.ApplicationModel.OBJECT_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.PROPERTY_ELEMENT;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.core.privileged.component.AnnotatedObjectInvocationHandler.addAnnotationsToClass;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.model.config.RuntimeConfigurationException;
import org.mule.runtime.config.privileged.dsl.BeanDefinitionPostProcessor;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * {@code BeanDefinitionCreator} that handles <object> elements
 * <p>
 * The value provided by the element will be the same object referenced.
 *
 * @since 4.0
 */
class ObjectBeanDefinitionCreator extends BeanDefinitionCreator<CreateComponentBeanDefinitionRequest> {

  static final String REF_PARAMETER = "ref";
  static final String CLASS_PARAMETER = "class";

  @Override
  boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels,
                        CreateComponentBeanDefinitionRequest createBeanDefinitionRequest) {
    ComponentAst component = createBeanDefinitionRequest.getComponent();
    if (component == null || !component.getIdentifier().equals(OBJECT_IDENTIFIER)) {
      return false;
    }

    ComponentParameterAst refParameterAst = component.getParameter(DEFAULT_GROUP_NAME, REF_PARAMETER);
    ComponentParameterAst classParameterAst = component.getParameter(DEFAULT_GROUP_NAME, CLASS_PARAMETER);
    String refParameterValue = refParameterAst != null ? refParameterAst.getResolvedRawValue() : null;
    String classParameterValue = classParameterAst != null ? classParameterAst.getResolvedRawValue() : null;

    if (refParameterValue != null && classParameterValue != null) {
      throw new RuntimeConfigurationException(createStaticMessage(format("Object cannot contain both '%s' and '%s' parameters. Offending resource is '%s'",
                                                                         REF_PARAMETER, CLASS_PARAMETER,
                                                                         component.getLocation())));
    }
    if (refParameterValue == null && classParameterValue == null) {
      throw new RuntimeConfigurationException(createStaticMessage(format("Object must contain '%s' or '%s' parameter. Offending resource is '%s'",
                                                                         REF_PARAMETER, CLASS_PARAMETER,
                                                                         component.getLocation())));
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
      processMuleProperties(component, beanDefinitionBuilder, null);
      createBeanDefinitionRequest.getSpringComponentModel().setBeanDefinition(beanDefinitionBuilder.getBeanDefinition());
    }

    return true;
  }

  private void processMuleProperties(ComponentAst component, BeanDefinitionBuilder beanDefinitionBuilder,
                                     BeanDefinitionPostProcessor beanDefinitionPostProcessor) {
    // TODO (MULE-19608) remove this method, by having a component building definition that
    // allows to have the properties being set as any other component
    List<ComponentAst> properties = (List<ComponentAst>) component
        .getParameter(DEFAULT_GROUP_NAME, PROPERTY_ELEMENT)
        .getValue().getRight();

    if (properties != null) {
      doProcessMuleProperties(beanDefinitionBuilder, properties.stream());
    }
  }

}
