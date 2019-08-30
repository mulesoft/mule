/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.source.scheduler;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToScheduleWork;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.FAIL;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.component.ComponentUtils.getFromAnnotatedObjectOrFail;
import static org.mule.runtime.core.internal.util.rx.Operators.requestUnbounded;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.setCurrentEvent;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.CreateException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.notification.ConnectorMessageNotification;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.source.SchedulerConfiguration;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.context.notification.NotificationHelper;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.source.scheduler.PeriodicScheduler;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import org.slf4j.Logger;

import java.util.concurrent.ScheduledFuture;

/**
 * <p>
 * Polling {@link org.mule.runtime.core.api.source.MessageSource}.
 * </p>
 * <p>
 * The {@link DefaultSchedulerMessageSource} is responsible of creating a {@link org.mule.runtime.api.scheduler.Scheduler} at the
 * initialization phase. This {@link org.mule.runtime.api.scheduler.Scheduler} can be stopped/started and executed by using the
 * {@link org.mule.runtime.core.internal.registry.MuleRegistry} interface, this way users can manipulate poll from outside mule
 * server.
 * </p>
 */
public class DefaultSchedulerMessageSource extends AbstractComponent
    implements MessageSource, SchedulerMessageSource, MuleContextAware, Initialisable, Disposable {

  private final static Logger LOGGER = getLogger(DefaultSchedulerMessageSource.class);

  private final PeriodicScheduler scheduler;
  private final NotificationHelper notificationHelper;
  private final boolean disallowConcurrentExecution;

  private Scheduler pollingExecutor;
  private ScheduledFuture<?> schedulingJob;
  private Processor listener;
  private FlowConstruct flowConstruct;
  private MuleContext muleContext;
  private boolean started;
  private volatile boolean executing = false;

  /**
   * @param muleContext application's context
   * @param scheduler the scheduler
   */
  public DefaultSchedulerMessageSource(MuleContext muleContext, PeriodicScheduler scheduler,
                                       boolean disallowConcurrentExecution) {
    this.muleContext = muleContext;
    this.scheduler = scheduler;
    this.disallowConcurrentExecution = disallowConcurrentExecution;
    this.notificationHelper =
        new NotificationHelper(muleContext.getNotificationManager(), ConnectorMessageNotification.class, false);
  }

  @Override
  public synchronized void start() throws MuleException {
    if (started) {
      return;
    }
    try {
      // The initialization phase if handled by the scheduler
      schedulingJob =
          withContextClassLoader(muleContext.getExecutionClassLoader(), () -> scheduler.schedule(pollingExecutor, () -> run()));
      this.started = true;
    } catch (Exception ex) {
      this.stop();
      throw new CreateException(failedToScheduleWork(), ex, this);
    }
  }

  @Override
  public synchronized void stop() throws MuleException {
    if (!started) {
      return;
    }
    // Stop the scheduler to address the case when the flow is stop but not the application
    if (schedulingJob != null) {
      schedulingJob.cancel(false);
      schedulingJob = null;
    }
    this.started = false;
  }

  @Override
  public void trigger() {
    pollingExecutor.execute(() -> withContextClassLoader(muleContext.getExecutionClassLoader(), () -> poll()));
  }

  @Override
  public boolean isStarted() {
    return started;
  }

  @Override
  public SchedulerConfiguration getConfiguration() {
    return scheduler;
  }

  /**
   * Checks whether polling should take place on this instance.
   */
  private final void run() {
    // Make sure we start with a clean state.
    setCurrentEvent(null);

    if (muleContext.isPrimaryPollingInstance()) {
      poll();
    }
  }

  /**
   * Triggers the forced execution of the polling message processor ignoring the configured scheduler.
   */
  private void poll() {
    boolean execute = false;
    synchronized (this) {
      if (disallowConcurrentExecution && executing) {
        execute = false;
      } else {
        execute = true;
        executing = true;
      }
    }

    if (execute) {
      Message request = of(null);
      pollWith(request);
    } else {
      LOGGER.info("Flow '{}' is already running and 'disallowConcurrentExecution' is set to 'true'. Execution skipped.",
                  flowConstruct.getRootContainerLocation().getGlobalName());
    }
  }

  private void pollWith(final Message request) {
    try {
      just(request)
          .map(message -> InternalEvent.builder(create(flowConstruct, getLocation())).message(request)
              .build())
          .doOnNext(event -> setCurrentEvent(event))
          .doOnNext(event -> notificationHelper.fireNotification(this, event, getLocation(), MESSAGE_RECEIVED))
          .cast(CoreEvent.class)
          // Adding backpressure check call from inside the Scheduler, since the event is not dispatched from FlowProcessMediator
          .doOnNext(event -> flowConstruct.checkBackpressure(event))
          .transform(listener)
          .doOnError(MessagingException.class,
                     me -> ((BaseEventContext) me.getEvent().getContext()).error(me))
          .doOnSuccess(result -> ((BaseEventContext) result.getContext()).success())
          .doFinally(s -> {
            synchronized (DefaultSchedulerMessageSource.this) {
              executing = false;
            }
          })
          .subscribe(requestUnbounded());
    } catch (Exception e) {
      muleContext.getExceptionListener().handleException(e);
    }
  }

  /**
   * <p>
   * On the Initialization phase it.
   * <ul>
   * <li>Calls the {@link PeriodicScheduler} to create the scheduler</li>
   * <li>Gets the Poll the message source</li>
   * <li>Gets the Poll override</li>
   * </ul>
   * </p>
   */
  @Override
  public void initialise() throws InitialisationException {
    this.flowConstruct = getFromAnnotatedObjectOrFail(muleContext.getConfigurationComponentLocator(), this);
    createScheduler();
  }

  @Override
  public void dispose() {
    disposeScheduler();
  }

  private void createScheduler() throws InitialisationException {
    pollingExecutor = muleContext.getSchedulerService().cpuLightScheduler();
  }

  private void disposeScheduler() {
    if (pollingExecutor != null) {
      pollingExecutor.stop();
      pollingExecutor = null;
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }


  @Override
  public void setListener(Processor listener) {
    this.listener = listener;
  }

  @Override
  public BackPressureStrategy getBackPressureStrategy() {
    return FAIL;
  }
}
