/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.data.sample;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.setValueIntoField;
import static org.mule.sdk.api.data.sample.SampleDataException.CONNECTION_FAILURE;
import static org.mule.sdk.api.data.sample.SampleDataException.MISSING_REQUIRED_PARAMETERS;
import static org.mule.sdk.api.data.sample.SampleDataException.UNKNOWN;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.extension.internal.loader.java.property.InjectableParameterInfo;
import org.mule.runtime.module.extension.internal.loader.java.property.SampleDataProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.data.sample.SampleDataProvider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;

/**
 * Provides instances of {@link SampleDataProvider}
 *
 * @since 4.4.0
 */
public class SampleDataProviderFactory {

  private static final Logger LOGGER = getLogger(SampleDataProviderFactory.class);

  private final SampleDataProviderFactoryModelProperty factoryModelProperty;
  private final ParameterValueResolver parameterValueResolver;
  private final Supplier<Object> connectionSupplier;
  private final Supplier<Object> configurationSupplier;
  private final Field connectionField;
  private final Field configField;
  private final ReflectionCache reflectionCache;
  private final MuleContext muleContext;

  public SampleDataProviderFactory(SampleDataProviderFactoryModelProperty factoryModelProperty,
                                   ParameterValueResolver parameterValueResolver,
                                   Supplier<Object> connectionSupplier,
                                   Supplier<Object> configurationSupplier,
                                   Field connectionField,
                                   Field configField,
                                   ReflectionCache reflectionCache,
                                   MuleContext muleContext) {
    this.factoryModelProperty = factoryModelProperty;
    this.parameterValueResolver = parameterValueResolver;
    this.connectionSupplier = connectionSupplier;
    this.configurationSupplier = configurationSupplier;
    this.connectionField = connectionField;
    this.configField = configField;
    this.reflectionCache = reflectionCache;
    this.muleContext = muleContext;
  }

  <T, A> SampleDataProvider<T, A> createSampleDataProvider() throws SampleDataException {
    Class<? extends SampleDataProvider<T, A>> providerClass = factoryModelProperty.getSampleDataProviderClass();

    try {
      SampleDataProvider<T, A> resolver = instantiateClass(providerClass);
      initialiseIfNeeded(resolver, true, muleContext);

      injectProviderFields(resolver);

      if (factoryModelProperty.usesConnection()) {
        Object connection;
        try {
          connection = connectionSupplier.get();
        } catch (Exception e) {
          throw new SampleDataException("Failed to establish connection: " + e.getMessage(), CONNECTION_FAILURE, e);
        }
        if (connection == null) {
          throw new SampleDataException("The sample data provider requires a connection and none was provided",
                                        MISSING_REQUIRED_PARAMETERS);
        }
        setValueIntoField(resolver, connectionSupplier.get(), connectionField);
      }

      if (factoryModelProperty.usesConfig()) {
        Object config = configurationSupplier.get();
        if (config == null) {
          throw new SampleDataException("The sample data provider requires a configuration and none was provided",
                                        MISSING_REQUIRED_PARAMETERS);
        }
        setValueIntoField(resolver, configurationSupplier.get(), configField);
      }
      return resolver;
    } catch (SampleDataException e) {
      throw e;
    } catch (Exception e) {
      throw new SampleDataException("An error occurred trying to create a SampleDataProvider", UNKNOWN, e);
    }
  }

  private void injectProviderFields(SampleDataProvider resolver) throws SampleDataException {
    List<String> missingParameters = new ArrayList<>();
    for (InjectableParameterInfo injectableParam : factoryModelProperty.getInjectableParameters()) {
      Object parameterValue = null;
      String parameterName = injectableParam.getParameterName();
      try {
        parameterValue = parameterValueResolver.getParameterValue(parameterName);
      } catch (ValueResolvingException ignored) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("An error occurred while resolving parameter " + parameterName, ignored);
        }
      }

      if (parameterValue != null) {
        setValueIntoField(resolver, parameterValue, parameterName, reflectionCache);
      } else if (injectableParam.isRequired()) {
        missingParameters.add(parameterName);
      }
    }

    if (!missingParameters.isEmpty()) {
      throw new SampleDataException("Unable to retrieve Sample Data. There are missing required parameters for the resolution: "
          + missingParameters, MISSING_REQUIRED_PARAMETERS);
    }
  }
}
