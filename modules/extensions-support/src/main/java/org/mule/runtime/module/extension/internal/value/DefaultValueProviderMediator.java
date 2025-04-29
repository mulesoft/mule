/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.value;

import static org.mule.runtime.extension.api.values.ValueResolvingException.INVALID_VALUE_RESOLVER_NAME;
import static org.mule.runtime.extension.api.values.ValueResolvingException.UNKNOWN;
import static org.mule.runtime.module.extension.internal.loader.utils.FieldValueProviderNameUtils.getParameterName;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.withRefreshToken;
import static org.mule.runtime.module.extension.internal.value.ValueProviderUtils.cloneAndEnrichValue;

import static java.lang.String.format;
import static java.util.stream.Collectors.toCollection;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.api.tooling.valueprovider.ValueProviderMediator;
import org.mule.runtime.module.extension.internal.loader.java.property.FieldsValueProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ValueProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.sdk.api.values.ValueBuilder;
import org.mule.sdk.api.values.ValueProvider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Resolves a parameter's {@link Value values} by coordinating the several moving parts that are affected by the {@link Value}
 * fetching process, so that such pieces can remain decoupled.
 *
 * @since 4.0
 */
public final class DefaultValueProviderMediator<T extends ParameterizedModel & EnrichableModel> implements ValueProviderMediator {

  private final T containerModel;
  private final Supplier<ReflectionCache> reflectionCache;
  private final Supplier<ExpressionManager> expressionManager;
  private final Supplier<Injector> injector;
  private final Supplier<Object> nullSupplier = () -> null;

  /**
   * Creates a new instance of the mediator
   *
   * @param containerModel container model which is a {@link ParameterizedModel} and {@link EnrichableModel}
   * @param muleContext    context to be able to initialize {@link ValueProvider} if necessary
   */
  public DefaultValueProviderMediator(T containerModel,
                                      Supplier<ReflectionCache> reflectionCache,
                                      Supplier<ExpressionManager> expressionManager,
                                      Supplier<Injector> injector) {
    this.containerModel = containerModel;
    this.reflectionCache = reflectionCache;
    this.expressionManager = expressionManager;
    this.injector = injector;
  }

  /**
   * Given the name of a parameter or parameter group, and if the parameter supports it, this will try to resolve the {@link Value
   * values} for the parameter.
   *
   * @param parameterName          the name of the parameter to resolve their possible {@link Value values}
   * @param parameterValueResolver parameter resolver required if the associated {@link ValueProvider} requires the value of
   *                               parameters from the same parameter container.
   * @return a {@link Set} of {@link Value} correspondent to the given parameter
   * @throws org.mule.sdk.api.values.ValueResolvingException if an error occurs resolving {@link Value values}
   */
  public Set<Value> getValues(String parameterName, ParameterValueResolver parameterValueResolver)
      throws ValueResolvingException {
    return getValues(parameterName, parameterValueResolver, nullSupplier, nullSupplier);
  }

  /**
   * Given the name of a parameter or parameter group and the target path of a field of the parameter, if the parameter supports
   * it, this will try to resolve the {@link Value values} for the parameter's field.
   *
   * @param parameterName          the name of the parameter to resolve their possible {@link Value values}
   * @param parameterValueResolver parameter resolver required if the associated {@link ValueProvider} requires the value of
   *                               parameters from the same parameter container.
   * @param targetSelector         the target path of the field of the parameter.
   * 
   * @return a {@link Set} of {@link Value} correspondent to the given parameter
   * @throws ValueResolvingException if an error occurs resolving {@link Value values}
   */
  public Set<Value> getValues(String parameterName, String targetSelector, ParameterValueResolver parameterValueResolver)
      throws ValueResolvingException {
    return getValues(parameterName, parameterValueResolver, targetSelector, nullSupplier, nullSupplier);
  }

