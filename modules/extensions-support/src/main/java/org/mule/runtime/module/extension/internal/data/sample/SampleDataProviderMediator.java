/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.data.sample;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.core.internal.util.rx.ImmediateScheduler.IMMEDIATE_SCHEDULER;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.withRefreshToken;
import static org.mule.sdk.api.data.sample.SampleDataException.NOT_SUPPORTED;
import static org.mule.sdk.api.data.sample.SampleDataException.UNKNOWN;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.NullCursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.loader.java.property.SampleDataProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.result.ReturnDelegate;
import org.mule.runtime.module.extension.internal.runtime.result.ValueReturnDelegate;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.data.sample.SampleDataProvider;
import org.mule.sdk.api.runtime.operation.Result;

import java.util.function.Supplier;

/**
 * Coordinates all the moving parts necessary to provision and execute a {@link SampleDataProvider}, handling possible errors and
 * transforming the output into a {@link Message}
 *
 * @since 4.4.0
 */
public class SampleDataProviderMediator {

  private final ExtensionModel extensionModel;
  private final ComponentModel componentModel;
  private final Component component;
  private final MuleContext muleContext;
  private final ReflectionCache reflectionCache;
  private final StreamingManager streamingManager;
  private final SampleDataProviderFactoryModelProperty sampleDataProperty;
  private final ReturnDelegate returnDelegate;
  private final CursorProviderFactory cursorProviderFactory = new NullCursorProviderFactory();

  /**
   * Creates a new instance of the mediator
   *
   * @param componentModel container model which is a {@link ParameterizedModel} and {@link EnrichableModel}
   * @param muleContext    context to be able to initialize {@link SampleDataProvider} if necessary
   */
  public SampleDataProviderMediator(ExtensionModel extensionModel,
                                    ComponentModel componentModel,
                                    Component component,
                                    MuleContext muleContext,
                                    ReflectionCache reflectionCache,
                                    StreamingManager streamingManager) {
    this.extensionModel = extensionModel;
    this.componentModel = componentModel;
    this.component = component;
    this.muleContext = muleContext;
    this.reflectionCache = reflectionCache;
    this.streamingManager = streamingManager;
    sampleDataProperty = componentModel.getModelProperty(SampleDataProviderFactoryModelProperty.class).orElse(null);
    returnDelegate = new ValueReturnDelegate(componentModel, cursorProviderFactory, muleContext);
  }

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
  public Message getSampleData(ParameterValueResolver parameterValueResolver,
                               Supplier<Object> connectionSupplier,
                               Supplier<Object> configurationSupplier)
      throws SampleDataException {
    return getSampleData(parameterValueResolver, connectionSupplier, configurationSupplier, () -> null);
  }

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
  public Message getSampleData(ParameterValueResolver parameterValueResolver,
                               Supplier<Object> connectionSupplier,
                               Supplier<Object> configurationSupplier,
                               Supplier<ConnectionProvider> connectionProviderSupplier)
      throws SampleDataException {
    if (sampleDataProperty == null) {
      throw new SampleDataException(
                                    format("Component '%s' does not support Sample Data resolution", componentModel.getName()),
                                    NOT_SUPPORTED);
    }

    try {
      SampleDataProviderFactory factory = sampleDataProperty.createFactory(
                                                                           parameterValueResolver,
                                                                           connectionSupplier,
                                                                           configurationSupplier,
                                                                           reflectionCache,
                                                                           muleContext,
                                                                           componentModel);

      SampleDataProvider provider = factory.createSampleDataProvider();
      Result result = withRefreshToken(connectionProviderSupplier, () -> provider.getSample());

      return returnDelegate.asReturnValue(result, createExecutionContext(configurationSupplier)).getMessage();
    } catch (SampleDataException e) {
      throw e;
    } catch (Exception e) {
      throw new SampleDataException(format("An error occurred trying to obtain Sample Data for component '%s'. Cause: %s",
                                           componentModel.getName(), e.getMessage()),
                                    UNKNOWN, e);
    }
  }

  private ExecutionContextAdapter createExecutionContext(Supplier<Object> configurationSupplier) {
    return new DefaultExecutionContext(extensionModel,
                                       ofNullable(configurationSupplier.get()),
                                       emptyMap(),
                                       componentModel,
                                       getNullEvent(muleContext),
                                       cursorProviderFactory,
                                       streamingManager,
                                       component,
                                       new NoRetryPolicyTemplate(),
                                       IMMEDIATE_SCHEDULER,
                                       empty(),
                                       muleContext);
  }
}
