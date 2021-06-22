/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.dsl.api.component.DslSimpleType.isSimpleType;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.TypeConverter;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Bean definition creator for elements that end up representing simple types.
 * <p>
 * Elements that represent a simple type have the form {@code <element value="simpleValue"/>} or
 * {@code <element>simpleValue</element>}
 *
 * @since 4.0
 */
class SimpleTypeBeanDefinitionCreator extends BeanDefinitionCreator {

  public SimpleTypeBeanDefinitionCreator() {}

  @Override
  boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels,
                        CreateBeanDefinitionRequest createBeanDefinitionRequest,
                        Consumer<ComponentAst> nestedComponentParamProcessor,
                        Consumer<SpringComponentModel> componentBeanDefinitionHandler) {
    Class<?> type = createBeanDefinitionRequest.retrieveTypeVisitor().getType();

    if (!isSimpleType(type)) {
      return false;
    }

    createBeanDefinitionRequest.getSpringComponentModel().setType(type);

    final ComponentAst paramOwner = createBeanDefinitionRequest.getParamOwnerComponentModel();
    final ComponentParameterAst param = paramOwner != null
        ? paramOwner.getParameter(createBeanDefinitionRequest.getParamName())
        : null;

    if (param != null) {
      this.setConvertibleBeanDefinition(createBeanDefinitionRequest, type, param.getResolvedRawValue());
      componentBeanDefinitionHandler.accept(createBeanDefinitionRequest.getSpringComponentModel());
      return true;
    }

    ComponentAst componentModel = createBeanDefinitionRequest.getComponentModel();
    final ComponentParameterAst valueParam = componentModel.getParameter("value");

    if (valueParam == null || valueParam.getResolvedRawValue() == null) {
      return false;
      // throw new MuleRuntimeException(createStaticMessage("Parameter at %s:%s must provide a non-empty value",
      // componentModel.getMetadata().getFileName().orElse("unknown"),
      // componentModel.getMetadata().getStartLine().orElse(-1)));
    }

    this.setConvertibleBeanDefinition(createBeanDefinitionRequest, type, valueParam.getResolvedRawValue());

    componentBeanDefinitionHandler.accept(createBeanDefinitionRequest.getSpringComponentModel());

    return true;
  }

  private void setConvertibleBeanDefinition(CreateBeanDefinitionRequest createBeanDefinitionRequest, Class<?> type,
                                            String value) {
    Optional<TypeConverter> typeConverterOptional =
        createBeanDefinitionRequest.getComponentBuildingDefinition().getTypeConverter();

    SpringComponentModel springComponentModel = createBeanDefinitionRequest.getSpringComponentModel();

    springComponentModel.setBeanDefinition(getConvertibleBeanDefinition(type, value, typeConverterOptional));
  }

}
