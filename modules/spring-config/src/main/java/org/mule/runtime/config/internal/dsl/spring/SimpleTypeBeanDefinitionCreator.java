/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.NameUtils.toCamelCase;
import static org.mule.runtime.ast.api.ComponentAst.BODY_RAW_PARAM_NAME;
import static org.mule.runtime.dsl.api.component.DslSimpleType.isSimpleType;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
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
 * @since 4.0
 */
class SimpleTypeBeanDefinitionCreator extends BeanDefinitionCreator {

  @Override
  boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels,
                        CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    Class<?> type = createBeanDefinitionRequest.retrieveTypeVisitor().getType();
    if (isSimpleType(type)) {
      ComponentAst componentModel = createBeanDefinitionRequest.getComponentModel();
      createBeanDefinitionRequest.getSpringComponentModel().setType(type);

      ComponentAst ownerComponent = null;
      for (int i = createBeanDefinitionRequest.getComponentModelHierarchy().size() - 1; i >= 0; --i) {
        final ComponentAst possibleOwner = createBeanDefinitionRequest.getComponentModelHierarchy().get(i);
        if (possibleOwner.getModel(ParameterizedModel.class).isPresent()) {
          ownerComponent = possibleOwner;
          break;
        }
      }

      final ComponentParameterAst paramInOwner =
          ownerComponent != null
              ? ownerComponent.getParameter(toCamelCase(componentModel.getIdentifier().getName(), "-"))
              : null;
      final ComponentParameterAst valueParame = componentModel.getParameter("value");

      String value = null;

      if (paramInOwner != null) {
        value = paramInOwner.getRawValue();
      } else if (valueParame != null) {
        value = valueParame.getRawValue();
      }

      if (value == null) {
        // TODO MULE-17859 and MULE-18327 remove this fallback
        value = fallback(componentModel);
      }

      if (value == null) {
        throw new MuleRuntimeException(createStaticMessage("Parameter at %s:%s must provide a non-empty value",
                                                           componentModel.getMetadata().getFileName()
                                                               .orElse("unknown"),
                                                           componentModel.getMetadata().getStartLine().orElse(-1)));
      }
      Optional<TypeConverter> typeConverterOptional =
          createBeanDefinitionRequest.getComponentBuildingDefinition().getTypeConverter();
      createBeanDefinitionRequest.getSpringComponentModel()
          .setBeanDefinition(getConvertibleBeanDefinition(type, value, typeConverterOptional));
      return true;
    }
    return false;
  }

  private String fallback(ComponentAst componentModel) {
    final Optional<String> textContent = componentModel.getRawParameterValue(BODY_RAW_PARAM_NAME);
    final Optional<String> valueParam = componentModel.getRawParameterValue("value");

    return textContent.orElseGet(() -> valueParam.orElse(null));
  }
}
