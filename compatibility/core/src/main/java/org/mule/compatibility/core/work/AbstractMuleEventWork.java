/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.work;

import static org.mule.runtime.core.api.Event.setCurrentEvent;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.session.DefaultMuleSession;

import javax.resource.spi.work.Work;

/**
 * Abstract implementation of Work to be used whenever Work needs to be scheduled that operates on a MuleEvent. The abstract
 * implementation ensures that a copy of MuleEvent is used and that this copy is available in the RequestContext for this new
 * thread. Implementations of AbstractMuleEventWork should be run/scheduled only once. NOTE: This approach does not attempt to
 * resolve MULE-4409 so this work may need to be reverted to correctly fix MULE-4409 in future releases.
 */
public abstract class AbstractMuleEventWork implements Work {

  protected Event event;

  public AbstractMuleEventWork(Event event) {
    // Event must be copied here rather than once work is executed, so main flow can't mutate the message
    // before work execution
    this(event, true);
  }

  /**
   * Constructor allowing event copying to be disabled. This is used when a copy has already been made previously e.g. if the
   * event is queued before being processed asynchronously like with
   */
  public AbstractMuleEventWork(Event event, boolean copyEvent) {
    this.event = copyEvent ? Event.builder(event).session(new DefaultMuleSession(event.getSession())).build() : event;
  }

  @Override
  public final void run() {
    try {
      // Set event in RequestContext now we are in new thread (fresh copy already made in constructor)
      setCurrentEvent(event);
      doRun();
    } finally {
      setCurrentEvent(null);
    }
  }

  protected abstract void doRun();

  @Override
  public void release() {
    // no-op
  }

  public Event getEvent() {
    return event;
  }
}
