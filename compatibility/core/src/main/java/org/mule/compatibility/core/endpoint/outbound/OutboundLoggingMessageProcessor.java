/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.outbound;

import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.util.ObjectUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutboundLoggingMessageProcessor extends AbstractAnnotatedObject implements Processor {

  protected final transient Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public Event process(Event event) throws MuleException {
    if (logger.isDebugEnabled()) {
      logger.debug("sending event: " + event);
    }

    return event;
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }
}
