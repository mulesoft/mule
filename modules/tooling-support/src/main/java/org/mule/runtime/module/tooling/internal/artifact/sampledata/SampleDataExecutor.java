/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.sampledata;

import static org.mule.runtime.api.sampledata.SampleDataFailure.Builder.newFailure;
import static org.mule.runtime.api.sampledata.SampleDataResult.resultFrom;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.sdk.api.data.sample.SampleDataException.NOT_SUPPORTED;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import static com.google.common.base.Throwables.throwIfInstanceOf;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.meta.model.data.sample.SampleDataProviderModel;
import org.mule.runtime.api.meta.model.parameter.ActingParameterModel;
import org.mule.runtime.api.sampledata.SampleDataFailure;
import org.mule.runtime.api.sampledata.SampleDataResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.data.sample.SampleDataService;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingException;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.tooling.internal.artifact.AbstractParameterResolverExecutor;
import org.mule.runtime.module.tooling.internal.artifact.ExecutorExceptionWrapper;
import org.mule.runtime.module.tooling.internal.artifact.params.ExpressionNotSupportedException;
import org.mule.runtime.module.tooling.internal.utils.ArtifactHelper;
import org.mule.sdk.api.data.sample.SampleDataException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleDataExecutor extends AbstractParameterResolverExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(SampleDataExecutor.class);

  private final SampleDataService sampleDataService;

  public SampleDataExecutor(MuleContext muleContext, ExpressionManager expressionManager,
                            SampleDataService sampleDataService, ReflectionCache reflectionCache, ArtifactHelper artifactHelper) {
    super(muleContext, expressionManager, reflectionCache, artifactHelper);
    this.sampleDataService = sampleDataService;
  }

  public SampleDataResult getSampleData(ComponentModel componentModel, ComponentElementDeclaration componentElementDeclaration) {
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Get sample data STARTED for component: {}", componentModel.getName());
      }
      String componentName = componentElementDeclaration.getName();

      if (!getSampleDataProviderModel(componentModel).isPresent()) {
        String message = format("Component %s does not support Sample Data", componentName);
        return resultFrom(newFailure().withMessage(message).withReason(message).withFailureCode(NOT_SUPPORTED).build());
      }

      ExtensionModel extensionModel = artifactHelper.getExtensionModel(componentElementDeclaration);
      String extensionName = extensionModel.getName();
      ClassLoader extensionClassLoader = getClassLoader(extensionModel);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Invoking connector's sample data  provider for component: {}", componentModel.getName());
      }
      return resultFrom(withContextClassLoader(extensionClassLoader, () -> sampleDataService.getSampleData(extensionName,
                                                                                                           componentName,
                                                                                                           parameterMapWithDefaults(componentElementDeclaration,
                                                                                                                                    componentModel),
                                                                                                           getConfigurationInstance(componentElementDeclaration)),
                                               SampleDataException.class, e -> {
                                                 throw new ExecutorExceptionWrapper(e);
                                               }));
    } catch (SampleDataException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(format("Get sample data has FAILED with code: %s for component: %s", e.getFailureCode(),
                           componentModel.getName()),
                    e);
      }
      return resultFrom(newFailure(e).withFailureCode(e.getFailureCode()).build());
    } catch (ExpressionNotSupportedException e) {
      return resultFrom(newFailure(new SampleDataException(e.getMessage(), INVALID_PARAMETER_VALUE))
          .withFailureCode(INVALID_PARAMETER_VALUE).build());
    } catch (ExecutorExceptionWrapper e) {
      Throwable cause = e.getCause();
      if (cause == null) {
        throw new MuleRuntimeException(e);
      }

      if (cause instanceof SampleDataException sampleDataException) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn(format("Get sample data has FAILED with code: %s for component: %s", sampleDataException.getFailureCode(),
                             componentModel.getName()),
                      e);
        }
        SampleDataFailure.Builder failureBuilder = newFailure(cause);
        failureBuilder.withFailureCode(sampleDataException.getFailureCode());
        return resultFrom(failureBuilder.build());
      }

      throwIfInstanceOf(e, MuleRuntimeException.class);
      throw new MuleRuntimeException(cause);
    } catch (Exception e) {
      throwIfInstanceOf(e, MuleRuntimeException.class);
      throw new MuleRuntimeException(e);
    } finally {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Get sample data FINISHED for component: {}", componentModel.getName());
      }
    }
  }

  private Map<String, Object> parameterMapWithDefaults(ParameterizedElementDeclaration componentElementDeclaration,
                                                       ComponentModel componentModel) {
    Map<String, Object> explicitParameterMaps = parametersMap(componentElementDeclaration, componentModel);
    if (componentModel instanceof HasOutputModel) {
      ((HasOutputModel) componentModel).getSampleDataProviderModel().ifPresent(model -> {
        // No need to identify the acting parameter is required or not, maybe be required but DSL on component
        // is optional with default so we need to include its default value.
        List<String> actingParameters = model.getParameters().stream()
            .map(ActingParameterModel::getName)
            .collect(toList());

        // Now we get the default values for those optional parameters from model that are marked as acting
        // parameters
        ParameterValueResolver parameterValueResolver =
            parameterValueResolver(componentElementDeclaration, componentModel);

        componentModel.getAllParameterModels().stream()
            .filter(p -> actingParameters.contains(p.getName()))
            .filter(p -> !explicitParameterMaps.containsKey(p.getName()))
            .filter(p -> !p.isRequired() && p.getDefaultValue() != null)
            .forEach(p -> {
              try {
                // Force to use parameterValueResolver with default values in order to convert from String to model field type
                explicitParameterMaps.put(p.getName(), parameterValueResolver.getParameterValue(p.getName()));
              } catch (ValueResolvingException e) {
                throw new MuleRuntimeException(e);
              }
            });
      });
    }
    return explicitParameterMaps;
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
