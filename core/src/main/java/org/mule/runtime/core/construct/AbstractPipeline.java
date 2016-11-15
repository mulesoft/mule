/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.construct;

import static java.lang.String.format;
import static org.apache.commons.collections.CollectionUtils.selectRejected;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.context.notification.PipelineMessageNotification.PROCESS_COMPLETE;
import static org.mule.runtime.core.context.notification.PipelineMessageNotification.PROCESS_END;
import static org.mule.runtime.core.context.notification.PipelineMessageNotification.PROCESS_START;
import static org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory.SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.runtime.core.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.util.NotificationUtils.buildPathResolver;
import static org.mule.runtime.core.util.rx.Exceptions.rxExceptionToMuleException;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.GlobalNameableObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.construct.FlowConstructInvalidException;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.processor.DefaultMessageProcessorPathElement;
import org.mule.runtime.core.api.processor.InternalMessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.source.ClusterizableMessageSource;
import org.mule.runtime.core.api.source.CompositeMessageSource;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.source.NonBlockingMessageSource;
import org.mule.runtime.core.api.transport.LegacyInboundEndpoint;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.connector.ConnectException;
import org.mule.runtime.core.context.notification.PipelineMessageNotification;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.AbstractFilteringMessageProcessor;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.processor.AbstractRequestResponseMessageProcessor;
import org.mule.runtime.core.processor.IdempotentRedeliveryPolicy;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.processor.strategy.DefaultFlowProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.LegacyAsynchronousProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.LegacyNonBlockingProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory;
import org.mule.runtime.core.source.ClusterizableMessageSourceWrapper;
import org.mule.runtime.core.util.NotificationUtils;
import org.mule.runtime.core.util.NotificationUtils.PathResolver;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.Predicate;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Abstract implementation of {@link AbstractFlowConstruct} that allows a list of {@link Processor}s that will be used to process
 * messages to be configured. These MessageProcessors are chained together using the {@link DefaultMessageProcessorChainBuilder}.
 * <p/>
 * If no message processors are configured then the source message is simply returned.
 */
public abstract class AbstractPipeline extends AbstractFlowConstruct implements Pipeline {

  protected MessageSource messageSource;
  protected MessageProcessorChain pipeline;

  protected final SchedulerService schedulerService;

  protected List<Processor> messageProcessors = Collections.emptyList();
  private PathResolver flowMap;

  protected ProcessingStrategyFactory processingStrategyFactory;
  protected ProcessingStrategy processingStrategy;
  private boolean canProcessMessage = false;

  private static final Predicate sourceCompatibleWithAsync = new Predicate() {

    @Override
    public boolean evaluate(Object messageSource) {
      if (messageSource instanceof LegacyInboundEndpoint) {
        return ((LegacyInboundEndpoint) messageSource).isCompatibleWithAsync();
      } else if (messageSource instanceof CompositeMessageSource) {
        return selectRejected(((CompositeMessageSource) messageSource).getSources(), this).isEmpty();
      } else {
        return true;
      }
    }
  };

  public AbstractPipeline(String name, MuleContext muleContext) {
    super(name, muleContext);
    this.schedulerService = muleContext.getSchedulerService();
    initialiseProcessingStrategy();
  }

  /**
   * Creates a {@link Processor} that will process messages from the configured {@link MessageSource} .
   * <p>
   * The default implementation of this methods uses a {@link DefaultMessageProcessorChainBuilder} and allows a chain of
   * {@link Processor}s to be configured using the
   * {@link #configureMessageProcessors(org.mule.runtime.core.api.processor.MessageProcessorChainBuilder)} method but if you wish
   * to use another {@link MessageProcessorBuilder} or just a single {@link Processor} then this method can be overridden and
   * return a single {@link Processor} instead.
   */
  protected MessageProcessorChain createPipeline() throws MuleException {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.setName("'" + getName() + "' processor chain");
    configurePreProcessors(builder);
    configureMessageProcessors(builder);
    configurePostProcessors(builder);
    return builder.build();
  }

  /**
   * A fallback method for creating a {@link ProcessingStrategyFactory} to be used in case the user hasn't specified one through
   * either {@link #setProcessingStrategyFactory(ProcessingStrategyFactory)}, through
   * {@link MuleConfiguration#getDefaultProcessingStrategyFactory()} or the
   * {@link MuleProperties#MULE_DEFAULT_PROCESSING_STRATEGY} system property
   *
   * @return a {@link SynchronousProcessingStrategyFactory}
   */
  protected ProcessingStrategyFactory createDefaultProcessingStrategyFactory() {
    return new SynchronousProcessingStrategyFactory();
  }

