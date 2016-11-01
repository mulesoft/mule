/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.source;

import static java.lang.String.format;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.extension.socket.internal.SocketUtils.WORK;
import static org.mule.runtime.core.util.concurrent.ThreadNameHelper.getPrefix;
import org.mule.extension.socket.api.SocketAttributes;
import org.mule.extension.socket.api.config.ListenerConfig;
import org.mule.extension.socket.api.connection.ListenerConnection;
import org.mule.extension.socket.api.worker.SocketWorker;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

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
  private ExecutorService executorService;
  private FlowConstruct flowConstruct;

  @Inject
  private MuleContext muleContext;

  @Connection
  private ListenerConnection connection;

  @UseConfig
  private ListenerConfig config;

  private AtomicBoolean stopRequested = new AtomicBoolean(false);
  private WorkManager workManager;

  /**
   * {@inheritDoc}
   */
  @Override
  public void onStart(SourceCallback<InputStream, SocketAttributes> sourceCallback) throws MuleException {
    // TODO MULE-9898
    ThreadingProfile threadingProfile =
        config.getThreadingProfile() == null ? muleContext.getDefaultThreadingProfile() : config.getThreadingProfile();
    workManager =
        threadingProfile.createWorkManager("SocketListenerWorkManager", muleContext.getConfiguration().getShutdownTimeout());
    workManager.start();

    executorService =
        newSingleThreadExecutor(r -> new Thread(r,
                                                format("%s%s.socket.listener", getPrefix(muleContext), flowConstruct.getName())));

    stopRequested.set(false);
    executorService.execute(() -> listen(sourceCallback));
  }

  @OnSuccess
  public void onSuccess(@Optional(defaultValue = "#[payload]") @XmlHints(allowReferences = false) Object responseValue,
                        SourceCallbackContext context) {
    SocketWorker worker = context.getVariable(WORK);
    worker.onComplete(responseValue);
  }


  @OnError
  public void onError(Error error, SourceCallbackContext context) {
    SocketWorker worker = context.getVariable("work");
    worker.onError(error.getCause());
  }

  private class SocketWorkListener implements WorkListener {

    private final SourceCallback<InputStream, SocketAttributes> sourceCallback;

    private SocketWorkListener(
                               SourceCallback<InputStream, SocketAttributes> sourceCallback) {
      this.sourceCallback = sourceCallback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void workAccepted(WorkEvent event) {
      handleWorkException(event, "workAccepted");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void workRejected(WorkEvent event) {
      handleWorkException(event, "workRejected");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void workStarted(WorkEvent event) {
      handleWorkException(event, "workStarted");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void workCompleted(WorkEvent event) {
      handleWorkException(event, "workCompleted");
    }

    private void handleWorkException(WorkEvent event, String type) {
      if (event == null) {
        return;
      }

      Throwable e = event.getException();

      if (e == null) {
        return;
      }

      if (e.getCause() != null) {
        e = e.getCause();
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Work caused exception on '%s'. Work being executed was: %s", type, event.getWork().toString()));
      }

      if (e instanceof MessagingException || e instanceof ConnectionException) {
        sourceCallback.onSourceException(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onStop() {
    stopRequested.set(true);
    workManager.dispose();
    shutdownExecutor();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  private boolean isRequestedToStop() {
    return stopRequested.get() || Thread.currentThread().isInterrupted();
  }

  private void shutdownExecutor() {
    if (executorService == null) {
      return;
    }

    executorService.shutdownNow();
    try {
      if (!executorService.awaitTermination(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS)) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn("Could not properly terminate pending events for socket listener on flow " + flowConstruct.getName());
        }
      }
    } catch (InterruptedException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Got interrupted while trying to terminate pending events for socket listener on flow "
            + flowConstruct.getName());
      }
    }
  }

  private void listen(SourceCallback<InputStream, SocketAttributes> sourceCallback) {

    SocketWorkListener socketWorkListener = new SocketWorkListener(sourceCallback);
    for (;;) {
      if (isRequestedToStop()) {
        return;
      }

      try {
        SocketWorker worker = connection.listen(sourceCallback);
        worker.setEncoding(config.getDefaultEncoding());
        workManager.scheduleWork(worker, WorkManager.INDEFINITE, null, socketWorkListener);
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
