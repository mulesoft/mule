/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.source;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.extension.socket.internal.SocketUtils.WORK;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.core.util.concurrent.ThreadNameHelper.getPrefix;

import org.mule.extension.socket.api.SocketAttributes;
import org.mule.extension.socket.api.config.ListenerConfig;
import org.mule.extension.socket.api.connection.ListenerConnection;
import org.mule.extension.socket.api.worker.SocketWorker;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.io.InputStream;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for socket connections of the given protocol in the configured host and port.
 * <p>
 * Whenever a new connection is received, this {@link Source} will schedule a a {@link SocketWorker} that will handle the
 * communication for that particular connection.
 *
 * @since 4.0
 */
@EmitsResponse
public final class SocketListener extends Source<InputStream, SocketAttributes> implements FlowConstructAware {

  private static final Logger LOGGER = LoggerFactory.getLogger(SocketListener.class);
  private FlowConstruct flowConstruct;

  @Inject
  private MuleContext muleContext;

  @Inject
  private SchedulerService schedulerService;

  @Connection
  private ListenerConnection connection;

  @UseConfig
  private ListenerConfig config;

  private AtomicBoolean stopRequested = new AtomicBoolean(false);
  private Scheduler workManager;
  private Scheduler listenerExecutor;

  private Future<?> submittedListenerTask;

  /**
   * {@inheritDoc}
   */
  @Override
  public void onStart(SourceCallback<InputStream, SocketAttributes> sourceCallback) throws MuleException {
    workManager = schedulerService
        .ioScheduler(config().withName(format("%s%s.socket.worker", getPrefix(muleContext), flowConstruct.getName())));

    stopRequested.set(false);

    listenerExecutor = schedulerService.customScheduler(config().withMaxConcurrentTasks(1)
        .withName(format("%s%s.socket.listener", getPrefix(muleContext), flowConstruct.getName())));
    submittedListenerTask = listenerExecutor.submit(() -> listen(sourceCallback));
  }

  @OnSuccess
  public void onSuccess(@Optional(defaultValue = "#[mel:payload]") @XmlHints(allowReferences = false) Object responseValue,
                        SourceCallbackContext context) {
    SocketWorker worker = context.getVariable(WORK);
    worker.onComplete(responseValue);
  }


  @OnError
  public void onError(Error error, SourceCallbackContext context) {
    SocketWorker worker = context.getVariable("work");
    worker.onError(error.getCause());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onStop() {
    submittedListenerTask.cancel(false);
    stopRequested.set(true);
    listenerExecutor.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS);
    workManager.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  private boolean isRequestedToStop() {
    return stopRequested.get() || currentThread().isInterrupted();
  }

  private void listen(SourceCallback<InputStream, SocketAttributes> sourceCallback) {
    for (;;) {
      if (isRequestedToStop()) {
        return;
      }

      try {
        SocketWorker worker = connection.listen(sourceCallback);
        worker.setEncoding(config.getDefaultEncoding());
        worker.onError(e -> {
          Throwable t = e;
          if (t.getCause() != null) {
            t = t.getCause();
          }

          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("Got exception '%s'. Work being executed was: %s", t.getClass().getName(), worker.toString()));
          }

          if (t instanceof MessagingException || t instanceof ConnectionException) {
            sourceCallback.onSourceException(t);
          }
        });
        workManager.execute(worker);
      } catch (ConnectionException e) {
        if (!isRequestedToStop()) {
          sourceCallback.onSourceException(e);
        }
      } catch (Exception e) {
        if (isRequestedToStop()) {
          return;
        }

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("An exception occurred while listening for new connections", e);
        }
      }
    }
  }
}