  /**
   * Given the name of a parameter or parameter group, and if the parameter supports it, this will try to resolve the {@link Value
   * values} for the parameter.
   *
   * @param parameterName          the name of the parameter to resolve their possible {@link Value values}
   * @param parameterValueResolver parameter resolver required if the associated {@link ValueProvider} requires the value of
   *                               parameters from the same parameter container.
   * @param connectionSupplier     supplier of connection instances related to the container and used, if necessary, by the
   *                               {@link ValueProvider}
   * @param configurationSupplier  supplier of connection instances related to the container and used, if necessary, by the
   *                               {@link ValueProvider}
   * @return a {@link Set} of {@link Value} correspondent to the given parameter
   * @throws ValueResolvingException if an error occurs resolving {@link Value values}
   */
  public Set<Value> getValues(String parameterName, ParameterValueResolver parameterValueResolver,
                              Supplier<Object> connectionSupplier, Supplier<Object> configurationSupplier)
      throws ValueResolvingException {
    return getValues(parameterName, parameterValueResolver, connectionSupplier, configurationSupplier, null);
  }

  /**
   * Given the name of a parameter or parameter group and the target path of a field of the parameter, if the parameter supports
   * it, this will try to resolve the {@link Value values} for the parameter's field.
   *
   * @param parameterName          the name of the parameter to resolve their possible {@link Value values}
   * @param parameterValueResolver parameter resolver required if the associated {@link ValueProvider} requires the value of
   *                               parameters from the same parameter container.
   * @param targetSelector         the target selector of the field of the parameter.
   * @param connectionSupplier     supplier of connection instances related to the container and used, if necessary, by the
   *                               {@link ValueProvider}
   * @param configurationSupplier  supplier of connection instances related to the container and used, if necessary, by the
   *                               {@link ValueProvider}
   * 
   * @return a {@link Set} of {@link Value} correspondent to the given parameter
   * @throws ValueResolvingException if an error occurs resolving {@link Value values}
   */
  @Override
  public Set<Value> getValues(String parameterName, ParameterValueResolver parameterValueResolver, String targetSelector,
                              Supplier<Object> connectionSupplier, Supplier<Object> configurationSupplier)
      throws ValueResolvingException {
    return getValues(parameterName, parameterValueResolver, targetSelector, connectionSupplier, configurationSupplier, null);
  }

  /**
   * Given the name of a parameter or parameter group, and if the parameter supports it, this will try to resolve the {@link Value
   * values} for the parameter.
   *
   * @param parameterName          the name of the parameter to resolve their possible {@link Value values}
   * @param parameterValueResolver parameter resolver required if the associated {@link ValueProvider} requires the value of
   *                               parameters from the same parameter container.
   * @param connectionSupplier     supplier of connection instances related to the container and used, if necessary, by the
   *                               {@link ValueProvider}
   * @param configurationSupplier  supplier of connection instances related to the container and used, if necessary, by the
   *                               {@link ValueProvider}
   * @param connectionProvider     the connection provider in charge of providing the connection given by the connection supplier.
   * @return a {@link Set} of {@link Value} correspondent to the given parameter
   * @throws ValueResolvingException if an error occurs resolving {@link Value values}
   */
  @Override
  public Set<Value> getValues(String parameterName, ParameterValueResolver parameterValueResolver,
                              Supplier<Object> connectionSupplier, Supplier<Object> configurationSupplier,
                              ConnectionProvider connectionProvider)
      throws ValueResolvingException {
    return getValues(parameterName, parameterValueResolver, null, connectionSupplier, configurationSupplier, connectionProvider);
  }

