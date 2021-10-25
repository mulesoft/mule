/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.config.internal.dsl.spring.ObjectBeanDefinitionCreator.CLASS_PARAMETER;
import static org.mule.runtime.config.internal.model.ApplicationModel.OBJECT_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.PROPERTY_ELEMENT;

import static java.util.Collections.emptyMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.functional.Either;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import io.qameta.allure.Issue;

public class ObjectBeanDefinitionCreatorTestCase extends AbstractMuleTestCase {

  @Test
  @Issue("MULE-19886")
  public void objectWithNullProperties() {
    ComponentAst component = mock(ComponentAst.class);
    when(component.getIdentifier()).thenReturn(OBJECT_IDENTIFIER);

    ComponentParameterAst classParameterAst = mock(ComponentParameterAst.class);
    when(classParameterAst.getResolvedRawValue()).thenReturn("org.mule.tck.testmodels.fruit.Apple");
    when(component.getParameter(DEFAULT_GROUP_NAME, CLASS_PARAMETER)).thenReturn(classParameterAst);

    ComponentParameterAst propertyParameterAst = mock(ComponentParameterAst.class);
    when(propertyParameterAst.getValue()).thenReturn(Either.empty());
    when(component.getParameter(DEFAULT_GROUP_NAME, PROPERTY_ELEMENT)).thenReturn(propertyParameterAst);

    CreateComponentBeanDefinitionRequest request = mock(CreateComponentBeanDefinitionRequest.class);
    when(request.getComponent()).thenReturn(component);
    when(request.getSpringComponentModel()).thenReturn(mock(SpringComponentModel.class));

    new ObjectBeanDefinitionCreator().handleRequest(emptyMap(), request);
  }
}
