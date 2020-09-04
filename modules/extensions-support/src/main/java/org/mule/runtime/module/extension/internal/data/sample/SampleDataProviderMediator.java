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
import static org.mule.runtime.core.internal.management.stats.NoOpCursorComponentDecoratorFactory.NO_OP_INSTANCE;
import static org.mule.runtime.core.internal.util.rx.ImmediateScheduler.IMMEDIATE_SCHEDULER;
import static org.mule.sdk.api.data.sample.SampleDataException.NOT_SUPPORTED;
import static org.mule.sdk.api.data.sample.SampleDataException.UNKNOWN;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.NullCursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;
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

import java.util.Set;
import java.util.function.Supplier;

public class SampleDataProviderMediator {

  private final ExtensionModel extensionModel;
  private final ComponentModel componentModel;
  private final Component component;
  private final Supplier<MuleContext> muleContext;
  private final Supplier<ReflectionCache> reflectionCache;
  private final Supplier<Object> nullSupplier = () -> null;
  private final Supplier<StreamingManager> streamingManager;
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
                                    Supplier<MuleContext> muleContext,
                                    Supplier<ReflectionCache> reflectionCache,
                                    Supplier<StreamingManager> streamingManager) {
    this.extensionModel = extensionModel;
    this.componentModel = componentModel;
    this.component = component;
    this.muleContext = muleContext;
    this.reflectionCache = reflectionCache;
    this.streamingManager = streamingManager;
    sampleDataProperty = componentModel.getModelProperty(SampleDataProviderFactoryModelProperty.class).orElse(null);
    returnDelegate = new ValueReturnDelegate(componentModel, NO_OP_INSTANCE, cursorProviderFactory, muleContext.get());
  }

  /**
   * Given the name of a parameter or parameter group, and if the parameter supports it, this will try to resolve
   * the {@link Value values} for the parameter.
   *
   * @param parameterValueResolver parameter resolver required if the associated {@link ValueProvider} requires
   *                               the value of parameters from the same parameter container.
   * @return a {@link Set} of {@link Value} correspondent to the given parameter
   * @throws ValueResolvingException if an error occurs resolving {@link Value values}
   */
  public Message getSampleData(ParameterValueResolver parameterValueResolver) throws SampleDataException {
    return getSampleData(parameterValueResolver, nullSupplier, nullSupplier);
  }

  /**
   * Given the name of a parameter or parameter group, and if the parameter supports it, this will try to resolve
   * the {@link Value values} for the parameter.
   *
   * @param parameterValueResolver parameter resolver required if the associated {@link ValueProvider} requires
   *                               the value of parameters from the same parameter container.
   * @param connectionSupplier     supplier of connection instances related to the container and used, if necessary, by the
   *                               {@link ValueProvider}
   * @param configurationSupplier  supplier of connection instances related to the container and used, if necessary, by the
   *                               {@link ValueProvider}
   * @return a {@link Set} of {@link Value} correspondent to the given parameter
   * @throws SampleDataException if an error occurs resolving the sample data
   */
  public Message getSampleData(ParameterValueResolver parameterValueResolver,
                               Supplier<Object> connectionSupplier,
                               Supplier<Object> configurationSupplier) throws SampleDataException {
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
              reflectionCache.get(),
              muleContext.get());

      SampleDataProvider provider = factory.createSampleDataProvider();
      Result result = provider.getSample();

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
            getNullEvent(muleContext.get()),
            cursorProviderFactory,
            NO_OP_INSTANCE,
            streamingManager.get(),
            component,
            new NoRetryPolicyTemplate(),
            IMMEDIATE_SCHEDULER,
            empty(),
            muleContext.get());
  }
}
