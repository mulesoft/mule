/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.routing.outbound.AbstractMessageSequenceSplitter;
import org.mule.runtime.core.routing.outbound.CollectionMessageSequence;

import java.util.List;

/**
 * Splits a message invoking the next message processor one for each split part. Implementations must implement
 * {@link #splitMessage(Event)} and determine how the message is split.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Sequencer.html">http://www .eaipatterns.com/Sequencer.html</a>
 */

public abstract class AbstractSplitter extends AbstractMessageSequenceSplitter {

  @Override
  @SuppressWarnings("unchecked")
  protected MessageSequence<?> splitMessageIntoSequence(Event event) throws MuleException {
    return new CollectionMessageSequence(splitMessage(event));
  }

  /**
   * Performs the split of the message payload in the current event, creating a new
   * {@link InternalMessage} and in turn a new {@link Event} for each split part.
   *
   * @param event the event continaing the {@link InternalMessage} whose payload is to be split.
   * @return a list of {@link Event}s each containing a new {@link InternalMessage} with a split part.
   * @throws MuleException if an error occurs doing spltting.
   */
  protected abstract List<Event> splitMessage(Event event) throws MuleException;

}
