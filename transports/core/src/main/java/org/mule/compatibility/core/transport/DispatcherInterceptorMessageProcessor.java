/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport;

import static java.util.Objects.requireNonNull;
import static javax.resource.spi.work.WorkManager.INDEFINITE;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.context.WorkManagerSource;
import org.mule.runtime.core.processor.AsyncWorkListener;
import org.mule.runtime.core.processor.BaseAsyncInterceptingMessageProcessor;

import java.util.concurrent.Executor;

import javax.resource.spi.work.Work;

public class DispatcherInterceptorMessageProcessor extends BaseAsyncInterceptingMessageProcessor {

  public static final String SYNCHRONOUS_EVENT_ERROR_MESSAGE = "Unable to process a synchronous event asynchronously";

  protected WorkManagerSource workManagerSource;

  public DispatcherInterceptorMessageProcessor(WorkManagerSource workManagerSource) {
    requireNonNull(workManagerSource);
    this.workManagerSource = workManagerSource;
  }

  @Override
  protected boolean isProcessAsync(Event event) throws MuleException {
    return canProcessAsync(event);
  }

  @Override
  protected void doProcessNextAsync(Work work) throws Exception {
    workManagerSource.getWorkManager().scheduleWork(work, INDEFINITE, null, new AsyncWorkListener(next));
  }

  @Override
  protected Executor resolveExecutor() throws MuleException {
    return workManagerSource.getWorkManager();
  }
}
