/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.value;

import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;
import static org.mule.runtime.api.value.ResolvingFailure.Builder.newFailure;
import static org.mule.runtime.api.value.ValueResult.resultFrom;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import static com.google.common.base.Throwables.propagateIfPossible;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.value.ResolvingFailure;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.api.tooling.valueprovider.ValueProviderMediator;
import org.mule.runtime.module.extension.internal.ExtensionResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.extension.internal.value.DefaultValueProviderMediator;
import org.mule.runtime.module.tooling.internal.artifact.AbstractParameterResolverExecutor;
import org.mule.runtime.module.tooling.internal.artifact.ExecutorExceptionWrapper;
import org.mule.runtime.module.tooling.internal.artifact.params.ExpressionNotSupportedException;
import org.mule.runtime.module.tooling.internal.artifact.sampledata.SampleDataExecutor;
import org.mule.runtime.module.tooling.internal.utils.ArtifactHelper;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueProviderExecutor extends AbstractParameterResolverExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(SampleDataExecutor.class);

  private final ConnectionManager connectionManager;

  public ValueProviderExecutor(MuleContext muleContext, ConnectionManager connectionManager,
                               ExtendedExpressionManager expressionManager, ReflectionCache reflectionCache,
                               ArtifactHelper artifactHelper) {
    super(muleContext, expressionManager, reflectionCache, artifactHelper);
    this.connectionManager = connectionManager;
  }

  public ValueResult resolveValues(ParameterizedModel parameterizedModel,
                                   ParameterizedElementDeclaration parameterizedElementDeclaration,
                                   String providerName) {

    return resolveValues(
                         parameterizedModel,
                         parameterizedElementDeclaration,
                         providerName,
                         (mediator, resolver, context) -> mediator.getValues(providerName,
                                                                             resolver,
                                                                             connectionSupplier(context),
                                                                             configSupplier(context),
                                                                             context.getConnectionProvider().orElse(null)),
                         "");

  }

  public ValueResult resolveFieldValues(ParameterizedModel parameterizedModel,
                                        ParameterizedElementDeclaration parameterizedElementDeclaration,
                                        String providerName,
                                        String targetSelector) {
    return resolveValues(
                         parameterizedModel,
                         parameterizedElementDeclaration,
                         providerName,
                         (mediator, resolver, context) -> mediator.getValues(providerName,
                                                                             resolver,
                                                                             targetSelector,
                                                                             connectionSupplier(context),
                                                                             configSupplier(context),
                                                                             context.getConnectionProvider().orElse(null)),
                         " with targetSelector: " + targetSelector);

  }

  private ValueResult resolveValues(ParameterizedModel parameterizedModel,
                                    ParameterizedElementDeclaration parameterizedElementDeclaration,
                                    String providerName,
                                    ValueProviderFunction valueProviderFunction,
                                    String loggingSuffix) {
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Resolve value provider: {} STARTED for component: {} {}", providerName, parameterizedModel.getName(),
                     loggingSuffix);
      }
      Optional<ConfigurationInstance> optionalConfigurationInstance =
          getConfigurationInstance(parameterizedModel, parameterizedElementDeclaration, providerName);

      ValueProviderMediator valueProviderMediator = createValueProviderMediator(parameterizedModel);

      ExtensionResolvingContext context = new ExtensionResolvingContext(() -> optionalConfigurationInstance,
                                                                        connectionManager);
      ClassLoader extensionClassLoader = getClassLoader(artifactHelper.getExtensionModel(parameterizedElementDeclaration));
      try {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Invoking connector's value provider: {} for component: {} {}",
                       providerName,
                       parameterizedModel.getName(),
                       loggingSuffix);
        }
        return resultFrom(
                          withContextClassLoader(
                                                 extensionClassLoader,
                                                 () -> valueProviderFunction.apply(valueProviderMediator,
                                                                                   parameterValueResolver(parameterizedElementDeclaration,
                                                                                                          parameterizedModel),
                                                                                   context),
                                                 ValueResolvingException.class,
                                                 e -> {
                                                   throw new ExecutorExceptionWrapper(e);
                                                 }));
      } finally {
        context.dispose();
      }
    } catch (ValueResolvingException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(format("Resolve value provider has FAILED with code: %s for component: %s %s",
                           e.getFailureCode(),
                           parameterizedModel.getName(),
                           loggingSuffix),
                    e);
      }
      return resultFrom(newFailure(e).withFailureCode(e.getFailureCode()).build());
    } catch (ExpressionNotSupportedException e) {
      return resultFrom(newFailure(new ValueResolvingException(e.getMessage(), INVALID_PARAMETER_VALUE))
          .withFailureCode(INVALID_PARAMETER_VALUE).build());
    } catch (ExecutorExceptionWrapper e) {
      Throwable cause = e.getCause();
      if (cause instanceof ValueResolvingException valueResolvingException) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn(format("Resolve value provider has FAILED with code: %s for component: %s %s",
                             valueResolvingException.getFailureCode(),
                             parameterizedModel.getName(),
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
        LOGGER.debug("Resolve value provider: {} FINISHED for component: {} {}", providerName, parameterizedModel.getName(),
                     loggingSuffix);
      }

    }
  }

  private Supplier<Object> connectionSupplier(ExtensionResolvingContext context) {
    return (CheckedSupplier<Object>) () -> context.getConnection().orElse(null);
  }

  private Supplier<Object> configSupplier(ExtensionResolvingContext context) {
    return (CheckedSupplier<Object>) () -> context.getConfig().orElse(null);
  }

  private Optional<ConfigurationInstance> getConfigurationInstance(ParameterizedModel parameterizedModel,
                                                                   ParameterizedElementDeclaration parameterizedElementDeclaration,
                                                                   String providerName)
      throws ValueResolvingException {
    Optional<String> optionalConfigRef = getConfigRef(parameterizedElementDeclaration);
    Optional<ConfigurationInstance> optionalConfigurationInstance =
        optionalConfigRef.flatMap(artifactHelper::getConfigurationInstance);

    if (optionalConfigRef.isPresent()) {
      Optional<ValueProviderModel> valueProviderModelOptional = getValueProviderModel(parameterizedModel, providerName);
      if (valueProviderModelOptional.isPresent() && valueProviderModelOptional.get().requiresConfiguration()
          && !optionalConfigurationInstance.isPresent()) {
        // Improves the error message when configuration is required and not present, as we do the resolve parameter with lazyInit
        // in order
        // to avoid getting an error when a required parameter from model is not defined for resolving the value provider.
        throw new ValueResolvingException(format("The provider requires a configuration but the one referenced by element declaration with name: '%s' is not present",
                                                 optionalConfigRef.get()),
                                          COMPONENT_NOT_FOUND.getName());
      }
    }

    return optionalConfigurationInstance;
  }

  private Optional<ValueProviderModel> getValueProviderModel(ParameterizedModel parameterizedModel, String providerName) {
    return parameterizedModel.getAllParameterModels().stream()
        .filter(parameterModel -> parameterModel.getValueProviderModel()
            .map(vpm -> vpm.getProviderName().equals(providerName)).orElse(false))
        .findFirst().flatMap(ParameterModel::getValueProviderModel);
  }

  private DefaultValueProviderMediator createValueProviderMediator(ParameterizedModel parameterizedModel) {
    return new DefaultValueProviderMediator(parameterizedModel,
                                            () -> reflectionCache,
                                            () -> expressionManager,
                                            muleContext::getInjector);
  }

  private Optional<String> getConfigRef(ParameterizedElementDeclaration component) {
    if (component instanceof ComponentElementDeclaration) {
      return ofNullable(((ComponentElementDeclaration) component).getConfigRef());
    }
    return empty();
  }

  @FunctionalInterface
  private interface ValueProviderFunction {

    Set<Value> apply(ValueProviderMediator valueProviderMediator, ParameterValueResolver parameterValueResolver,
                     ExtensionResolvingContext extensionResolvingContext)
        throws ValueResolvingException;

  }

}
