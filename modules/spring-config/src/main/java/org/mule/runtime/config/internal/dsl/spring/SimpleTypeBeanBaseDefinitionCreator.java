/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.dsl.api.component.DslSimpleType.isSimpleType;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.TypeConverter;

import java.util.Map;
import java.util.Optional;

/**
 * Bean definition creator for elements that end up representing simple types.
 * <p>
 * Elements that represent a simple type have the form {@code <element value="simpleValue"/>} or
 * {@code <element>simpleValue</element>}
 *
 * @since 4.4
 */
abstract class SimpleTypeBeanBaseDefinitionCreator<R extends CreateBeanDefinitionRequest>
    extends BeanDefinitionCreator<R> {

  @Override
  protected final boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                        R createBeanDefinitionRequest) {
    Class<?> type = createBeanDefinitionRequest.getSpringComponentModel().getType();

    if (isSimpleType(type)
        // Expressions are String, which are simple values for the spring bean definitions
        || isExpressionValue(createBeanDefinitionRequest)) {
      createBeanDefinitionRequest.getSpringComponentModel().setType(type);
      return doHandleRequest(createBeanDefinitionRequest, type);
    }

    return false;
  }

  protected boolean isExpressionValue(R request) {
    return false;
  }

  protected abstract boolean doHandleRequest(R createBeanDefinitionRequest, Class<?> type);

  protected final void setConvertibleBeanDefinition(R createBeanDefinitionRequest, Class<?> type,
                                                    String value) {
    Optional<TypeConverter> typeConverterOptional =
        createBeanDefinitionRequest.getComponentBuildingDefinition().getTypeConverter();

    SpringComponentModel springComponentModel = createBeanDefinitionRequest.getSpringComponentModel();

    springComponentModel.setBeanDefinition(getConvertibleBeanDefinition(type, value, typeConverterOptional));
  }

}
