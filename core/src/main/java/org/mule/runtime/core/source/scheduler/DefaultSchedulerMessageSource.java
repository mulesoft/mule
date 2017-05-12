/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.source.scheduler;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.api.Event.builder;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.config.i18n.CoreMessages.failedToScheduleWork;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.core.internal.util.rx.Operators.requestUnbounded;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.source.SchedulerMessageSource;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.api.lifecycle.CreateException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.source.polling.PeriodicScheduler;
import org.mule.runtime.core.context.notification.ConnectorMessageNotification;
import org.mule.runtime.core.context.notification.NotificationHelper;

import java.util.concurrent.ScheduledFuture;

/**
 * <p>
 * Polling {@link org.mule.runtime.core.api.source.MessageSource}.
 * </p>
 * <p>
 * The {@link DefaultSchedulerMessageSource} is responsible of creating a {@link org.mule.runtime.api.scheduler.Scheduler} at the
 * initialization phase. This {@link org.mule.runtime.api.scheduler.Scheduler} can be stopped/started and executed by using the
 * {@link org.mule.runtime.core.api.registry.MuleRegistry} interface, this way users can manipulate poll from outside mule server.
 * </p>
 */
public class DefaultSchedulerMessageSource extends AbstractAnnotatedObject
    implements MessageSource, FlowConstructAware, SchedulerMessageSource, MuleContextAware, Initialisable, Disposable {

  private final PeriodicScheduler scheduler;
  private final NotificationHelper notificationHelper;

  private Scheduler pollingExecutor;
  private ScheduledFuture<?> schedulingJob;
  private Processor listener;
  private FlowConstruct flowConstruct;
  private MuleContext muleContext;
  private boolean started;

  /**
   * @param muleContext application's context
   * @param scheduler the scheduler
   */
  public DefaultSchedulerMessageSource(MuleContext muleContext, PeriodicScheduler scheduler) {
    this.muleContext = muleContext;
    this.scheduler = scheduler;
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
      schedulingJob = scheduler.schedule(pollingExecutor, () -> run());
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
    pollingExecutor.execute(() -> poll());
  }

  @Override
  public boolean isStarted() {
    return started;
  }

  /**
   * Checks whether polling should take place on this instance.
   */
  private final void run() {
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
   * Triggers the forced execution of the polling message processor ignoring the configured scheduler.
   */
  private void poll() {
    Message request = of(null);
    pollWith(request);
  }

  private void pollWith(final Message request) {
    try {
      just(request)
          .map(message -> builder(create(flowConstruct, getLocation())).message(request).flow(flowConstruct).build())
          .doOnNext(event -> setCurrentEvent(event))
          .doOnNext(event -> notificationHelper.fireNotification(this, event, flowConstruct, MESSAGE_RECEIVED))
          .transform(listener)
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
