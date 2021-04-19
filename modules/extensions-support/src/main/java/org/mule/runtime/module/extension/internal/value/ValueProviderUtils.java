/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.value;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;

import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.util.Reference;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueBuilder;
import org.mule.sdk.api.values.ValueProvider;
import org.mule.sdk.api.values.ValueResolvingException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Utility class for {@link ValueProvider} related objects
 *
 * @since 4.0
 */
public class ValueProviderUtils {

  private ValueProviderUtils() {

  }

  // TODO: MULE-19298 - Create interface to provide values to tooling client and remove the following method
  static org.mule.runtime.extension.api.values.ValueBuilder cloneAndEnrichValue(org.mule.runtime.api.value.Value value,
                                                                                Map<Integer, String> partOrderMapping) {
    Value sdkValue = new SdkValueAdapter(value);
    return cloneAndEnrichMuleValue(sdkValue, partOrderMapping, 1);
  }

  // TODO: MULE-19298 - Create interface to provide values to tooling client and remove the following method
  public static org.mule.runtime.extension.api.values.ValueBuilder cloneAndEnrichValue(org.mule.runtime.api.value.Value value,
                                                                                       List<ParameterModel> parameters) {
    Value sdkValue = new SdkValueAdapter(value);
    return cloneAndEnrichMuleValue(sdkValue, orderParts(parameters, null), 1);
  }

  /**
   * Given a {@link Value}, this is navigated recursively cloning each {@link Value} of the tree structure creating a
   * {@link ValueBuilder} and adding the partName of each {@link Value} found.
   *
   * @param value            {@link Value} to be cloned and enriched
   * @param partOrderMapping {@link Map} that contains the mapping of the name of each part of the {@link Value}
   * @return a {@link ValueBuilder} with the cloned and enriched values
   */
  static ValueBuilder cloneAndEnrichValue(Value value, Map<Integer, String> partOrderMapping) {
    return cloneAndEnrichValue(value, partOrderMapping, 1);
  }

  public static ValueBuilder cloneAndEnrichValue(Value value, List<ParameterModel> parameters) {
    return cloneAndEnrichValue(value, orderParts(parameters, null), 1);
  }

  public static ValueBuilder cloneAndEnrichValue(Value value, List<ParameterModel> parameters, String providerId) {
    return cloneAndEnrichValue(value, orderParts(parameters, providerId), 1);
  }

  /**
   * Given a {@link Value}, this is navigated recursively cloning each {@link Value} of the tree structure creating a
   * {@link ValueBuilder} and adding the partName of each {@link Value} found.
   *
   * @param value            {@link Value} to be cloned and enriched
   * @param partOrderMapping {@link Map} that contains the mapping of the name of each part of the {@link Value}
   * @param level            the current level of the part of the {@link Value} to be cloned and enriched
   * @return a {@link ValueBuilder} with the cloned and enriched values
   */
  static ValueBuilder cloneAndEnrichValue(Value value, Map<Integer, String> partOrderMapping, int level) {
    final ValueBuilder keyBuilder =
        ValueBuilder.newValue(value.getId(), partOrderMapping.get(level)).withDisplayName(value.getDisplayName());
    value.getChilds().forEach(childKey -> keyBuilder.withChild(cloneAndEnrichValue(childKey, partOrderMapping, level + 1)));
    return keyBuilder;
  }

  static org.mule.runtime.extension.api.values.ValueBuilder cloneAndEnrichMuleValue(Value value,
                                                                                    Map<Integer, String> partOrderMapping,
                                                                                    int level) {
    final org.mule.runtime.extension.api.values.ValueBuilder keyBuilder =
        org.mule.runtime.extension.api.values.ValueBuilder.newValue(value.getId(), partOrderMapping.get(level))
            .withDisplayName(value.getDisplayName());
    value.getChilds().forEach(childKey -> keyBuilder.withChild(cloneAndEnrichMuleValue(childKey, partOrderMapping, level + 1)));
    return keyBuilder;
  }

