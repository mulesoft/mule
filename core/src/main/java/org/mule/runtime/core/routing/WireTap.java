/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static java.util.Collections.singletonList;
import static org.mule.runtime.core.api.Event.setCurrentEvent;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.processor.AbstractFilteringMessageProcessor;
import org.mule.runtime.core.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.util.ObjectUtils;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>WireTap</code> MessageProcessor allows inspection of messages in a flow.
 * <p>
 * The incoming message is is sent to both the primary and wiretap outputs. The flow of the primary output will be unmodified and
 * a copy of the message used for the wiretap output.
 * <p>
 * An optional filter can be used to filter which message are sent to the wiretap output, this filter does not affect the flow to
 * the primary output. If there is an error sending to the wiretap output no exception will be thrown but rather an error logged.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/WireTap.html">http://www.eaipatterns.com/WireTap.html<a/>
 */
public class WireTap extends AbstractMessageProcessorOwner implements Processor {

  protected final transient Logger logger = LoggerFactory.getLogger(getClass());
  protected volatile Processor tap;
  protected volatile Filter filter;

  protected Processor filteredTap = new WireTapFilter();

  @Override
  public Event process(Event event) throws MuleException {
    if (tap == null) {
      return event;
    }

    try {
      // Tap should not respond to reply to handler
      Event tapEvent = Event.builder(event).replyToHandler(null).build();
      setCurrentEvent(tapEvent);
      filteredTap.process(tapEvent);
      setCurrentEvent(event);
    } catch (MuleException e) {
      logger.error("Exception sending to wiretap output " + tap, e);
    }

    return event;
  }

  public Processor getTap() {
    return tap;
  }

  public void setTap(Processor tap) {
    this.tap = tap;
  }

  @Deprecated
  public void setMessageProcessor(Processor tap) {
    setTap(tap);
  }

  public Filter getFilter() {
    return filter;
  }

  public void setFilter(Filter filter) {
    this.filter = filter;
  }

  private class WireTapFilter extends AbstractFilteringMessageProcessor {

    @Override
    protected boolean accept(Event event, Event.Builder builder) {
      if (filter == null) {
        return true;
      } else {
        return filter.accept(event, builder);
      }
    }

    @Override
    protected Event processNext(Event event) throws MuleException {
      if (tap != null) {
        tap.process(event);
      }
      return null;
    }

    @Override
    public String toString() {
      return ObjectUtils.toString(this);
    }
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return singletonList(tap);
  }

}
