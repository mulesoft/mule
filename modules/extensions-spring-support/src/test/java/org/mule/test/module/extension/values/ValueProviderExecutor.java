/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.values;

import static org.mule.runtime.api.value.ResolvingFailure.Builder.newFailure;
import static org.mule.runtime.api.value.ValueResult.resultFrom;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import static java.lang.String.format;

import static com.google.common.base.Throwables.propagateIfPossible;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.value.ResolvingFailure;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.extension.api.property.ClassLoaderModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.runtime.module.extension.api.runtime.config.ExtensionDesignTimeResolversFactory;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.api.tooling.valueprovider.ValueProviderMediator;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;


import org.slf4j.Logger;


/**
 * Resolve Values for the given parameters
 *
 * @since 1.0
 */
public class ValueProviderExecutor {

  private static final Logger LOGGER = getLogger(ValueProviderExecutor.class);
  public static final String INVALID_PARAMETER_VALUE = "INVALID_PARAMETER_VALUE";

  private final ExtensionDesignTimeResolversFactory extensionDesignTimeResolversFactory;
  private final ValueProviderMediator valueProviderMediator;
  private final ParameterizedModel parameterizedModel;

  public ValueProviderExecutor(ExtensionDesignTimeResolversFactory extensionDesignTimeResolversFactory,
                               ParameterizedModel parameterizedModel) {
    this.extensionDesignTimeResolversFactory = extensionDesignTimeResolversFactory;
    this.parameterizedModel = parameterizedModel;
    this.valueProviderMediator = createValueProviderMediator(parameterizedModel);
  }

  public ValueResult resolveValues(ExtensionModel extensionModel,
                                   String providerName,
                                   ComponentParameterization actingParameter,
                                   Optional<ConfigurationProvider> configurationProvider,
                                   String targetSelector) {

    return resolveValues(extensionModel,
                         providerName,
                         actingParameter,
                         configurationProvider,
                         (mediator, resolver, selector, connection, config, provider) -> mediator.getValues(providerName,
                                                                                                            resolver,
                                                                                                            selector,
                                                                                                            connection,
                                                                                                            config,
                                                                                                            provider),
                         targetSelector,
                         "");
  }

  private ValueResult resolveValues(ExtensionModel extensionModel,
                                    String providerName,
                                    ComponentParameterization actingParameter,
                                    Optional<ConfigurationProvider> configurationProvider,
                                    ValueProviderFunction valueProviderFunction,
                                    String targetSelector,
                                    String loggingSuffix) {
    try {
      if (parameterizedModel != actingParameter.getModel()) {
        return resultFrom(newFailure(new ValueResolvingException("The parameter model from the acting parameters "
            + "is different from the one provided in the session", INVALID_PARAMETER_VALUE))
                .withFailureCode(INVALID_PARAMETER_VALUE).build());
      }
      LOGGER.debug("Resolve value provider: {} STARTED for component: {} {}", providerName, parameterizedModel.getName(),
                   loggingSuffix);
      ClassLoader extensionClassLoader = getClassLoader(extensionModel);

      LOGGER.debug("Invoking value provider: {} for component: {} {}",
                   providerName,
                   parameterizedModel.getName(),
                   loggingSuffix);
      return withContextClassLoader(extensionClassLoader, () -> {
        ParameterValueResolver parameterValueResolver =
            extensionDesignTimeResolversFactory.createParameterValueResolver(actingParameter, parameterizedModel);

        var connectionProvider = getConnectionProvider(configurationProvider);
        return resultFrom(valueProviderFunction.apply(valueProviderMediator,
                                                      parameterValueResolver,
                                                      targetSelector,
                                                      connectionSupplier(connectionProvider),
                                                      configurationProvider.map(this::configSupplier)
                                                          .orElse(() -> null),
                                                      connectionProvider.orElse(null)));

      }, ValueResolvingException.class, e -> {
        throw new MuleRuntimeException(e);
      });

    } catch (ValueResolvingException e) {
      LOGGER.warn(format("Resolve value provider has FAILED with code: %s for component: %s %s",
                         e.getFailureCode(),
                         actingParameter.getModel().getName(),
                         loggingSuffix),
                  e);

      return resultFrom(newFailure(e).withFailureCode(e.getFailureCode()).build());
      // } catch (ExpressionNotSupportedException e) {
      // return resultFrom(newFailure(new ValueResolvingException(e.getMessage(), INVALID_PARAMETER_VALUE))
      // .withFailureCode(INVALID_PARAMETER_VALUE).build());
    } catch (MuleRuntimeException e) {
      Throwable cause = e.getCause();
      if (cause instanceof ValueResolvingException) {
        ValueResolvingException valueResolvingException = (ValueResolvingException) cause;
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn(format("Resolve value provider has FAILED with code: %s for component: %s %s",
                             valueResolvingException.getFailureCode(),
                             actingParameter.getModel().getName(),
                             loggingSuffix),
                      cause);
        }
        ResolvingFailure.Builder failureBuilder = newFailure(cause);
        failureBuilder.withFailureCode(valueResolvingException.getFailureCode());
        return resultFrom(failureBuilder.build());
      }
      propagateIfPossible(cause, MuleRuntimeException.class);
      throw new MuleRuntimeException(cause);
    } catch (Exception e) {
      propagateIfPossible(e, MuleRuntimeException.class);
      throw new MuleRuntimeException(e);
    } finally {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Resolve value provider: {} FINISHED for component: {} {}", providerName,
                     actingParameter.getModel()
                         .getName(),
                     loggingSuffix);
      }
    }
  }

  private Supplier<Object> configSupplier(ConfigurationProvider configurationProvider) {
    return () -> configurationProvider.get(null).getValue();
  }

  private <C> Optional<ConnectionProvider<C>> getConnectionProvider(Optional<ConfigurationProvider> configurationProvider) {
    return configurationProvider.flatMap(cp -> cp.get(null).getConnectionProvider());
  }

  private <C> Supplier<C> connectionSupplier(Optional<ConnectionProvider<C>> connectionProvider) {
    return () -> (C) connectionProvider
        .map(cp -> {
          try {
            return cp.connect();
          } catch (ConnectionException e) {
            //
            throw new MuleRuntimeException(e);
          }
        }).orElse((C) null);
  }

  public static ClassLoader getClassLoader(ExtensionModel extensionModel) {
    return extensionModel.getModelProperty(ClassLoaderModelProperty.class).map(ClassLoaderModelProperty::getClassLoader)
        .orElse(Thread.currentThread().getContextClassLoader());
  }

  private ValueProviderMediator createValueProviderMediator(ParameterizedModel parameterizedModel) {
    return extensionDesignTimeResolversFactory.createValueProviderMediator(parameterizedModel);
  }

  @FunctionalInterface
  private interface ValueProviderFunction {

    Set<Value> apply(ValueProviderMediator valueProviderMediator, ParameterValueResolver parameterValueResolver,
                     String targetSelector,
                     Supplier<Object> connectionSupplier, Supplier<Object> configSupplier, ConnectionProvider connectionProvider)
        throws ValueResolvingException;
  }
}
