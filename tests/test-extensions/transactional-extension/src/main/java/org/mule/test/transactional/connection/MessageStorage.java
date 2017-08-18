/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactional.connection;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageStorage implements Processor {

  public static Queue<TestTransactionalConnection> messages = new ConcurrentLinkedQueue<>();

  public static Throwable exception;

  @Override
  public InternalEvent process(InternalEvent event) throws MuleException {
    event.getError().ifPresent(theError -> exception = theError.getCause());
    TypedValue<Object> payload = event.getMessage().getPayload();
    if (payload.getValue() != null) {
      messages.add((TestTransactionalConnection) payload.getValue());
    }
    return event;
  }

  public static void clean() {
    exception = null;
    messages = new ConcurrentLinkedQueue<>();
  }
}
