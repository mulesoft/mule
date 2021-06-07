/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getImplementingName;
import static org.mule.runtime.module.extension.internal.value.ValueProviderUtils.getParameterNameFromExtractionExpression;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.module.extension.internal.loader.java.property.InjectableParameterInfo;
import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;

/**
 * Class in charge of resolving the values of acting parameter given the list of {@link InjectableParameterInfo} , the
 * {@link ParameterizedModel} it is associated to and the {@link ParameterValueResolver} that corresponds to it.
 *
 * @since 4.4.0
 */
public class InjectableParameterResolver {

  private static final Logger LOGGER = getLogger(InjectableParameterResolver.class);

  private static final String EXPRESSION_PREFIX = "#[{";
  private static final String EXPRESSION_SUFFIX = "}]";
  private static final String EMPTY_EXPRESSION = EXPRESSION_PREFIX + EXPRESSION_SUFFIX;

  private final Map<String, Object> resolvedParameterValues;
  private final ExpressionManager expressionManager;
  private final Map<String, InjectableParameterInfo> injectableParametersMap;

  public InjectableParameterResolver(ParameterizedModel parameterizedModel,
                                     ParameterValueResolver parameterValueResolver,
                                     ExpressionManager expressionManager,
                                     List<InjectableParameterInfo> injectableParameters) {
    this.expressionManager = expressionManager;
    this.injectableParametersMap = getInjectableParametersMap(injectableParameters);
    this.resolvedParameterValues = getResolvedValues(parameterValueResolver, parameterizedModel);
  }

  /**
   * Retrieves the value of a injectable parameter
   *
   * @param parameterName the name of the injectable parameter.
   * @return the value of the injectable parameter.
   */
  public Object getInjectableParameterValue(String parameterName) {
    Object parameterValue;
    parameterValue = resolvedParameterValues.get(parameterName);
    InjectableParameterInfo injectableParameterInfo = injectableParametersMap.get(parameterName);

    if (parameterValue != null) {
      try {
        parameterValue = expressionManager
            .evaluate("#[payload]",
                      DataType.fromType(getClassFromType(injectableParameterInfo.getType())),
                      BindingContext.builder()
                          .addBinding("payload", new TypedValue(parameterValue, DataType.fromObject(parameterValue))).build())
            .getValue();
      } catch (ClassNotFoundException e) {
        LOGGER.debug("Transformation of injectable parameter '{}' failed, the same value of the resolution will be used.",
                     parameterName);
      }

    }

    return parameterValue;
  }

  private Map<String, InjectableParameterInfo> getInjectableParametersMap(List<InjectableParameterInfo> injectableParameters) {
    return injectableParameters.stream().collect(toMap(injectableParameter -> injectableParameter.getParameterName(),
                                                       injectableParameter -> injectableParameter));
  }

  private Map<String, Object> getResolvedValues(ParameterValueResolver parameterValueResolver,
                                                ParameterizedModel parameterizedModel) {
    BindingContext bindingContext = createBindingContext(parameterValueResolver, parameterizedModel);
    String expression = getResolvedParameterValuesExpression(bindingContext.identifiers());

    if (!expression.equals(EMPTY_EXPRESSION)) {
      return (Map<String, Object>) expressionManager
          .evaluate(expression, DataType.builder().mapType(Map.class).build(), bindingContext)
          .getValue();
    } else {
      return emptyMap();
    }
  }

  private BindingContext createBindingContext(ParameterValueResolver parameterValueResolver,
                                              ParameterizedModel parameterizedModel) {
    BindingContext.Builder bindingContextBuilder = BindingContext.builder();

    for (ParameterModel parameterModel : parameterizedModel.getAllParameterModels()) {
      String unaliasedName = getImplementingName(parameterModel);
      Object value = getParameterValueSafely(parameterValueResolver, unaliasedName);
      if (value == null) {
        value = getParameterValueSafely(parameterValueResolver, parameterModel.getName());
      }

      if (value != null) {
        if(!(value instanceof TypedValue)) {
          String mediaType = parameterModel.getType().getMetadataFormat().getValidMimeTypes().iterator().next();
          DataType valueDataType = DataType.builder().type(value.getClass()).mediaType(mediaType).build();
          value = new TypedValue<>(value, valueDataType);
        }
        bindingContextBuilder.addBinding(parameterModel.getName(), (TypedValue) value);
      }
    }
    return bindingContextBuilder.build();
  }

  private String getResolvedParameterValuesExpression(Collection<String> identifiers) {
    StringBuilder expression = new StringBuilder();
    expression.append(EXPRESSION_PREFIX);
    expression.append(
                      injectableParametersMap.values().stream()
                          .filter(injectableParameterInfo -> identifiers
                              .contains(getParameterNameFromExtractionExpression(injectableParameterInfo
                                  .getExtractionExpression())))
                          .map(injectableParameterInfo -> "\""
                              + injectableParameterInfo.getParameterName() + "\"  : "
                              + injectableParameterInfo.getExtractionExpression())
                          .collect(Collectors.joining(", ")));
    expression.append(EXPRESSION_SUFFIX);

    return expression.toString();
  }

  private Class getClassFromType(MetadataType parameterMetadataType) throws ClassNotFoundException {
    return Thread.currentThread().getContextClassLoader()
        .loadClass(parameterMetadataType.getAnnotation(ClassInformationAnnotation.class)
            .map(classInformationAnnotation -> classInformationAnnotation.getClassname())
            .orElse(Object.class.getName()));
  }

  private Object getParameterValueSafely(ParameterValueResolver parameterValueResolver, String parameterName) {
    try {
      return parameterValueResolver.getParameterValue(parameterName);
    } catch (ValueResolvingException e) {
      return null;
    }
  }

}
