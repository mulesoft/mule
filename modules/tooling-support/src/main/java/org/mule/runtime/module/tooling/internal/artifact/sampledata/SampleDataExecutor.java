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
import static org.mule.runtime.api.sampledata.SampleDataFailure.Builder.newFailure;
import static org.mule.runtime.api.sampledata.SampleDataResult.resultFrom;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.sdk.api.data.sample.SampleDataException.NOT_SUPPORTED;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.meta.model.data.sample.SampleDataProviderModel;
import org.mule.runtime.api.sampledata.SampleDataFailure;
import org.mule.runtime.api.sampledata.SampleDataResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.data.sample.SampleDataService;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.tooling.internal.artifact.AbstractParameterResolverExecutor;
import org.mule.runtime.module.tooling.internal.artifact.ExecutorExceptionWrapper;
import org.mule.runtime.module.tooling.internal.artifact.params.ExpressionNotSupportedException;
import org.mule.runtime.module.tooling.internal.utils.ArtifactHelper;
import org.mule.sdk.api.data.sample.SampleDataException;

import java.util.Optional;
import java.util.function.Supplier;

public class SampleDataExecutor extends AbstractParameterResolverExecutor {

  private final ConnectionManager connectionManager;
  private final SampleDataService sampleDataService;

  public SampleDataExecutor(MuleContext muleContext, ConnectionManager connectionManager, ExpressionManager expressionManager,
                            SampleDataService sampleDataService, ReflectionCache reflectionCache, ArtifactHelper artifactHelper) {
    super(muleContext, expressionManager, reflectionCache, artifactHelper);
    this.connectionManager = connectionManager;
    this.sampleDataService = sampleDataService;
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


      ClassLoader extensionClassLoader = getClassLoader(artifactHelper.getExtensionModel(componentElementDeclaration));
      return resultFrom(withContextClassLoader(extensionClassLoader, () -> sampleDataService.getSampleData(extensionName,
                                                                                                           componentName,
                                                                                                           parametersMap(componentElementDeclaration,
                                                                                                                         componentModel),
                                                                                                           getConfigurationInstance(componentElementDeclaration)),
                                               SampleDataException.class, e -> {
                                                 throw new ExecutorExceptionWrapper(e);
                                               }));
    } catch (SampleDataException e) {
      return resultFrom(newFailure(e).withFailureCode(e.getFailureCode()).build());
    } catch (ExpressionNotSupportedException e) {
      return resultFrom(newFailure(new SampleDataException(e.getMessage(), INVALID_PARAMETER_VALUE))
          .withFailureCode(INVALID_PARAMETER_VALUE).build());
    } catch (ExecutorExceptionWrapper e) {
      Throwable cause = e.getCause();
      SampleDataFailure.Builder failureBuilder = newFailure(cause);
      if (cause instanceof SampleDataException) {
        failureBuilder.withFailureCode(((SampleDataException) cause).getFailureCode());
      }
      return resultFrom(failureBuilder.build());
    } catch (Exception e) {
      return resultFrom(newFailure(e).build());
    }
  }

  private Supplier<Optional<ConfigurationInstance>> getConfigurationInstance(ComponentElementDeclaration componentElementDeclaration) {
    return () -> {
      Optional<String> optionalConfigRef = ofNullable(componentElementDeclaration.getConfigRef());
      return optionalConfigRef
          .map(configRef -> artifactHelper.getConfigurationInstance(configRef))
          .orElse(empty());
    };
  }

  private Optional<SampleDataProviderModel> getSampleDataProviderModel(ComponentModel componentModel) {
    if (componentModel instanceof HasOutputModel) {
      return ((HasOutputModel) componentModel).getSampleDataProviderModel();
    }
    return empty();
  }

}
