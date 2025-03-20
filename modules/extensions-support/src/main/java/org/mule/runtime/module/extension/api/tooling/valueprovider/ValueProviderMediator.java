/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.tooling.valueprovider;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.sdk.api.values.ValueProvider;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Resolves a parameter's {@link Value values} by coordinating the several moving parts that are affected by the {@link Value}
 * fetching process, so that such pieces can remain decoupled.
 *
 * @since 4.8
 */
@NoImplement
public interface ValueProviderMediator {

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
  Set<Value> getValues(String parameterName, ParameterValueResolver parameterValueResolver, String targetSelector,
                       Supplier<Object> connectionSupplier, Supplier<Object> configurationSupplier)
      throws ValueResolvingException;

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
  Set<Value> getValues(String parameterName, ParameterValueResolver parameterValueResolver,
                       Supplier<Object> connectionSupplier, Supplier<Object> configurationSupplier,
                       ConnectionProvider connectionProvider)
      throws ValueResolvingException;

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
  Set<Value> getValues(String parameterName, ParameterValueResolver parameterValueResolver, String targetSelector,
                       Supplier<Object> connectionSupplier, Supplier<Object> configurationSupplier,
                       ConnectionProvider connectionProvider)
      throws ValueResolvingException;

}
