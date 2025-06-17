/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.exception;

import static java.lang.Boolean.TRUE;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;

/**
 * This is the base class for error handlers that are declared in an artifact.
 *
 * @since 4.5
 */
@NoExtend
public abstract class AbstractDeclaredExceptionListener extends AbstractMessageProcessorOwner {

  @Inject
  private NotificationDispatcher notificationFirer;

  private DefaultExceptionListener exceptionListener;

  private final List<Processor> messageProcessors = new CopyOnWriteArrayList<>();

  private final AtomicBoolean initialised = new AtomicBoolean(false);

  private String logException = TRUE.toString();
  private boolean enableNotifications = true;

  public List<Processor> getMessageProcessors() {
    return messageProcessors;
  }

  public void setMessageProcessors(List<Processor> processors) {
    if (processors != null) {
      this.messageProcessors.clear();
      this.messageProcessors.addAll(processors);
    } else {
      throw new IllegalArgumentException("List of targets = null");
    }
  }

  /**
   * The initialise method is call every time the Exception strategy is assigned to a service or connector. This implementation
   * ensures that initialise is called only once. The actual initialisation code is contained in the <code>doInitialise()</code>
   * method.
   *
   * @throws InitialisationException
   */
  @Override
  public synchronized void initialise() throws InitialisationException {
    if (!isInitialised()) {
      doInitialise();
      super.initialise();
      if (exceptionListener == null) {
        exceptionListener = new DefaultExceptionListener();
        exceptionListener.setNotificationFirer(notificationFirer);
      }
      exceptionListener.setRepresentation(this.getClass().getSimpleName()
          + (getLocation() != null ? " @ " + getLocation().getLocation() : ""));
      initialised.set(true);
    }
  }

  public boolean isInitialised() {
    return initialised.get();
  }

  protected void doInitialise() throws InitialisationException {
    logger.debug("Initialising exception listener: {}", this);
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return messageProcessors;
  }

  public NotificationDispatcher getNotificationFirer() {
    return notificationFirer;
  }

  public DefaultExceptionListener getExceptionListener() {
    return exceptionListener;
  }

  public void setExceptionListener(DefaultExceptionListener exceptionListener) {
    this.exceptionListener = exceptionListener;
  }

  public String getLogException() {
    return logException;
  }

  public void setLogException(String logException) {
    this.logException = logException;
  }

  public boolean getEnableNotifications() {
    return enableNotifications;
  }

  public void setEnableNotifications(boolean enableNotifications) {
    this.enableNotifications = enableNotifications;
  }

}
