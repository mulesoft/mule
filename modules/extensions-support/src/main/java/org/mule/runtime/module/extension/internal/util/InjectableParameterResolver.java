/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.core.api.util.IOUtils.ifInputStream;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getImplementingName;
import static org.mule.runtime.module.extension.internal.value.ValueProviderUtils.getParameterNameFromExtractionExpression;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.api.util.NotAnInputStreamException;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingException;
import org.mule.runtime.module.extension.internal.loader.java.property.InjectableParameterInfo;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

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
  private final MuleExpressionLanguage expressionManager;
  private final Map<String, InjectableParameterInfo> injectableParametersMap;

  public InjectableParameterResolver(ParameterizedModel parameterizedModel,
                                     ParameterValueResolver parameterValueResolver,
                                     MuleExpressionLanguage expressionManager,
                                     List<InjectableParameterInfo> injectableParameters) {
    this.expressionManager = expressionManager;
    this.injectableParametersMap = getInjectableParametersMap(injectableParameters);
    this.expressionResolvingContext = createBindingContext(parameterValueResolver, parameterizedModel);
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
    String topLevelRequiredParameter = keywordSafeName(getParameterNameFromExtractionExpression(extractionExpression));
    if (expressionResolvingContext.lookup(topLevelRequiredParameter).isPresent()) {
      try {
        parameterValue = expressionManager
            .evaluate("#[" + sanitizeExpression(injectableParameterInfo.getExtractionExpression()) + "]",
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
    return parameterValue instanceof String stringValue ? stringValue.trim() : parameterValue;
  }

  private Map<String, InjectableParameterInfo> getInjectableParametersMap(List<InjectableParameterInfo> injectableParameters) {
    return injectableParameters.stream().collect(toMap(InjectableParameterInfo::getParameterName,
                                                       injectableParameter -> injectableParameter));
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
        if (!(value instanceof TypedValue)) {
          String mediaType = parameterModel.getType().getMetadataFormat().getValidMimeTypes().iterator().next();
          try {
            // Consume InputStreams so that we can read them multiple times. This will work only for parameters that are
            // represented as an InputStream. If a parameter was a POJO with an InputStream as field, then it can be read only
            // once.
            value = ifInputStream(value, (CheckedFunction<InputStream, ?>) IOUtils::toByteArray);
          } catch (NotAnInputStreamException e) {
            // do nothing, keep using the value as received
          }
          DataType valueDataType = DataType.builder().type(value.getClass()).mediaType(mediaType).build();
          value = new TypedValue<>(value, valueDataType);
        }
        bindingContextBuilder.addBinding(keywordSafeName(parameterModel.getName()), (TypedValue<?>) value);
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

  private String keywordSafeName(String parameterName) {
    return parameterName + "_";
  }

  private String sanitizeExpression(String extractionExpression) {
    String topLevelParameter = getParameterNameFromExtractionExpression(extractionExpression);
    return extractionExpression.replaceFirst(topLevelParameter, keywordSafeName(topLevelParameter));
  }

}
