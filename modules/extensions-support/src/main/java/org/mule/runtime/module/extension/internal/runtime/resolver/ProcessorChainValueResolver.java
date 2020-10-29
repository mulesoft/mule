/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Optional.empty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.registerObject;

import org.mule.runtime.api.component.execution.ExecutionResult;
import org.mule.runtime.api.component.execution.InputEvent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.util.ObjectNameHelper;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.module.extension.internal.runtime.operation.ImmutableProcessorChainExecutor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.xml.namespace.QName;

import org.reactivestreams.Publisher;

/**
 * An {@link ValueResolver} which wraps the given {@link Processor} in a {@link Chain},
 * using the event of the current {@link ValueResolvingContext}.
 * This resolver returns new instances per every invocation
 *
 * @since 4.0
 */
public final class ProcessorChainValueResolver implements ValueResolver<Chain> {

  private final MessageProcessorChain chain;

  /**
   * Creates a resolver for the provided chain executor. The lifecycle of the provided {@code chain} must be managed by the owner
   * of the chain.
   *
   * @param chain the chain to create an executor for
   */
  public ProcessorChainValueResolver(final MessageProcessorChain chain) {
    this.chain = chain;
  }

  /**
   * Creates a resolver for a new chain executor with the give {@code processors}, tying the lifecycle of that chain to the
   * lifecycle of the provided {@code muleContext}.
   *
   * @param ctx the context to tie the lifecycle of the chain to be created to
   * @param processors the processors that will be part of the chain to create an executor for
   */
  public ProcessorChainValueResolver(MuleContext ctx, List<Processor> processors) {
    // TODO MULE-18939 lifecycle of this chain must be managed by its owner, not the muleContext
    this(new LazyInitializerChainDecorator(ctx, newChain(empty(), processors)));

    try {
      registerObject(ctx, new ObjectNameHelper(ctx).getUniqueName(""), this.chain);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not register nested MessageProcessorChain"), e);
    }
  }

  /**
   * Returns a {@link Chain} that wraps the given {@link Processor} using the current {@code event}
   *
   * @param context a {@link ValueResolvingContext}
   * @return a {@link Chain}
   * @throws MuleException
   */
  @Override
  public Chain resolve(ValueResolvingContext context) throws MuleException {
    return new ImmutableProcessorChainExecutor(context.getEvent(), chain);
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return false;
  }

  @Override
  public boolean isContent() {
    return false;
  }

  private static final class LazyInitializerChainDecorator implements MessageProcessorChain {

    private final MessageProcessorChain delegate;
    private final MuleContext muleContext;
    private boolean initialised = false;

    LazyInitializerChainDecorator(MuleContext ctx, MessageProcessorChain chain) {
      this.delegate = chain;
      this.muleContext = ctx;
    }

    @Override
    public void initialise() throws InitialisationException {
      if (!initialised) {
        initialiseIfNeeded(delegate, muleContext);
        initialised = true;
      }
    }

    @Override
    public void start() throws MuleException {
      initialise();
      this.delegate.start();
    }

    @Override
    public void stop() throws MuleException {
      this.delegate.stop();
    }

    @Override
    public void dispose() {
      this.delegate.dispose();
      initialised = false;
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return this.delegate.apply(publisher);
    }

    @Override
    public List<Processor> getMessageProcessors() {
      return this.delegate.getMessageProcessors();
    }

    @Override
    public CompletableFuture<ExecutionResult> execute(InputEvent inputEvent) {
      return this.delegate.execute(inputEvent);
    }

    @Override
    public CompletableFuture<Event> execute(Event event) {
      return this.delegate.execute(event);
    }

    @Override
    public Object getAnnotation(QName name) {
      return this.delegate.getAnnotation(name);
    }

    @Override
    public Map<QName, Object> getAnnotations() {
      return this.delegate.getAnnotations();
    }

    @Override
    public void setAnnotations(Map<QName, Object> annotations) {
      this.delegate.setAnnotations(annotations);
    }

    @Override
    public ComponentLocation getLocation() {
      return this.delegate.getLocation();
    }

    @Override
    public Location getRootContainerLocation() {
      return this.delegate.getRootContainerLocation();
    }

    @Override
    public void setMuleContext(MuleContext context) {
      this.delegate.setMuleContext(context);
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return this.delegate.process(event);
    }
  }

}
