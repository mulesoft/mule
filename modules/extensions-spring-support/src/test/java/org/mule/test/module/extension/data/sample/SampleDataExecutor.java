/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.data.sample;

import static org.mule.runtime.api.sampledata.SampleDataFailure.Builder.newFailure;
import static org.mule.runtime.api.sampledata.SampleDataResult.resultFrom;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;
import static org.mule.sdk.api.data.sample.SampleDataException.NOT_SUPPORTED;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import static com.google.common.base.Throwables.propagateIfPossible;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.meta.model.parameter.ActingParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.api.sampledata.SampleDataFailure;
import org.mule.runtime.api.sampledata.SampleDataResult;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.property.ClassLoaderModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.api.runtime.config.ExtensionDesignTimeResolversFactory;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.api.tooling.sampledata.SampleDataProviderMediator;
import org.mule.runtime.module.extension.internal.runtime.client.NullComponent;
import org.mule.sdk.api.data.sample.SampleDataException;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolve Sample Data for the given parameters
 *
 * @since 1.0
 */
public class SampleDataExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(SampleDataExecutor.class);
  private final ExtensionDesignTimeResolversFactory extensionDesignTimeResolversFactory;

  public SampleDataExecutor(ExtensionDesignTimeResolversFactory extensionDesignTimeResolversFactory) {
    this.extensionDesignTimeResolversFactory = extensionDesignTimeResolversFactory;
  }

  public SampleDataResult getSampleData(ExtensionModel extensionModel,
                                        ComponentParameterization parameters,
                                        Optional<ConfigurationProvider> configurationProvider) {
    try {
      ClassLoader extensionClassLoader = getClassLoader(extensionModel);
      return withContextClassLoader(extensionClassLoader, () -> {
        if (!(parameters.getModel() instanceof ComponentModel && parameters.getModel() instanceof HasOutputModel)) {
          String message = format("Component '%s' does not support Sample Data", parameters.getModel().getName());
          return resultFrom(newFailure().withMessage(message).withReason(message).withFailureCode(NOT_SUPPORTED).build());
        }

        ComponentModel componentModel = (ComponentModel) parameters.getModel();
        ComponentParameterization paramWithDefaultValues = getParametersWithDefaultValues(componentModel, parameters);

        ParameterValueResolver parameterValueResolver =
            extensionDesignTimeResolversFactory.createParameterValueResolver(paramWithDefaultValues, componentModel);

        SampleDataProviderMediator sampleDataProviderMediator =
            extensionDesignTimeResolversFactory.createSampleDataProviderMediator(extensionModel,
                                                                                 componentModel,
                                                                                 new NullComponent(extensionModel.getName(),
                                                                                                   componentModel.getName()),
                                                                                 null);

        return resultFrom(sampleDataProviderMediator.getSampleData(parameterValueResolver,
                                                                   configurationProvider.map(this::connectionSupplier)
                                                                       .orElse(() -> null),
                                                                   configurationProvider.map(this::configSupplier)
                                                                       .orElse(() -> null),
                                                                   () -> null));
      }, SampleDataException.class, e -> {
        throw new MuleRuntimeException(e);
      });
    } catch (SampleDataException e) {
      LOGGER.warn(format("Get sample data has FAILED with code: %s for component: %s", e.getFailureCode(),
                         parameters.getModel().getName()),
                  e);
      return resultFrom(newFailure(e).withFailureCode(e.getFailureCode()).build());
    } catch (MuleRuntimeException e) {
      Throwable cause = e.getCause();
      if (cause instanceof SampleDataException sampleDataException) {
        LOGGER.warn(format("Get sample data has FAILED with code: %s for component: %s", sampleDataException.getFailureCode(),
                           parameters.getModel().getName()),
                    e);
        SampleDataFailure.Builder failureBuilder = newFailure(cause);
        failureBuilder.withFailureCode(sampleDataException.getFailureCode());
        return resultFrom(failureBuilder.build());
      }
      propagateIfPossible(cause, MuleRuntimeException.class);
      throw new MuleRuntimeException(cause);
    } catch (Exception e) {
      propagateIfPossible(e, MuleRuntimeException.class);
      throw new MuleRuntimeException(e);
    }
  }

  private ComponentParameterization getParametersWithDefaultValues(ComponentModel componentModel,
                                                                   ComponentParameterization parameters) {
    return ((HasOutputModel) componentModel).getSampleDataProviderModel().map(model -> {
      // No need to identify the acting parameter is required or not, maybe be required but DSL on component
      // is optional with default so we need to include its default value.
      List<String> actingParameters = model.getParameters().stream()
          .map(ActingParameterModel::getName)
          .collect(toList());

      List<Pair<ParameterGroupModel, ParameterModel>> defaultValues = componentModel.getParameterGroupModels().stream()
          .flatMap(group -> group.getParameterModels().stream()
              .filter(p -> actingParameters.contains(p.getName()) && parameters.getParameter(group.getName(), p.getName()) == null
                  && !p.isRequired() && p.getDefaultValue() != null)
              .map(p -> new Pair<>(group, p)))
          .collect(toList());

      if (defaultValues.isEmpty()) {
        return parameters;
      }

      ComponentParameterization.Builder parametersWithDefaultValues = ComponentParameterization.builder(parameters.getModel());

      parameters.forEachParameter((group, param, value) -> parametersWithDefaultValues.withParameter(param.getName(), value));

      defaultValues.forEach(value -> parametersWithDefaultValues.withParameter(value.getFirst().getName(),
                                                                               value.getSecond().getName(),
                                                                               value.getSecond().getDefaultValue()));

      return parametersWithDefaultValues.build();
    }).orElse(parameters);
  }

  private Supplier<Object> configSupplier(ConfigurationProvider configurationProvider) {
    return () -> configurationProvider.get(null).getValue();
  }

  private <C> Supplier<C> connectionSupplier(ConfigurationProvider configurationProvider) {
    return () -> (C) configurationProvider.get(null).getConnectionProvider()
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

  private static class NullComponent extends AbstractComponent {

    private final ComponentLocation location;

    public NullComponent(String extensionName, String componentName) {
      location = from(extensionName + "/" + componentName);
    }

    @Override
    public ComponentLocation getLocation() {
      return location;
    }

    @Override
    public Location getRootContainerLocation() {
      return Location.builder().globalName(location.getLocation()).build();
    }
  }
}
