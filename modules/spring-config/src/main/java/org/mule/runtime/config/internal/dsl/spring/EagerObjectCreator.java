/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.lang.String.format;
import static org.apache.commons.beanutils.BeanUtils.setProperty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.ioc.ObjectProvider;
import org.mule.runtime.config.api.dsl.processor.AbstractAttributeDefinitionVisitor;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.factories.ConstantFactoryBean;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.SetterAttributeDefinition;

import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Optional;

/**
 * Creates objects form the DSL representation which are required before any other object. Such objects do not support full DI
 * since they must be fully functional before all the artifacts objects are created.
 *
 * @since 4.0
 */
class EagerObjectCreator extends BeanDefinitionCreator {

  /**
   * These are the set of component model types that will not support complete dependency injection and lifecycle capabilities
   * because they are required to be created before the whole
   */
  private final ImmutableSet<Class<?>> earlyCreationObjectTypes =
      ImmutableSet.<Class<?>>builder()
          .add(ObjectProvider.class)
          .build();

  @Override
  boolean handleRequest(CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    SpringComponentModel componentModel = createBeanDefinitionRequest.getComponentModel();
    Class<?> type = componentModel.getType();
    if (type == null) {
      return false;
    }
    Optional<Class<?>> foundClass = earlyCreationObjectTypes.stream().filter(clazz -> clazz.isAssignableFrom(type)).findAny();
    return foundClass.map(clazz -> {
      ComponentBuildingDefinition componentBuildingDefinition = createBeanDefinitionRequest.getComponentBuildingDefinition();
      Object instance;
      try {
        instance = type.newInstance();
      } catch (Exception e) {
        throw new MuleRuntimeException(createStaticMessage("Could not create an instance of '%s' using default constructor. Early created object must have a default constructor",
                                                           type.getName()));
      }
      componentBuildingDefinition.getSetterParameterDefinitions().forEach(attributeDefinition -> {
        SetterAttributeDefinition setterAttributeDefinition = (SetterAttributeDefinition) attributeDefinition;
        setterAttributeDefinition.getAttributeDefinition().accept(new AbstractAttributeDefinitionVisitor() {

          @Override
          public void onUndefinedSimpleParameters() {
            Map<String, String> parameters = componentModel.getParameters();
            String attributeName = setterAttributeDefinition.getAttributeName();
            try {
              setProperty(instance, attributeName, parameters);
            } catch (Exception e) {
              throw new MuleRuntimeException(e);
            }
          }

          @Override
          protected void doOnOperation(String operation) {
            throw new MuleRuntimeException(createStaticMessage(format("Attribute definition with operation '%s' is not supported for earlyCreationObjects",
                                                                      operation)));
          }
        });
      });
      componentModel.setObjectInstance(instance);
      componentModel.setBeanDefinition(rootBeanDefinition(ConstantFactoryBean.class)
          .addConstructorArgValue(instance).getBeanDefinition());
      return true;
    }).orElse(false);
  }
}
