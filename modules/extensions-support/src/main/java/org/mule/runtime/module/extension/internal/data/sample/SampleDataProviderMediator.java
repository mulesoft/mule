/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.data.sample;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.extension.api.values.ValueResolvingException.INVALID_VALUE_RESOLVER_NAME;
import static org.mule.runtime.extension.api.values.ValueResolvingException.UNKNOWN;
import static org.mule.runtime.module.extension.internal.value.ValueProviderUtils.cloneAndEnrichValue;
import static org.mule.sdk.api.data.sample.SampleDataException.NOT_SUPPORTED;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.runtime.module.extension.internal.loader.java.property.SampleDataProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ValueProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.data.sample.SampleDataProvider;
import org.mule.sdk.api.runtime.operation.Result;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class SampleDataProviderMediator<T extends ParameterizedModel & EnrichableModel> {

  private final T componentModel;
  private final Supplier<MuleContext> muleContext;
  private final Supplier<ReflectionCache> reflectionCache;
  private final Supplier<Object> nullSupplier = () -> null;
  private final SampleDataProviderFactoryModelProperty sampleDataProperty;

  /**
   * Creates a new instance of the mediator
   *
   * @param componentModel container model which is a {@link ParameterizedModel} and {@link EnrichableModel}
   * @param muleContext    context to be able to initialize {@link SampleDataProvider} if necessary
   */
  public SampleDataProviderMediator(T componentModel, Supplier<MuleContext> muleContext, Supplier<ReflectionCache> reflectionCache) {
    this.componentModel = componentModel;
    this.muleContext = muleContext;
    this.reflectionCache = reflectionCache;
    sampleDataProperty = componentModel.getModelProperty(SampleDataProviderFactoryModelProperty.class).orElse(null);
  }

  /**
   * Given the name of a parameter or parameter group, and if the parameter supports it, this will try to resolve
   * the {@link Value values} for the parameter.
   *
   * @param parameterValueResolver parameter resolver required if the associated {@link ValueProvider} requires
   *                               the value of parameters from the same parameter container.
   * @return a {@link Set} of {@link Value} correspondent to the given parameter
   * @throws ValueResolvingException if an error occurs resolving {@link Value values}
   */
  public Message getSample(ParameterValueResolver parameterValueResolver) throws SampleDataException {
    return getValues(parameterName, parameterValueResolver, nullSupplier, nullSupplier);
  }

  /**
   * Given the name of a parameter or parameter group, and if the parameter supports it, this will try to resolve
   * the {@link Value values} for the parameter.
   *
   * @param parameterValueResolver parameter resolver required if the associated {@link ValueProvider} requires
   *                               the value of parameters from the same parameter container.
   * @param connectionSupplier     supplier of connection instances related to the container and used, if necessary, by the
   *                               {@link ValueProvider}
   * @param configurationSupplier  supplier of connection instances related to the container and used, if necessary, by the
   *                               {@link ValueProvider}
   * @return a {@link Set} of {@link Value} correspondent to the given parameter
   * @throws SampleDataException if an error occurs resolving the sample data
   */
  public Message getSample(ParameterValueResolver parameterValueResolver,
                           Supplier<Object> connectionSupplier,
                           Supplier<Object> configurationSupplier) throws SampleDataException {
    if (sampleDataProperty == null) {
      throw new SampleDataException(
              format("Component '%s' does not support Sample Data resolution", componentModel.getName()),
              NOT_SUPPORTED);
    }

    SampleDataProviderFactory factory = sampleDataProperty.createFactory(
            parameterValueResolver,
            connectionSupplier,
            configurationSupplier,
            reflectionCache.get(),
            muleContext.get());

    SampleDataProvider provider = factory.createSampleDataProvider();

    Result result = provider.getSample();


    try {
      return resolveValues(parameters, factoryModelProperty, parameterValueResolver, connectionSupplier, configurationSupplier);
    } catch (ValueResolvingException e) {
      throw e;
    } catch (Exception e) {
      throw new ValueResolvingException(format("An error occurred trying to resolve the Values for parameter '%s' of component '%s'. Cause: %s",
              parameterName, componentModel.getName(), e.getMessage()),
              UNKNOWN, e);
    }
  }

  private Set<Value> resolveValues(List<ParameterModel> parameters, ValueProviderFactoryModelProperty factoryModelProperty,
                                   ParameterValueResolver parameterValueResolver, Supplier<Object> connectionSupplier,
                                   Supplier<Object> configurationSupplier)
          throws ValueResolvingException {

    ValueProvider valueProvider =
            factoryModelProperty
                    .createFactory(parameterValueResolver, connectionSupplier, configurationSupplier, reflectionCache.get(),
                            muleContext.get())
                    .createValueProvider();

    Set<Value> valueSet = valueProvider.resolve();

    return valueSet.stream()
            .map(option -> cloneAndEnrichValue(option, parameters))
            .map(ValueBuilder::build)
            .collect(toSet());
  }

  /**
   * Given a parameter or parameter group name, this method will look for the correspondent {@link ParameterModel} or
   * {@link ParameterGroupModel}
   *
   * @param valueName name of value provider
   * @return the correspondent parameter
   */
  private List<ParameterModel> getParameters(String valueName) {
    return componentModel.getAllParameterModels()
            .stream()
            .filter(parameterModel -> parameterModel.getValueProviderModel()
                    .map(provider -> provider.getProviderName().equals(valueName))
                    .orElse(false))
            .collect(toList());
  }

}