  private static Map<Integer, String> orderParts(List<ParameterModel> parameters, String providerId) {
    if (parameters.size() == 1 && !parameters.get(0).getFieldValueProviderModels().isEmpty()) {
      return parameters.get(0).getFieldValueProviderModels().stream()
          .filter(fieldValueProviderModel -> fieldValueProviderModel.getProviderId().equals(providerId))
          .collect(toMap(ValueProviderModel::getPartOrder,
                         fieldModel -> parameters.get(0).getName() + "." + fieldModel.getTargetPath()));
    }

    return parameters.stream()
        .collect(toMap(param -> param.getValueProviderModel().get().getPartOrder(), NamedObject::getName));
  }

  /**
   * Given a list of {@link ParameterModel} retrieves all the parameters that have an associated {@link ValueProviderModel}
   *
   * @param parameterModels Parameters to introspect
   * @return The {@link List} of {@link ParameterModel} that have an associated {@link ValueProviderModel}
   */
  public static List<ValueProviderModel> getValueProviderModels(List<ParameterModel> parameterModels) {
    return parameterModels.stream()
        .map(parameterModel -> parameterModel.getValueProviderModel().orElse(null))
        .filter(Objects::nonNull)
        .collect(toList());
  }

  /**
   * Executes the {@link ValueProvider} logic with the required extension related classloader
   *
   * @param valueResolver {@link Callable} that wraps the logic of resolve the {@link Value values}
   * @return The {@link Set} of resolved {@link Value values}
   * @throws ValueResolvingException if an error occurs trying to resolve the values
   * @since 4.1.1
   */
  public static Set<org.mule.runtime.api.value.Value> valuesWithClassLoader(Callable<Set<org.mule.runtime.api.value.Value>> valueResolver,
                                                                            ExtensionModel extensionModel)
      throws org.mule.runtime.extension.api.values.ValueResolvingException {
    Reference<org.mule.runtime.extension.api.values.ValueResolvingException> exceptionReference = new Reference<>();
    Set<org.mule.runtime.api.value.Value> values =
        withContextClassLoader(getClassLoader(extensionModel), valueResolver,
                               org.mule.runtime.extension.api.values.ValueResolvingException.class, (e) -> {
                                 exceptionReference.set((org.mule.runtime.extension.api.values.ValueResolvingException) e);
                                 return null;
                               });

    if (exceptionReference.get() != null) {
      throw exceptionReference.get();
    }

    return values;
  }

  /**
   * Creates an instance of the given {@link ValueProvider} class and retrieves its id.
   *
   * @param valueProviderClazz a class that implements the {@link ValueProvider} interface.
   * @return The id of the value provider
   *
   * @since 4.4.0
   */
  public static String getValueProviderId(Class valueProviderClazz) {
    if (!org.mule.runtime.extension.api.values.ValueProvider.class.isAssignableFrom(valueProviderClazz) &&
        !ValueProvider.class.isAssignableFrom(valueProviderClazz)) {
      throw new IllegalStateException(format("Value Provider %s does not implement %s or %s",
                                             valueProviderClazz.getName(),
                                             org.mule.runtime.extension.api.values.ValueProvider.class.getName(),
                                             ValueProvider.class.getName()));
    }

    Object valueProviderObject;
    try {
      valueProviderObject = instantiateClass(valueProviderClazz);
    } catch (Exception e) {
      throw new IllegalStateException(format("There was an error creating an instance of %s to retrieve the Id of the provider",
                                             valueProviderClazz.getName()),
                                      e);
    }

    return valueProviderObject instanceof org.mule.runtime.extension.api.values.ValueProvider
        ? ((org.mule.runtime.extension.api.values.ValueProvider) valueProviderObject).getId()
        : ((ValueProvider) valueProviderObject).getId();
  }

  /**
   * Retrieves the name of the parameter an extraction expression is referring to
   *
   * @param extractionExpression an expression of a binding defined in an
   *                             {@link org.mule.runtime.api.meta.model.parameter.ActingParameterModel}
   * @return the name of the parameter it refers to
   * @since 4.4.0
   */
  public static String getParameterNameFromExtractionExpression(String extractionExpression) {
    int parameterNameDelimiter = extractionExpression.indexOf(".");
    return parameterNameDelimiter < 0 ? extractionExpression : extractionExpression.substring(0, parameterNameDelimiter);
  }

}
