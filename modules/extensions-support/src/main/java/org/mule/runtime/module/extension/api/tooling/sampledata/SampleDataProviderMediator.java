/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.tooling.sampledata;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.data.sample.SampleDataProvider;

import java.util.function.Supplier;

/**
 * Coordinates all the moving parts necessary to provision and execute a {@link SampleDataProvider}, handling possible errors and
 * transforming the output into a {@link Message}
 *
 * @since 4.8
 */
@NoImplement
public interface SampleDataProviderMediator {

  /**
   * Resolves the sample data
   *
   * @param parameterValueResolver parameter resolver required if the associated {@link SampleDataProvider} requires the value of
   *                               parameters from the same parameter container.
   * @param connectionSupplier     supplier of connection instances related to the container and used, if necessary, by the
   *                               {@link SampleDataProvider}
   * @param configurationSupplier  supplier of configuration instance related to the container and used, if necessary, by the
   *                               {@link SampleDataProvider}
   * @return a {@link Message} carrying the sample data
   * @throws SampleDataException if an error occurs resolving the sample data
   */
  Message getSampleData(ParameterValueResolver parameterValueResolver,
                        Supplier<Object> connectionSupplier,
                        Supplier<Object> configurationSupplier)
      throws SampleDataException;

  /**
   * Resolves the sample data
   *
   * @param parameterValueResolver     parameter resolver required if the associated {@link SampleDataProvider} requires the value
   *                                   of parameters from the same parameter container.
   * @param connectionSupplier         supplier of connection instances related to the container and used, if necessary, by the
   *                                   {@link SampleDataProvider}
   * @param configurationSupplier      supplier of configuration instance related to the container and used, if necessary, by the
   *                                   {@link SampleDataProvider}
   * @param connectionProviderSupplier the connection provider in charge of providing the connection given by the connection
   *                                   supplier.
   *
   * @return a {@link Message} carrying the sample data
   * @throws SampleDataException if an error occurs resolving the sample data
   */
  Message getSampleData(ParameterValueResolver parameterValueResolver,
                        Supplier<Object> connectionSupplier,
                        Supplier<Object> configurationSupplier,
                        Supplier<ConnectionProvider> connectionProviderSupplier)
      throws SampleDataException;

}
