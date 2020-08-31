/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.sampledata;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;
import static org.mule.runtime.api.sampledata.SampleDataFailure.Builder.newFailure;
import static org.mule.runtime.api.sampledata.SampleDataResult.resultFrom;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.sdk.api.data.sample.SampleDataException.NOT_SUPPORTED;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.meta.model.data.sample.SampleDataProviderModel;
import org.mule.runtime.api.sampledata.SampleDataResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.ExtensionResolvingContext;
import org.mule.runtime.module.extension.internal.data.sample.MuleSampleDataService.ResolvingComponent;
import org.mule.runtime.module.extension.internal.data.sample.SampleDataProviderMediator;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.tooling.internal.artifact.AbstractParameterResolverExecutor;
import org.mule.runtime.module.tooling.internal.artifact.ExecutorExceptionWrapper;
import org.mule.runtime.module.tooling.internal.artifact.context.LoggingResolvingContext;
import org.mule.runtime.module.tooling.internal.utils.ArtifactHelper;
import org.mule.sdk.api.data.sample.SampleDataException;

import java.util.Optional;

public class SampleDataExecutor extends AbstractParameterResolverExecutor {

  private final ConnectionManager connectionManager;
  private final StreamingManager streamingManager;

  public SampleDataExecutor(MuleContext muleContext, ConnectionManager connectionManager, ExpressionManager expressionManager,
                            StreamingManager streamingManager, ReflectionCache reflectionCache, ArtifactHelper artifactHelper) {
    super(muleContext, expressionManager, reflectionCache, artifactHelper);
    this.connectionManager = connectionManager;
    this.streamingManager = streamingManager;
  }

  public SampleDataResult getSampleData(ComponentModel componentModel, ComponentElementDeclaration componentElementDeclaration) {
    try {
      String componentName = componentElementDeclaration.getName();

      if (!getSampleDataProviderModel(componentModel).isPresent()) {
        String message = format("Component %s does not support Sample Data", componentName);
        return resultFrom(newFailure().withMessage(message).withReason(message).withFailureCode(NOT_SUPPORTED).build());
      }

      ExtensionModel extensionModel = artifactHelper.getExtensionModel(componentElementDeclaration);
      String extensionName = extensionModel.getName();

      SampleDataProviderMediator mediator = new SampleDataProviderMediator(
                                                                           extensionModel,
                                                                           componentModel,
                                                                           new ResolvingComponent(extensionName, componentName),
                                                                           muleContext,
                                                                           new ReflectionCache(),
                                                                           streamingManager);

      Optional<ConfigurationInstance> optionalConfigurationInstance =
          getConfigurationInstance(componentModel, componentElementDeclaration);

      ParameterValueResolver parameterValueResolver = parameterValueResolver(componentElementDeclaration, componentModel);

      LoggingResolvingContext context =
          new LoggingResolvingContext(new ExtensionResolvingContext(() -> optionalConfigurationInstance,
                                                                    connectionManager));

      ClassLoader extensionClassLoader = getClassLoader(artifactHelper.getExtensionModel(componentElementDeclaration));
      try {
        return resultFrom(withContextClassLoader(extensionClassLoader, () -> mediator.getSampleData(parameterValueResolver,
                                                                                                    connectionSupplier(context),
                                                                                                    configSupplier(context)),
                                                 SampleDataException.class, e -> {
                                                   throw new ExecutorExceptionWrapper(e);
                                                 }));
      } finally {
        context.dispose();
      }
    } catch (Exception e) {
      return resultFrom(newFailure(e).build());
    }
  }

  private Optional<ConfigurationInstance> getConfigurationInstance(ComponentModel componentModel,
                                                                   ComponentElementDeclaration componentElementDeclaration)
      throws SampleDataException {
    Optional<String> optionalConfigRef = ofNullable(componentElementDeclaration.getConfigRef());
    Optional<ConfigurationInstance> optionalConfigurationInstance = optionalConfigRef
        .map(configRef -> artifactHelper.getConfigurationInstance(configRef))
        .orElse(empty());

    if (optionalConfigRef.isPresent()) {
      Optional<SampleDataProviderModel> valueProviderModelOptional = getSampleDataProviderModel(componentModel);
      if (valueProviderModelOptional.isPresent() && valueProviderModelOptional.get().requiresConfiguration()
          && !optionalConfigurationInstance.isPresent()) {
        throw new SampleDataException(format("The sample data provider requires a configuration but the one referenced by element declaration with name: '%s' is not present",
                                             optionalConfigRef.get()),
                                      COMPONENT_NOT_FOUND.getName());
      }
    }

    return optionalConfigurationInstance;
  }

  private Optional<SampleDataProviderModel> getSampleDataProviderModel(ComponentModel componentModel) {
    if (componentModel instanceof HasOutputModel) {
      return ((HasOutputModel) componentModel).getSampleDataProviderModel();
    }
    return empty();
  }

}
