/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.isLazyInitMode;
import static org.mule.runtime.extension.api.ExtensionConstants.SCHEDULING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.injectComponentLocation;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.injectDefaultEncoding;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.injectRefName;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.source.scheduler.Scheduler;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ResolverSetBasedObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.runtime.source.poll.PollingSourceWrapper;

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
  private final ConfigurationProperties properties;
  private final MuleContext muleContext;
  private final boolean restarting;

  /**
   * Create a new instance
   *
   * @param model             the {@link SourceModel} which describes the instances that the {@link #configure(Source, Optional)} method will
   *                          accept
   * @param resolverSet       the {@link ResolverSet} used to resolve the parameters
   * @param expressionManager the {@link ExpressionManager} used to create a session used to evaluate the attributes.
   * @param properties        deployment configuration properties
   * @param muleContext       the current {@link MuleContext}
   */
  public SourceConfigurer(SourceModel model, ComponentLocation componentLocation, ResolverSet resolverSet,
                          ExpressionManager expressionManager, ConfigurationProperties properties, MuleContext muleContext) {
    this(model, componentLocation, resolverSet, expressionManager, properties, muleContext, false);
  }

  /**
   * Create a new instance
   *
   * @param model             the {@link SourceModel} which describes the instances that the {@link #configure(Source, Optional)} method will
   *                          accept
   * @param resolverSet       the {@link ResolverSet} used to resolve the parameters
   * @param expressionManager the {@link ExpressionManager} used to create a session used to evaluate the attributes.
   * @param properties        deployment configuration properties
   * @param muleContext       the current {@link MuleContext}
   * @param restarting        indicates if the source is being created after a restart or not.
   */
  public SourceConfigurer(SourceModel model, ComponentLocation componentLocation, ResolverSet resolverSet,
                          ExpressionManager expressionManager, ConfigurationProperties properties, MuleContext muleContext,
                          boolean restarting) {
    this.model = model;
    this.resolverSet = resolverSet;
    this.componentLocation = componentLocation;
    this.expressionManager = expressionManager;
    this.properties = properties;
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
  public Source configure(Source source, Optional<ConfigurationInstance> config) {
    ResolverSetBasedObjectBuilder<Source> builder =
        new ResolverSetBasedObjectBuilder<Source>(source.getClass(), model, resolverSet, expressionManager, muleContext) {

          @Override
          protected Source instantiateObject() {
            return source;
          }

          @Override
          public Source build(ValueResolvingContext context) throws MuleException {
            Source source = build(resolverSet.resolve(context));
            injectDefaultEncoding(model, source, muleContext.getConfiguration().getDefaultEncoding());
            injectComponentLocation(source, componentLocation);
            config.ifPresent(c -> injectRefName(source, c.getName(), getReflectionCache()));
            return source;
          }

        };

    CoreEvent initialiserEvent = null;
    ValueResolvingContext context = null;
    try {
      initialiserEvent = getInitialiserEvent(muleContext);
      context = ValueResolvingContext.builder(initialiserEvent, expressionManager).withConfig(config).build();
      Source configuredSource = builder.build(context);

      if (configuredSource instanceof PollingSource) {
        ValueResolver<?> valueResolver = resolverSet.getResolvers().get(SCHEDULING_STRATEGY_PARAMETER_NAME);
        if (valueResolver == null) {
          if (!isLazyInitMode(properties)) {
            throw new IllegalStateException("The scheduling strategy has not been configured");
          }
        } else {
          context = ValueResolvingContext.builder(initialiserEvent, expressionManager).build();
          Scheduler scheduler = (Scheduler) valueResolver.resolve(context);
          if (restarting) {
            scheduler = PollingSourceRestartSchedulerProvider.getScheduler(scheduler);
          }
          configuredSource = new PollingSourceWrapper<>((PollingSource) configuredSource, scheduler);
        }
      }

      return configuredSource;
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
}
