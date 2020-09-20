/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang3.text.WordUtils.capitalize;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.NameUtils.hyphenize;
import static org.mule.runtime.api.util.NameUtils.sanitizeName;
import static org.mule.runtime.api.util.NameUtils.toCamelCase;
import static org.mule.runtime.dsl.api.component.DslSimpleType.isSimpleType;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.TypeConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

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

      final ComponentParameterAst paramInOwner = getParamInOwnerComponent(createBeanDefinitionRequest, componentModel);
      final ComponentParameterAst valueParame = componentModel.getParameter("value");

      String value = null;

      if (paramInOwner != null) {
        value = paramInOwner.getResolvedRawValue();
      } else if (valueParame != null) {
        value = valueParame.getResolvedRawValue();
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

  private ComponentParameterAst getParamInOwnerComponent(CreateBeanDefinitionRequest createBeanDefinitionRequest,
                                                         ComponentAst componentModel) {
    ComponentAst ownerComponent = null;
    ParameterizedModel ownerComponentModel = null;
    int ownerIndex = 0;
    for (int i = createBeanDefinitionRequest.getComponentModelHierarchy().size() - 1; i >= 0; --i) {
      final ComponentAst possibleOwner = createBeanDefinitionRequest.getComponentModelHierarchy().get(i);
      final Optional<ParameterizedModel> model = possibleOwner.getModel(ParameterizedModel.class);
      if (model.isPresent()) {
        ownerComponent = possibleOwner;
        ownerComponentModel = model.get();
        ownerIndex = i;
        break;
      }
    }

    if (ownerComponent != null) {
      final String paramName = toCamelCase(componentModel.getIdentifier().getName(), "-");

      if (ownerComponentModel instanceof SourceModel) {
        // For sources, we need to account for the case where parameters in the callbacks may have colliding names.
        // This logic ensures that the parameter fetching logic is consistent with the logic that handles this scenario in
        // previous implementations.

        final ComponentAst possibleGroup = createBeanDefinitionRequest.getComponentModelHierarchy().get(ownerIndex + 1);

        List<ParameterGroupModel> sourceParamGroups = new ArrayList<>();
        sourceParamGroups.addAll(ownerComponentModel.getParameterGroupModels());
        ((SourceModel) ownerComponentModel).getSuccessCallback()
            .ifPresent(scb -> sourceParamGroups.addAll(scb.getParameterGroupModels()));
        ((SourceModel) ownerComponentModel).getErrorCallback()
            .ifPresent(ecb -> sourceParamGroups.addAll(ecb.getParameterGroupModels()));

        for (ParameterGroupModel parameterGroupModel : sourceParamGroups) {
          if (parameterGroupModel.getParameter(paramName).isPresent()
              && parameterGroupModel.isShowInDsl()
              && possibleGroup.getIdentifier().getName().equals(getSanitizedElementName(parameterGroupModel))) {
            return ownerComponent.getParameter(parameterGroupModel.getName(), paramName);
          }
        }
        return null;
      } else {
        ComponentParameterAst paramInOwner =
            ownerComponent.getParameter(paramName);

        if (paramInOwner == null) {
          // XML SDK 1 allows for hyphenized names in parameters, so need to account for those.
          paramInOwner = ownerComponent.getParameter(componentModel.getIdentifier().getName());
        }

        return paramInOwner;
      }
    } else {
      return null;
    }
  }

  private final static Pattern SANITIZE_PATTERN = compile("\\s+");

  /**
   * Provides a sanitized, hyphenized, space-free name that can be used as an XML element-name for a given {@link NamedObject}
   *
   * @param component the {@link NamedObject} who's name we want to convert
   * @return a sanitized, hyphenized, space-free name that can be used as an XML element-name
   */
  // TODO MULE-18660: remove and use a resolved DSLElementSyntax available in the ast
  private static String getSanitizedElementName(NamedObject component) {
    return SANITIZE_PATTERN.matcher(hyphenize(sanitizeName(capitalize(component.getName())))).replaceAll("");
  }

}