  /**
   * Given the name of a parameter or parameter group and the target path of a field of the parameter, if the parameter supports
   * it, this will try to resolve the {@link Value values} for the parameter's field.
   *
   * @param parameterName          the name of the parameter to resolve their possible {@link Value values}
   * @param parameterValueResolver parameter resolver required if the associated {@link ValueProvider} requires the value of
   *                               parameters from the same parameter container.
   * @param targetSelector         the target selector of the field of the parameter.
   * @param connectionSupplier     supplier of connection instances related to the container and used, if necessary, by the
   *                               {@link ValueProvider}
   * @param configurationSupplier  supplier of connection instances related to the container and used, if necessary, by the
   *                               {@link ValueProvider}
   * @param connectionProvider     the connection provider in charge of providing the connection given by the connection supplier.
   * @return a {@link Set} of {@link Value} correspondent to the given parameter
   * @throws ValueResolvingException if an error occurs resolving {@link Value values}
   */
  @Override
  public Set<Value> getValues(String parameterName, ParameterValueResolver parameterValueResolver, String targetSelector,
                              Supplier<Object> connectionSupplier, Supplier<Object> configurationSupplier,
                              ConnectionProvider connectionProvider)
      throws ValueResolvingException {

    List<ParameterModel> parameters = getParameters(parameterName);

    if (parameters.isEmpty()) {
      throw new ValueResolvingException(format("Unable to find model for parameter or parameter group with name '%s'.",
                                               parameterName),
                                        INVALID_VALUE_RESOLVER_NAME);
    }

    ParameterModel parameterModel = parameters.get(0);

    ValueProviderFactoryModelProperty factoryModelProperty;
    if (targetSelector != null) {
      factoryModelProperty =
          parameterModel.getModelProperty(FieldsValueProviderFactoryModelProperty.class)
              .map(fieldsValueProvider -> fieldsValueProvider.getFieldsValueProviderFactories().get(targetSelector))
              .orElseThrow(() -> new ValueResolvingException(
                                                             format("The parameter with name '%s' does not have a Value Provider associated with the targetSelector '%s'",
                                                                    parameterName, targetSelector),
                                                             INVALID_VALUE_RESOLVER_NAME));
    } else {
      factoryModelProperty =
          parameterModel.getModelProperty(ValueProviderFactoryModelProperty.class)
              .orElseThrow(() -> new ValueResolvingException(format("The parameter with name '%s' is not an Values Provider",
                                                                    parameterName),
                                                             INVALID_VALUE_RESOLVER_NAME));
    }

    try {
      return withRefreshToken(connectionProvider,
                              () -> resolveValues(parameters, factoryModelProperty, parameterValueResolver,
                                                  connectionSupplier, configurationSupplier, targetSelector));
    } catch (ValueResolvingException e) {
      throw e;
    } catch (Exception e) {
      throw new ValueResolvingException(format("An error occurred trying to resolve the Values for parameter '%s' of component '%s'. Cause: %s",
                                               parameterName, containerModel.getName(), e.getMessage()),
                                        UNKNOWN, e);
    }

  }

  private Set<Value> resolveValues(List<ParameterModel> parameters, ValueProviderFactoryModelProperty factoryModelProperty,
                                   ParameterValueResolver parameterValueResolver, Supplier<Object> connectionSupplier,
                                   Supplier<Object> configurationSupplier, String targerSelector)
      throws ValueResolvingException {
    try {
      ValueProvider valueProvider =
          factoryModelProperty
              .createFactory(parameterValueResolver, connectionSupplier, configurationSupplier, reflectionCache.get(),
                             expressionManager.get(),
                             injector.get(),
                             containerModel)
              .createValueProvider();

      Set<org.mule.sdk.api.values.Value> valueSet = valueProvider.resolve();

      return valueSet.stream()
          .map(option -> cloneAndEnrichValue(option, parameters, valueProvider.getId(), targerSelector))
          .map(ValueBuilder::build)
          .map(MuleValueAdapter::new)
          .collect(toCollection(LinkedHashSet::new));
    } catch (org.mule.sdk.api.values.ValueResolvingException e) {
      throw new ValueResolvingException(e.getMessage(), e.getFailureCode(), e.getCause());
    }
  }

  /**
   * Given a parameter or parameter group name, this method will look for the correspondent {@link ParameterModel} or
   * {@link ParameterGroupModel}
   *
   * @param valueName name of value provider
   * @return the correspondent parameter
   */
  private List<ParameterModel> getParameters(String valueName) {
    List<ParameterModel> parameterModels = new ArrayList<>();
    for (ParameterModel parameterModel : containerModel.getAllParameterModels()) {
      if (parameterModel.getValueProviderModel().isPresent() &&
          parameterModel.getValueProviderModel().get().getProviderName().equals(valueName)) {
        parameterModels.add(parameterModel);
      } else if (parameterModel.getFieldValueProviderModels()
          .stream()
          .anyMatch(fieldValueProviderModel -> getParameterName(fieldValueProviderModel).equals(valueName))) {

        parameterModels.add(parameterModel);
      }
    }

    return parameterModels;
  }
}
