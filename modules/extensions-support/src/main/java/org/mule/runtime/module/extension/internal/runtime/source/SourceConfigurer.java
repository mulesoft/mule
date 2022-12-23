/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.getMuleVersion;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.extension.api.ExtensionConstants.POLLING_SOURCE_LIMIT_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.SCHEDULING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.runtime.source.legacy.SourceTransactionalActionUtils.toLegacy;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.injectComponentLocation;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.injectDefaultEncoding;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.injectRefName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.injectRuntimeVersion;

import static java.lang.String.format;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.scheduler.SchedulingStrategy;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ResolverSetBasedObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.runtime.source.legacy.SdkSourceAdapterFactory;
import org.mule.runtime.module.extension.internal.runtime.source.poll.PollingSourceWrapper;
import org.mule.sdk.api.runtime.source.PollingSource;
import org.mule.sdk.api.runtime.source.Source;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves and injects the values of a {@link Source} that has fields annotated with {@link Parameter} or
 * {@link ParameterGroupDescriptor}
 *
 * @since 4.0
 */
public final class SourceConfigurer {

  private final SourceModel model;
  private final ResolverSet resolverSet;
  private final ComponentLocation componentLocation;
  private final ExpressionManager expressionManager;
  private final MuleContext muleContext;
  private final boolean restarting;

  /**
   * Create a new instance
   *
   * @param model             the {@link SourceModel} which describes the instances that the {@link #configure(Source, Optional)}
   *                          method will accept
   * @param resolverSet       the {@link ResolverSet} used to resolve the parameters
   * @param expressionManager the {@link ExpressionManager} used to create a session used to evaluate the attributes.
   * @param muleContext       the current {@link MuleContext}
   */
  public SourceConfigurer(SourceModel model, ComponentLocation componentLocation, ResolverSet resolverSet,
                          ExpressionManager expressionManager, MuleContext muleContext) {
    this(model, componentLocation, resolverSet, expressionManager, muleContext, false);
  }

  /**
   * Create a new instance
   *
   * @param model             the {@link SourceModel} which describes the instances that the {@link #configure(Source, Optional)}
   *                          method will accept
   * @param resolverSet       the {@link ResolverSet} used to resolve the parameters
   * @param expressionManager the {@link ExpressionManager} used to create a session used to evaluate the attributes.
   * @param properties        deployment configuration properties
   * @param muleContext       the current {@link MuleContext}
   * @param restarting        indicates if the source is being created after a restart or not.
   */
  public SourceConfigurer(SourceModel model, ComponentLocation componentLocation, ResolverSet resolverSet,
                          ExpressionManager expressionManager, MuleContext muleContext,
                          boolean restarting) {
    this.model = model;
    this.resolverSet = resolverSet;
    this.componentLocation = componentLocation;
    this.expressionManager = expressionManager;
    this.muleContext = muleContext;
    this.restarting = restarting;
  }

  /**
   * Performs the configuration of the given {@code source} and returns the result
   *
   * @param source a {@link Source}
   * @param config the {@link ConfigurationInstance config instance} associated to {@code this} source object.
   * @return the configured instance
   * @throws MuleException
   */
  public Source configure(Object source, Optional<ConfigurationInstance> config) {
    ResolverSetBasedObjectBuilder<Object> builder =
        new ResolverSetBasedObjectBuilder<Object>(source.getClass(), model, resolverSet, expressionManager, muleContext) {

          @Override
          protected Object instantiateObject() {
            return source;
          }

          @Override
          public Object build(ValueResolvingContext context) throws MuleException {
            Object builtSource = build(buildResolverSetResult(source, context));
            injectDefaultEncoding(model, builtSource, muleContext.getConfiguration().getDefaultEncoding());
            injectRuntimeVersion(model, builtSource, getMuleVersion());
            injectComponentLocation(builtSource, componentLocation);
            config.ifPresent(c -> injectRefName(builtSource, c.getName(), getReflectionCache()));
            return builtSource;
          }

        };

    CoreEvent initialiserEvent = null;
    ValueResolvingContext context = null;
    try {
      initialiserEvent = getNullEvent(muleContext);
      context = ValueResolvingContext.builder(initialiserEvent, expressionManager).withConfig(config).build();
      Object configuredSource = builder.build(context);

      Source sdkSource = SdkSourceAdapterFactory.createAdapter(configuredSource);

      if (sdkSource instanceof PollingSource) {
        ValueResolver<?> valueResolver = resolverSet.getResolvers().get(SCHEDULING_STRATEGY_PARAMETER_NAME);
        if (valueResolver != null) {
          context = ValueResolvingContext.builder(initialiserEvent, expressionManager).build();
          SchedulingStrategy scheduler = (SchedulingStrategy) valueResolver.resolve(context);
          sdkSource = new PollingSourceWrapper<>((PollingSource) sdkSource, scheduler,
                                                 resolverMaxItemsPerPoll(resolverSet, context, initialiserEvent),
                                                 muleContext.getExceptionListener());

        } else {
          throw new IllegalStateException("No SchedulingStrategy provided for PollingSource");
        }
      }

      return sdkSource;
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Exception was found trying to configure source of type "
          + source.getClass().getName()), e);
    } finally {
      if (initialiserEvent != null) {
        ((BaseEventContext) initialiserEvent.getContext()).success();
      }
      if (context != null) {
        context.close();
      }
    }
  }

  private int resolverMaxItemsPerPoll(ResolverSet resolverSet, ValueResolvingContext context, CoreEvent event)
      throws MuleException {
    ValueResolver<?> valueResolver = resolverSet.getResolvers().get(POLLING_SOURCE_LIMIT_PARAMETER_NAME);
    if (valueResolver == null) {
      return Integer.MAX_VALUE;
    } else {
      int maxItemsPerPoll = (Integer) valueResolver.resolve(context);
      if (maxItemsPerPoll < 1) {
        throw new IllegalArgumentException(format("The %s parameter must have a value greater than 1",
                                                  POLLING_SOURCE_LIMIT_PARAMETER_NAME));
      }
      return maxItemsPerPoll;
    }
  }

  private ResolverSetResult buildResolverSetResult(Object source, ValueResolvingContext context) throws MuleException {
    ResolverSetResult resolverSetResult = resolverSet.resolve(context);
    Optional<Field> field = Arrays.stream(source.getClass().getDeclaredFields())
        .filter(f -> f.getType().getName().equals(org.mule.runtime.extension.api.tx.SourceTransactionalAction.class.getName()))
        .findFirst();
    if (field.isPresent() && resolverSet.getResolvers().get(field.get().getName()) != null) {
      return overwriteResolverResult(field.get().getName(), resolverSetResult);
    } else {
      return resolverSetResult;
    }
  }

  private ResolverSetResult overwriteResolverResult(String fieldName, ResolverSetResult resolverSetResult) {
    ResolverSetResult.Builder builder = ResolverSetResult.newBuilder();
    for (Map.Entry<String, Object> entry : resolverSetResult.asMap().entrySet()) {
      if (entry.getKey().equals(fieldName)) {
        builder.add(entry.getKey(), toLegacy(entry.getValue()));
      } else {
        builder.add(entry.getKey(), entry.getValue());
      }
    }
    return builder.build();
  }
}
