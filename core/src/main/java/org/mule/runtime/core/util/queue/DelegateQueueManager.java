/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.queue;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delegates every call to the real QueueManager.
 *
 * If there's a system property with mule.queue.objectstoremode=true then the old version of the QueueManager will be used. This
 * is to maintain backward compatibility in case a customer is relaying on ObjectStore for queue store customization.
 */
public class DelegateQueueManager implements QueueManager, Lifecycle, MuleContextAware {

  public static final String MULE_QUEUE_OLD_MODE_KEY = "mule.queue.objectstoremode";

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  private QueueManager delegate;

  public DelegateQueueManager() {
    if (isOldModeEnabled()) {
      logger.info("Using old QueueManager implementation");
      delegate = new org.mule.runtime.core.util.queue.objectstore.TransactionalQueueManager();
    } else {
      delegate = new TransactionalQueueManager();
    }
  }

  @Override
  public QueueSession getQueueSession() {
    return delegate.getQueueSession();
  }

  @Override
  public void setDefaultQueueConfiguration(QueueConfiguration config) {
    delegate.setDefaultQueueConfiguration(config);
  }

  @Override
  public void setQueueConfiguration(String queueName, QueueConfiguration config) {
    delegate.setQueueConfiguration(queueName, config);
  }

  @Override
  public void start() throws MuleException {
    this.delegate.start();
  }

  @Override
  public void stop() throws MuleException {
    this.delegate.stop();
  }

  @Override
  public void dispose() {
    if (this.delegate instanceof Disposable) {
      ((Disposable) this.delegate).dispose();
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    if (this.delegate instanceof Initialisable) {
      ((Initialisable) this.delegate).initialise();
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {
    if (this.delegate instanceof MuleContextAware) {
      ((MuleContextAware) this.delegate).setMuleContext(context);
    }
  }

  public static boolean isOldModeEnabled() {
    return Boolean.getBoolean(MULE_QUEUE_OLD_MODE_KEY);
  }

  QueueManager getDelegate() {
    return delegate;
  }
}
