/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.processor;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;

import java.util.concurrent.Executor;

/**
 * Test non-blocking {@link Processor} implementation that simply uses a {@link Executor} to invoke the
 * {@link org.mule.runtime.core.api.connector.ReplyToHandler} in another thread.
 */
public class TestNonBlockingProcessor implements Processor {

  /**
   * Force the proactor to change the thread.
   */
  @Override
  public ProcessingType getProcessingType() {
    return CPU_INTENSIVE;
  }

  @Override
  public Event process(final Event event) throws MuleException {
    return event;
  }

}
