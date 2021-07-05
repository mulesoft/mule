/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getImplementingName;
import static org.mule.runtime.module.extension.internal.value.ValueProviderUtils.getParameterNameFromExtractionExpression;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.api.util.NotAnInputStreamException;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.module.extension.internal.loader.java.property.InjectableParameterInfo;
import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

/**
 * Class in charge of resolving the values of acting parameter given the list of {@link InjectableParameterInfo} , the
 * {@link ParameterizedModel} it is associated to and the {@link ParameterValueResolver} that corresponds to it.
 *
 * @since 4.4.0
 */
public class InjectableParameterResolver {

  private static final Logger LOGGER = getLogger(InjectableParameterResolver.class);

  private final BindingContext expressionResolvingContext;
  private final ExpressionManager expressionManager;
  private final Map<String, InjectableParameterInfo> injectableParametersMap;
  private final Set<String> availableParams = new HashSet<>();

  public InjectableParameterResolver(ParameterizedModel parameterizedModel,
                                     ParameterValueResolver parameterValueResolver,
                                     ExpressionManager expressionManager,
                                     List<InjectableParameterInfo> injectableParameters) {
    this.expressionManager = expressionManager;
    this.injectableParametersMap = getInjectableParametersMap(injectableParameters);
    this.expressionResolvingContext = createBindingContextAndPopulateAvailableParams(parameterValueResolver, parameterizedModel);
  }

  /**
   * Retrieves the value of a injectable parameter
   *
   * @param parameterName the name of the injectable parameter.
   * @return the value of the injectable parameter.
   */
  public Object getInjectableParameterValue(String parameterName) {
    Object parameterValue = null;
    InjectableParameterInfo injectableParameterInfo = injectableParametersMap.get(parameterName);

    if (injectableParameterInfo == null) {
      throw new IllegalArgumentException("'" + parameterName + "' is not present in the resolver");
    }

    String extractionExpression = injectableParameterInfo.getExtractionExpression();
    String topLevelRequiredParameter = getParameterNameFromExtractionExpression(extractionExpression);
    if (availableParams.contains(topLevelRequiredParameter)) {
      try {
        parameterValue = expressionManager
            .evaluate("#[" + injectableParameterInfo.getExtractionExpression() + "]",
                      DataType.fromType(getType(injectableParameterInfo.getType())),
                      expressionResolvingContext)
            .getValue();
      } catch (IllegalArgumentException e) {
        LOGGER.debug(format("Transformation of injectable parameter '%s' failed, the same value of the resolution will be used.",
                            parameterName),
                     e);
      }
    } else {
      LOGGER.debug("The parameter: '" + topLevelRequiredParameter
          + "' on which the extraction expression was to be executed is not present in the context, returning null");
    }
    return parameterValue;
  }

  private Map<String, InjectableParameterInfo> getInjectableParametersMap(List<InjectableParameterInfo> injectableParameters) {
    return injectableParameters.stream().collect(toMap(InjectableParameterInfo::getParameterName,
                                                       injectableParameter -> injectableParameter));
  }

  private BindingContext createBindingContextAndPopulateAvailableParams(ParameterValueResolver parameterValueResolver,
                                                                        ParameterizedModel parameterizedModel) {
    BindingContext.Builder bindingContextBuilder = BindingContext.builder();
    for (ParameterModel parameterModel : parameterizedModel.getAllParameterModels()) {
      String unaliasedName = getImplementingName(parameterModel);
      Object value = getParameterValueSafely(parameterValueResolver, unaliasedName);
      if (value == null) {
        value = getParameterValueSafely(parameterValueResolver, parameterModel.getName());
      }
      if (value != null) {
        if (!(value instanceof TypedValue)) {
          String mediaType = parameterModel.getType().getMetadataFormat().getValidMimeTypes().iterator().next();
          try {
            value = IOUtils.ifInputStream(value, (CheckedFunction<InputStream, ?>) IOUtils::toByteArray);
          } catch (NotAnInputStreamException e) {
            // do nothing
          }
          DataType valueDataType = DataType.builder().type(value.getClass()).mediaType(mediaType).build();
          value = new TypedValue<>(value, valueDataType);
        }
        bindingContextBuilder.addBinding(keywordSafe(parameterModel.getName()), (TypedValue<?>) value);
        availableParams.add(parameterModel.getName());
      }
    }
    return bindingContextBuilder.build();
  }

  private Object getParameterValueSafely(ParameterValueResolver parameterValueResolver, String parameterName) {
    try {
      return parameterValueResolver.getParameterValue(parameterName);
    } catch (ValueResolvingException e) {
      return null;
    }
  }

  private String keywordSafe(String parameterName) {
    return "_" + parameterName;
  }

  private String sanitizeExpression(String extractionExpression) {
    StringBuilder sanitazedExpression = new StringBuilder();
    sanitazedExpression.append("\"");
    int extractionExpressionLength = extractionExpression.length();
    int firstDotIndex = extractionExpression.indexOf(".");

    if (firstDotIndex == -1) {
      sanitazedExpression.append(extractionExpression);
      sanitazedExpression.append("\"");
    } else {
      sanitazedExpression.append(extractionExpression.substring(0, firstDotIndex));
      sanitazedExpression.append("\"");
      sanitazedExpression.append(extractionExpression.substring(firstDotIndex, extractionExpressionLength));
    }
    return sanitazedExpression.toString();
  }

}
