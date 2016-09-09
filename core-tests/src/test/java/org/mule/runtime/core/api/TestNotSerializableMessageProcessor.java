/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.Processor;

public class TestNotSerializableMessageProcessor implements Processor {

  public TestNotSerializableMessageProcessor() {
    super();
  }

  @Override
  public Event process(Event event) throws MuleException {
    return event;
  }
}
