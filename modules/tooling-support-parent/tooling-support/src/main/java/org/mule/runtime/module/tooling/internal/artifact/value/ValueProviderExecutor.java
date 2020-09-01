/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.value;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;
import static org.mule.runtime.api.value.ResolvingFailure.Builder.newFailure;
import static org.mule.runtime.api.value.ValueResult.resultFrom;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.tooling.internal.artifact.params.ParameterExtractor.extractValue;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterGroupElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.runtime.module.extension.internal.ExtensionResolvingContext;
import org.mule.runtime.module.extension.internal.runtime.config.ResolverSetBasedParameterResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.extension.internal.value.ValueProviderMediator;
import org.mule.runtime.module.tooling.internal.artifact.context.LoggingResolvingContext;
import org.mule.runtime.module.tooling.internal.utils.ArtifactHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class ValueProviderExecutor {

  private final MuleContext muleContext;
  private final ConnectionManager connectionManager;
  private final ExpressionManager expressionManager;
  private final ReflectionCache reflectionCache;
  private final ArtifactHelper artifactHelper;

  public ValueProviderExecutor(MuleContext muleContext, ConnectionManager connectionManager,
                               ExpressionManager expressionManager, ReflectionCache reflectionCache,
                               ArtifactHelper artifactHelper) {
    this.muleContext = muleContext;
    this.connectionManager = connectionManager;
    this.expressionManager = expressionManager;
    this.reflectionCache = reflectionCache;
    this.artifactHelper = artifactHelper;
  }

  public ValueResult resolveValues(ParameterizedModel parameterizedModel,
                                   ParameterizedElementDeclaration parameterizedElementDeclaration, String providerName) {
    try {
      Optional<ConfigurationInstance> optionalConfigurationInstance =
          getConfigurationInstance(parameterizedModel, parameterizedElementDeclaration, providerName);

      ParameterValueResolver parameterValueResolver = parameterValueResolver(parameterizedElementDeclaration, parameterizedModel);
      ValueProviderMediator valueProviderMediator = createValueProviderMediator(parameterizedModel);

      LoggingResolvingContext context =
          new LoggingResolvingContext(new ExtensionResolvingContext(() -> optionalConfigurationInstance,
                                                                    connectionManager));
      ClassLoader extensionClassLoader = getClassLoader(artifactHelper.getExtensionModel(parameterizedElementDeclaration));
      try {
        return resultFrom(withContextClassLoader(extensionClassLoader, () -> valueProviderMediator.getValues(providerName,
                                                                                                             parameterValueResolver,
                                                                                                             connectionSupplier(context),
                                                                                                             configSupplier(context)),
                                                 ValueResolvingException.class,
                                                 e -> {
                                                   throw new ExecutorExceptionWrapper(e);
                                                 }));
      } finally {
        context.dispose();
      }
    } catch (ValueResolvingException e) {
      return resultFrom(newFailure(e).withFailureCode(e.getFailureCode()).build());
    } catch (ExecutorExceptionWrapper e) {
      return resultFrom(newFailure(e.getCause()).build());
    } catch (Exception e) {
      return resultFrom(newFailure(e).build());
    }
  }

  private Supplier<Object> connectionSupplier(LoggingResolvingContext context) {
    return (CheckedSupplier<Object>) () -> context.getConnection().orElse(null);
  }

  private Supplier<Object> configSupplier(LoggingResolvingContext context) {
    return (CheckedSupplier<Object>) () -> context.getConfig().orElse(null);
  }

  private Optional<ConfigurationInstance> getConfigurationInstance(ParameterizedModel parameterizedModel,
                                                                   ParameterizedElementDeclaration parameterizedElementDeclaration,
                                                                   String providerName)
      throws ValueResolvingException {
    Optional<String> optionalConfigRef = getConfigRef(parameterizedElementDeclaration);
    Optional<ConfigurationInstance> optionalConfigurationInstance = optionalConfigRef
        .map(configRef -> artifactHelper.getConfigurationInstance(configRef))
        .orElse(empty());

    if (optionalConfigRef.isPresent()) {
      Optional<ValueProviderModel> valueProviderModelOptional = getValueProviderModel(parameterizedModel, providerName);
      if (valueProviderModelOptional.isPresent() && valueProviderModelOptional.get().requiresConfiguration()
          && !optionalConfigurationInstance.isPresent()) {
        // Improves the error message when configuration is required and not present, as we do the resolve parameter with lazyInit in order
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
        .findFirst().flatMap(parameterModel -> parameterModel.getValueProviderModel());
  }

  private ValueProviderMediator createValueProviderMediator(ParameterizedModel parameterizedModel) {
    return new ValueProviderMediator(parameterizedModel,
                                     () -> muleContext,
                                     () -> reflectionCache);
  }

  private ParameterValueResolver parameterValueResolver(ParameterizedElementDeclaration parameterizedElementDeclaration,
                                                        ParameterizedModel parameterizedModel) {
    Map<String, Object> parametersMap = new HashMap<>();

    Map<String, ParameterGroupModel> parameterGroups =
        parameterizedModel.getParameterGroupModels().stream().collect(toMap(NamedObject::getName, identity()));

    for (ParameterGroupElementDeclaration parameterGroupElement : parameterizedElementDeclaration.getParameterGroups()) {
      final String parameterGroupName = parameterGroupElement.getName();
      final ParameterGroupModel parameterGroupModel = parameterGroups.get(parameterGroupName);
      if (parameterGroupModel == null) {
        throw new MuleRuntimeException(createStaticMessage("Could not find parameter group with name: '%s' in model",
                                                           parameterGroupName));
      }

      for (ParameterElementDeclaration parameterElement : parameterGroupElement.getParameters()) {
        final String parameterName = parameterElement.getName();
        final ParameterModel parameterModel = parameterGroupModel.getParameter(parameterName)
            .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find parameter with name: '%s' in parameter group: '%s'",
                                                                            parameterName, parameterGroupName)));
        parametersMap.put(parameterName,
                          extractValue(parameterElement.getValue(),
                                       artifactHelper.getParameterClass(parameterModel, parameterizedElementDeclaration)));
      }
    }

    try {
      final ResolverSet resolverSet =
          ParametersResolver.fromValues(parametersMap,
                                        muleContext,
                                        // Required parameters should not invalidate the resolution of resolving ValueProviders
                                        true,
                                        reflectionCache,
                                        expressionManager,
                                        parameterizedModel.getName())
              .getParametersAsResolverSet(parameterizedModel, muleContext);
      return new ResolverSetBasedParameterResolver(resolverSet, parameterizedModel, reflectionCache, expressionManager);
    } catch (ConfigurationException e) {
      throw new MuleRuntimeException(createStaticMessage("Error resolving parameters values from declaration"), e);
    }
  }

  private Optional<String> getConfigRef(ParameterizedElementDeclaration component) {
    if (component instanceof ComponentElementDeclaration) {
      return ofNullable(((ComponentElementDeclaration) component).getConfigRef());
    }
    return empty();
  }

  private class ExecutorExceptionWrapper extends MuleRuntimeException {

    public ExecutorExceptionWrapper(Throwable cause) {
      super(cause);
    }

  }

}