  private void initialiseProcessingStrategy() {
    if (processingStrategy == null) {
      if (processingStrategyFactory == null) {
        final ProcessingStrategyFactory defaultProcessingStrategyFactory =
            muleContext.getConfiguration().getDefaultProcessingStrategyFactory();

        if (defaultProcessingStrategyFactory == null) {
          processingStrategyFactory = createDefaultProcessingStrategyFactory();
        } else {
          processingStrategyFactory = defaultProcessingStrategyFactory;
        }
      }

      processingStrategy = processingStrategyFactory.create(muleContext);
    }

    boolean userConfiguredProcessingStrategy = !(getProcessingStrategyFactory() instanceof DefaultFlowProcessingStrategyFactory);
    boolean redeliveryHandlerConfigured = isRedeliveryPolicyConfigured();
    if (!userConfiguredProcessingStrategy && redeliveryHandlerConfigured) {
      processingStrategy = new SynchronousProcessingStrategyFactory().create(muleContext);
      if (LOGGER.isWarnEnabled()) {
        LOGGER
            .warn("Using message redelivery and on-error-propagate requires synchronous processing strategy. Processing strategy re-configured to synchronous");
      }
    }
  }

  protected void configurePreProcessors(MessageProcessorChainBuilder builder) throws MuleException {
    builder.chain(new ProcessorStartCompleteProcessor());
  }

  protected void configurePostProcessors(MessageProcessorChainBuilder builder) throws MuleException {
    builder.chain(new ProcessEndProcessor());
  }

  @Override
  public void setMessageProcessors(List<Processor> messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  @Override
  public List<Processor> getMessageProcessors() {
    return messageProcessors;
  }

  @Override
  public MessageSource getMessageSource() {
    return messageSource;
  }

  @Override
  public void setMessageSource(MessageSource messageSource) {
    if (messageSource instanceof ClusterizableMessageSource) {
      this.messageSource = new ClusterizableMessageSourceWrapper(muleContext, (ClusterizableMessageSource) messageSource, this);
    } else {
      this.messageSource = messageSource;
    }
  }

  @Override
  public boolean isSynchronous() {
    return processingStrategy.isSynchronous();
  }

  public ProcessingStrategyFactory getProcessingStrategyFactory() {
    return processingStrategyFactory;
  }

  @Override
  public void setProcessingStrategyFactory(ProcessingStrategyFactory processingStrategyFactory) {
    this.processingStrategyFactory = processingStrategyFactory;
    this.processingStrategy = null;
  }

  @Override
  public ProcessingStrategy getProcessingStrategy() {
    return processingStrategy;
  }

  @Override
  protected void doInitialise() throws MuleException {
    super.doInitialise();

    initialiseProcessingStrategy();

    pipeline = createPipeline();

    if (messageSource != null) {
      // Wrap chain to decouple lifecycle
      messageSource.setListener(new AbstractInterceptingMessageProcessor() {

        @Override
        public Event process(final Event event) throws MuleException {
          if (event.isSynchronous() || isSynchronous() || isTransactionActive()) {
            return pipeline.process(event);
          } else {
            return Mono.just(event).transform(this).block();
          }
        }

        @Override
        public Publisher<Event> apply(Publisher<Event> publisher) {
          return from(publisher).transform(pipeline);
        }
      });
    }

    injectFlowConstructMuleContext(messageSource);
    injectExceptionHandler(messageSource);
    injectFlowConstructMuleContext(pipeline);
    injectExceptionHandler(pipeline);
    initialiseIfInitialisable(messageSource);
    initialiseIfInitialisable(pipeline);

    createFlowMap();
  }

  protected void configureMessageProcessors(MessageProcessorChainBuilder builder) throws MuleException {
    builder.chain(new ProcessingStrategyInterceptingMessageProcessor());
    for (Object processor : getMessageProcessors()) {
      if (processor instanceof Processor) {
        builder.chain((Processor) processor);
      } else if (processor instanceof MessageProcessorBuilder) {
        builder.chain((MessageProcessorBuilder) processor);
      } else {
        throw new IllegalArgumentException(
                                           "MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured");
      }
    }
  }

  @Override
  public void validateConstruct() throws FlowConstructInvalidException {
    super.validateConstruct();

    // Ensure that inbound endpoints are compatible with processing strategy.
    boolean userConfiguredProcessingStrategy = !(getProcessingStrategyFactory() instanceof DefaultFlowProcessingStrategyFactory);
    boolean userConfiguredAsyncProcessingStrategy =
        getProcessingStrategyFactory() instanceof LegacyAsynchronousProcessingStrategyFactory && userConfiguredProcessingStrategy;

    boolean isCompatibleWithAsync = sourceCompatibleWithAsync.evaluate(messageSource);
    if (userConfiguredAsyncProcessingStrategy
        && (!(messageSource == null || isCompatibleWithAsync))) {
      throw new FlowConstructInvalidException(CoreMessages
          .createStaticMessage("One of the message sources configured on this Flow is not "
              + "compatible with an asynchronous processing strategy.  Either "
              + "because it is request-response, has a transaction defined, or " + "messaging redelivered is configured."), this);
    }

    if (getProcessingStrategyFactory() instanceof LegacyNonBlockingProcessingStrategyFactory && messageSource != null
        && !(messageSource instanceof NonBlockingMessageSource)) {
      throw new FlowConstructInvalidException(CoreMessages
          .createStaticMessage(format("The non-blocking processing strategy (%s) currently only supports non-blocking messages sources (source is %s).",
                                      getProcessingStrategyFactory().toString(), messageSource.toString())), this);
    }
  }

  protected boolean isRedeliveryPolicyConfigured() {
    if (getMessageProcessors().isEmpty()) {
      return false;
    }
    return getMessageProcessors().get(0) instanceof IdempotentRedeliveryPolicy;
  }

  @Override
  protected void doStart() throws MuleException {
    super.doStart();
    startIfStartable(processingStrategy);
    startIfStartable(pipeline);
    startIfNeeded(processingStrategy);
    canProcessMessage = true;
    if (muleContext.isStarted()) {
      try {
        startIfStartable(messageSource);
      }
      // Let connection exceptions bubble up to trigger the reconnection strategy.
      catch (ConnectException ce) {
        throw ce;
      } catch (MuleException e) {
        // If the messageSource couldn't be started we would need to stop the pipeline (if possible) in order to leave
        // its LifecycleManager also as initialise phase so the flow can be disposed later
        doStop();
        throw e;
      }
    }
  }

  private void createFlowMap() {
    DefaultMessageProcessorPathElement pipeLinePathElement = new DefaultMessageProcessorPathElement(null, getName());
    addMessageProcessorPathElements(pipeLinePathElement);
    flowMap = buildPathResolver(pipeLinePathElement);
  }

  @Override
  public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
    String processorsPrefix = "processors";
    String esPrefix = "es";

    NotificationUtils.addMessageProcessorPathElements(pipeline, pathElement.addChild(processorsPrefix));

    if (exceptionListener instanceof MessageProcessorContainer) {
      String esGlobalName = getExceptionStrategyGlobalName();
      MessageProcessorPathElement exceptionStrategyPathElement = pathElement;
      if (esGlobalName != null) {
        exceptionStrategyPathElement = exceptionStrategyPathElement.addChild(esGlobalName);
      }
      exceptionStrategyPathElement = exceptionStrategyPathElement.addChild(esPrefix);
      ((MessageProcessorContainer) exceptionListener).addMessageProcessorPathElements(exceptionStrategyPathElement);

    }

  }

