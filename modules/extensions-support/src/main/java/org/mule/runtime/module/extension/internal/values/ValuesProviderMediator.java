/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.values;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.values.ValueResolvingException.INVALID_PARAMETER;
import static org.mule.runtime.api.values.ValueResolvingException.UNKNOWN;
import static org.mule.runtime.module.extension.internal.values.ValuesProviderMediatorUtils.cloneAndEnrichMetadataKey;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.parameter.HasValuesProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.values.Value;
import org.mule.runtime.api.values.ValueBuilder;
import org.mule.runtime.api.values.ValueResolvingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.values.ValuesProvider;
import org.mule.runtime.module.extension.internal.loader.java.property.ValuesProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Resolves a parameter's {@link Value values} by coordinating the several moving parts that are affected by the
 * {@link Value} fetching process, so that such pieces can remain decoupled.
 *
 * @since 4.0
 */
public final class ValuesProviderMediator<T extends ParameterizedModel & EnrichableModel> {

  private T containerModel;
  private MuleContext muleContext;
  private Supplier<Object> NULL_SUPPLIER = () -> null;

  /**
   * Creates a new instance of the mediator
   *
   * @param containerModel container model which is a {@link ParameterizedModel} and {@link EnrichableModel}
   * @param muleContext context to be able to initialize {@link ValuesProvider} if necessary
   */
  public ValuesProviderMediator(T containerModel, MuleContext muleContext) {
    this.containerModel = containerModel;
    this.muleContext = muleContext;
  }

  /**
   * Given the name of a parameter or parameter group, and if the parameter supports it, this will try to resolve
   * the {@link Value values} for the parameter.
   *
   * @param parameterName          the name of the parameter to resolve their possible {@link Value values}
   * @param parameterValueResolver parameter resolver required if the associated {@link ValuesProvider} requires
   *                               the value of parameters from the same parameter container.
   * @return a {@link Set} of {@link Value} correspondent to the given parameter
   * @throws ValueResolvingException if an error occurs resolving {@link Value values}
   */
  public Set<Value> getValues(String parameterName, ParameterValueResolver parameterValueResolver)
      throws ValueResolvingException {
    return getValues(parameterName, parameterValueResolver, NULL_SUPPLIER, NULL_SUPPLIER);
  }

  /**
   * Given the name of a parameter or parameter group, and if the parameter supports it, this will try to resolve
   * the {@link Value values} for the parameter.
   *
   * @param parameterName          the name of the parameter to resolve their possible {@link Value values}
   * @param parameterValueResolver parameter resolver required if the associated {@link ValuesProvider} requires
   *                               the value of parameters from the same parameter container.
   * @param connectionSupplier     supplier of connection instances related to the container and used, if necessary, by the
   *                               {@link ValuesProvider}
   * @param configurationSupplier  supplier of connection instances related to the container and used, if necessary, by the
   *                               {@link ValuesProvider}
   * @return a {@link Set} of {@link Value} correspondent to the given parameter
   * @throws ValueResolvingException if an error occurs resolving {@link Value values}
   */
  public <P extends EnrichableModel & HasValuesProviderModel> Set<Value> getValues(String parameterName,
                                                                                   ParameterValueResolver parameterValueResolver,
                                                                                   Supplier<Object> connectionSupplier,
                                                                                   Supplier<Object> configurationSupplier)
      throws ValueResolvingException {
    P parameter = (P) getParameter(parameterName)
        .orElseThrow(() -> new ValueResolvingException(format("Unable to find model for parameter or parameter group with name '%s'.",
                                                              parameterName),
                                                       INVALID_PARAMETER));

    ValuesProviderFactoryModelProperty factoryModelProperty = parameter.getModelProperty(ValuesProviderFactoryModelProperty.class)
        .orElseThrow(() -> new ValueResolvingException(format("The parameter with name '%s' is not an Values Provider",
                                                              parameterName),
                                                       INVALID_PARAMETER));

    try {
      return resolveValues(parameter, factoryModelProperty, parameterValueResolver, connectionSupplier, configurationSupplier);
    } catch (Exception e) {
      throw new ValueResolvingException(format("An error occurred trying to resolve the Values for parameter '%s' of component '%s'",
                                               parameterName, containerModel.getName()),
                                        UNKNOWN, e);
    }
  }

  private Set<Value> resolveValues(HasValuesProviderModel parameter, ValuesProviderFactoryModelProperty factoryModelProperty,
                                   ParameterValueResolver parameterValueResolver, Supplier<Object> connectionSupplier,
                                   Supplier<Object> configurationSupplier)
      throws NoSuchMethodException, InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException,
      InitialisationException, org.mule.runtime.module.extension.internal.runtime.ValueResolvingException,
      ValueResolvingException {

    ValuesProvider valueProvider =
        factoryModelProperty.createFactory(parameterValueResolver, connectionSupplier, configurationSupplier, muleContext)
            .createValueProvider();

    Set<Value> valueSet = valueProvider.resolve();

    return valueSet.stream()
        .map(option -> cloneAndEnrichMetadataKey(option, parameter.getValuesProviderModel().get().getValueParts()))
        .map(ValueBuilder::build)
        .collect(toSet());
  }

  /**
   * Given a parameter or parameter group name, this method will look for the correspondent {@link ParameterModel} or
   * {@link ParameterGroupModel}
   *
   * @param parameterName name of the parameter or parameter group to find
   * @return the correspondent parameter
   */
  private <P extends EnrichableModel & HasValuesProviderModel> Optional<P> getParameter(String parameterName) {
    Optional<ParameterModel> optionalParamModel = containerModel.getAllParameterModels()
        .stream()
        .filter(parameterModel -> parameterModel.getName().equals(parameterName))
        .findFirst();

    if (optionalParamModel.isPresent()) {
      return of((P) optionalParamModel.get());
    }

    Optional<ParameterGroupModel> optionalGroupModel = containerModel.getParameterGroupModels()
        .stream()
        .filter(parameterGroupModel -> parameterGroupModel.getName().equals(parameterName))
        .findFirst();

    if (optionalGroupModel.isPresent()) {
      return of((P) optionalGroupModel.get());
    }

    return empty();
  }

  /**
   * {@inheritDoc}
   */
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
