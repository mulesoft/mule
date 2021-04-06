/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.config.internal.dsl.spring.ParameterGroupUtils.getSourceCallbackAwareParameter;
import static org.mule.runtime.dsl.api.component.DslSimpleType.isSimpleType;

import java.util.HashMap;
import java.util.Set;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.internal.DefaultComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.TypeConverter;

import java.util.Map;
import java.util.Optional;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;

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
    ComponentAst ownerComponent = resolveOwnerComponent(createBeanDefinitionRequest);

    if (ownerComponent != null) {

      String name = componentModel.getIdentifier().getName();
      final String paramName = getParamName((DefaultComponentAst) ownerComponent, name);

      ParameterizedModel ownerComponentModel = ownerComponent.getModel(ParameterizedModel.class).get();
      if (ownerComponent != componentModel && ownerComponentModel instanceof SourceModel) {
        // For sources, we need to account for the case where parameters in the callbacks may have colliding names.
        // This logic ensures that the parameter fetching logic is consistent with the logic that handles this scenario in
        // previous implementations.
        int ownerIndex = createBeanDefinitionRequest.getComponentModelHierarchy().indexOf(ownerComponent);
        final ComponentAst possibleGroup = createBeanDefinitionRequest.getComponentModelHierarchy().get(ownerIndex + 1);

        return getSourceCallbackAwareParameter(ownerComponent, paramName, possibleGroup, (SourceModel) ownerComponentModel);
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

  private String getParamName(DefaultComponentAst ownerComponent, String name) {
    Optional<DslElementSyntax> optionalDslElementSyntax = ownerComponent.getGenerationInformation().getSyntax();

    if (!optionalDslElementSyntax.isPresent()) {
      return null;
    }

    return getElementNameToParamNameMap(optionalDslElementSyntax.get()).get(name);
  }

  private HashMap<String, String> getElementNameToParamNameMap(DslElementSyntax dslElementSyntax) {
    Set<Map.Entry<String, DslElementSyntax>> entries = dslElementSyntax.getContainedElementsByName().entrySet();

    HashMap<String, String> elementNameToParamNameMap = new HashMap<>();
    for (Map.Entry<String, DslElementSyntax> entry : entries) {
      elementNameToParamNameMap.put(entry.getValue().getElementName(), entry.getKey());
    }
    return elementNameToParamNameMap;
  }

}