  private String getExceptionStrategyGlobalName() {
    String globalName = null;
    if (exceptionListener instanceof GlobalNameableObject) {
      globalName = ((GlobalNameableObject) exceptionListener).getGlobalName();
    }
    return globalName;
  }

  @Override
  public String getProcessorPath(Processor processor) {
    return flowMap.resolvePath(processor);
  }


  public class ProcessIfPipelineStartedMessageProcessor extends AbstractFilteringMessageProcessor implements
      InternalMessageProcessor {

    @Override
    protected boolean accept(Event event, Event.Builder builder) {
      return canProcessMessage;
    }

    @Override
    protected Event handleUnaccepted(Event event) throws LifecycleException {
      throw new LifecycleException(CoreMessages.isStopped(getName()), event.getMessage());
    }
  }

  @Override
  protected void doStop() throws MuleException {
    try {
      stopIfStoppable(messageSource);
    } finally {
      canProcessMessage = false;
    }

    stopIfStoppable(processingStrategy);
    stopIfStoppable(pipeline);
    super.doStop();
  }

  @Override
  protected void doDispose() {
    disposeIfDisposable(pipeline);
    disposeIfDisposable(messageSource);
    super.doDispose();
  }

  private class ProcessEndProcessor implements Processor, InternalMessageProcessor {

    @Override
    public Event process(Event event) throws MuleException {
      muleContext.getNotificationManager()
          .fireNotification(new PipelineMessageNotification(AbstractPipeline.this, event, PROCESS_END));
      return event;
    }
  }

  private class ProcessorStartCompleteProcessor extends AbstractRequestResponseMessageProcessor
      implements InternalMessageProcessor {

    @Override
    protected Event processRequest(Event event) throws MuleException {
      muleContext.getNotificationManager()
          .fireNotification(new PipelineMessageNotification(AbstractPipeline.this, event, PROCESS_START));
      return super.processRequest(event);
    }

    @Override
    protected void processFinally(Event event, MessagingException exception) {
      muleContext.getNotificationManager()
          .fireNotification(new PipelineMessageNotification(AbstractPipeline.this, event, PROCESS_COMPLETE, exception));
    }
  }

  private class ProcessingStrategyInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor
      implements InternalMessageProcessor {

    @Override
    public Event process(Event event) throws MuleException {
      if (processingStrategy != SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE) {
        try {
          return Mono.just(event).transform(this).block();
        } catch (Throwable e) {
          throw rxExceptionToMuleException(e);
        }
      } else {
        return processNext(event);
      }
    }

    @Override
    public Publisher<Event> apply(Publisher<Event> publisher) {
      return from(publisher).transform(processingStrategy.onPipeline(AbstractPipeline.this, next));
    }

    @Override
    public ProcessingType getProccesingType() {
      return CPU_LITE;
    }

  }
}
