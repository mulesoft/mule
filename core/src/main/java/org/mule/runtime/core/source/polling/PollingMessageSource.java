/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.source.polling;

import static java.lang.String.format;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.config.i18n.CoreMessages.couldNotRegisterNewScheduler;
import static org.mule.runtime.core.config.i18n.CoreMessages.pollSourceReturnedNull;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.core.execution.TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.source.polling.ScheduledPollFactory;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.context.notification.ConnectorMessageNotification;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.source.polling.schedule.ScheduledPoll;
import org.mule.runtime.core.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Polling {@link org.mule.runtime.core.api.source.MessageSource}.
 * </p>
 * <p>
 * The {@link PollingMessageSource} is responsible of creating a {@link org.mule.runtime.core.api.schedule.Scheduler} at the
 * initialization phase. This {@link org.mule.runtime.core.api.schedule.Scheduler} can be stopped/started and executed by using
 * the {@link org.mule.runtime.core.api.registry.MuleRegistry} interface, this way users can manipulate poll from outside mule
 * server.
 * </p>
 */
public class PollingMessageSource
    implements MessageSource, FlowConstructAware, Startable, Stoppable, MuleContextAware, Initialisable, Disposable {

  private static Logger logger = LoggerFactory.getLogger(PollingMessageSource.class);
  /**
   * The Polling name identifier. Used to create the scheduler name
   */
  public static final String POLLING_SCHEME = "polling";
  /**
   * Format string for all the Polling Schedulers name.
   */
  private static final String POLLING_SCHEDULER_NAME_FORMAT = POLLING_SCHEME + "://%s/%s";
  private final ScheduledPollFactory pollFactory;

  /**
   * The {@link ScheduledPoll} instance used to execute the scheduled jobs
   */
  private ScheduledPoll poll;
  private Processor listener;
  private FlowConstruct flowConstruct;
  private MuleContext muleContext;
  /**
   * <p>
   * The poll message source, configured inside the poll element in the xml configuration. i.e.:
   * 
   * <pre>
   * {@code
   * <poll>
   *       <sfdc:query query=""/>
   * </poll>
   * 
  </pre>
  
   * 
  </p>
   */
  protected Processor sourceMessageProcessor;

  /**
   * <p>
   * The {@link MessageProcessorPollingOverride} that affects the routing of the {@link Event}
   * </p>
   */
  protected MessageProcessorPollingOverride override;

  /**
   * @param muleContext application's context
   * @param sourceMessageProcessor message processor that should be triggered
   * @param override interceptor for each triggered operation
   * @param schedulerFactory factory for the scheduler
   */
  public PollingMessageSource(MuleContext muleContext, Processor sourceMessageProcessor,
                              MessageProcessorPollingOverride override, ScheduledPollFactory schedulerFactory) {
    this.muleContext = muleContext;
    this.sourceMessageProcessor = sourceMessageProcessor;
    this.override = override;
    this.pollFactory = schedulerFactory;
  }

  @Override
  public void start() throws MuleException {
    try {
      // The initialization phase if handled by the scheduler
      if (override instanceof Startable) {
        ((Startable) override).start();
      }
      if (sourceMessageProcessor instanceof Startable) {
        ((Startable) sourceMessageProcessor).start();
      }
    } catch (Exception ex) {
      this.stop();
      throw new CreateException(CoreMessages.failedToScheduleWork(), ex, this);
    }
  }

  public String getPollingUniqueName() {
    return flowConstruct.getName() + "-polling-" + this.hashCode();
  }

  @Override
  public void stop() throws MuleException {
    if (override instanceof Stoppable) {
      ((Stoppable) override).stop();
    }

    // Stop the scheduler to address the case when the flow is stop but not the application
    if (poll != null) {
      poll.stop();
    }
  }

  /**
   * Checks whether polling should take place on this instance.
   */
  public final void performPoll() {
    // Make sure we start with a clean state.
    setCurrentEvent(null);

    if (!pollOnPrimaryInstanceOnly() || flowConstruct.getMuleContext().isPrimaryPollingInstance()) {
      poll();
    }
  }

  private boolean pollOnPrimaryInstanceOnly() {
    return true;
  }

  /**
   * <p>
   * Helper method to create {@link org.mule.runtime.core.api.schedule.Scheduler} names
   * </p>
   */
  private String schedulerNameOf(FlowConstruct flowConstruct) {
    return format(POLLING_SCHEDULER_NAME_FORMAT, flowConstruct.getName(), this.hashCode());
  }

  /**
   * Triggers the forced execution of the polling message processor ignoring the configured scheduler.
   *
   * @throws Exception
   */
  public void poll() {
    InternalMessage request = InternalMessage.builder().payload(StringUtils.EMPTY).build();
    pollWith(request);
  }

  private void pollWith(final InternalMessage request) {
    ExecutionTemplate<Event> executionTemplate =
        createMainExecutionTemplate(muleContext, flowConstruct, flowConstruct.getExceptionListener());
    try {
      final MessageProcessorPollingInterceptor interceptor = override.interceptor();
      if (interceptor instanceof MuleContextAware) {
        ((MuleContextAware) interceptor).setMuleContext(muleContext);
      }
      Event muleEvent = executionTemplate.execute(new ExecutionCallback<Event>() {

        @Override
        public Event process() throws Exception {
          Event event = Event.builder(create(flowConstruct, getPollingUniqueName())).message(request)
              .exchangePattern(ONE_WAY).flow(flowConstruct).build();
          event = interceptor.prepareSourceEvent(event);

          setCurrentEvent(event);

          Event sourceEvent = sourceMessageProcessor.process(event);
          if (isNewMessage(sourceEvent)) {
            muleContext.getNotificationManager()
                .fireNotification(new ConnectorMessageNotification(this, sourceEvent.getMessage(), getPollingUniqueName(),
                                                                   flowConstruct, MESSAGE_RECEIVED));
            event = interceptor.prepareRouting(sourceEvent, sourceEvent);
            interceptor.postProcessRouting(listener.process(event));
          } else {
            logger.info(pollSourceReturnedNull(flowConstruct.getName()).getMessage());
          }
          return null;
        }
      });
      if (muleEvent != null) {
        interceptor.postProcessRouting(muleEvent);
      }
    } catch (MessagingException e) {
      // Already handled by TransactionTemplate
    } catch (Exception e) {
      muleContext.getExceptionListener().handleException(e);
    }
  }

  /**
   * <p>
   * On the Initialization phase it.
   * <ul>
   * <li>Calls the {@link ScheduledPollFactory} to create the scheduler</li>
   * <li>Gets the Poll the message source</li>
   * <li>Gets the Poll override</li>
   * </ul>
   * </p>
   */
  @Override
  public void initialise() throws InitialisationException {
    if (sourceMessageProcessor instanceof MuleContextAware) {
      ((MuleContextAware) sourceMessageProcessor).setMuleContext(muleContext);
    }
    if (sourceMessageProcessor instanceof FlowConstructAware) {
      ((FlowConstructAware) sourceMessageProcessor).setFlowConstruct(flowConstruct);
    }
    if (sourceMessageProcessor instanceof Initialisable) {
      ((Initialisable) sourceMessageProcessor).initialise();
    }
    if (override instanceof MuleContextAware) {
      ((MuleContextAware) override).setMuleContext(muleContext);
    }
    if (override instanceof FlowConstructAware) {
      ((FlowConstructAware) override).setFlowConstruct(flowConstruct);
    }
    if (override instanceof Initialisable) {
      ((Initialisable) override).initialise();
    }
    createScheduler();
  }

  @Override
  public void dispose() {
    if (override instanceof Disposable) {
      try {
        ((Disposable) override).dispose();
      } catch (Exception e) {
        logger.warn(format("Could not dispose polling override of class %s. Message receiver will continue to dispose",
                           override.getClass().getCanonicalName()),
                    e);
      }
    }

    disposeScheduler();
  }

  private void createScheduler() {
    poll = pollFactory.create(schedulerNameOf(flowConstruct), () -> performPoll());
    try {
      muleContext.getRegistry().registerObject(poll.getName(), poll);
    } catch (MuleException e) {
      logger.error(couldNotRegisterNewScheduler(poll.getName()).toString(), e);
    }
  }

  private void disposeScheduler() {
    if (poll != null) {
      try {
        muleContext.getRegistry().unregisterObject(poll.getName());
      } catch (MuleException e) {
        logger.warn(format("Could not unregister scheduler %s from registry.", poll.getName()), e);
      }
      poll = null;
    }
  }

  /**
   * Only consider response for source message processor a new message if it is not null and payload is not NullPayload
   *
   * @param event result of the polled message processor
   * @return true if the polled message processor return new content, false otherwise
   */
  protected boolean isNewMessage(Event event) {
    if (event != null && event.getMessage() != null) {
      InternalMessage message = event.getMessage();
      return message.getPayload().getValue() != null;
    }
    return false;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }


  @Override
  public void setListener(Processor listener) {
    this.listener = listener;
  }

}
