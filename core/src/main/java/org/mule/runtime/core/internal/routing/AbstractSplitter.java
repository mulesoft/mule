/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.routing.outbound.AbstractMessageSequenceSplitter;
import org.mule.runtime.core.internal.routing.outbound.CollectionMessageSequence;

import java.util.List;

/**
 * Splits a message invoking the next message processor one for each split part. Implementations must implement
 * {@link #splitMessage(CoreEvent)} and determine how the message is split.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Sequencer.html">http://www .eaipatterns.com/Sequencer.html</a>
 */

public abstract class AbstractSplitter extends AbstractMessageSequenceSplitter {

  @Override
  @SuppressWarnings("unchecked")
  protected MessageSequence<?> splitMessageIntoSequence(CoreEvent event) throws MuleException {
    return new CollectionMessageSequence(splitMessage(event));
  }

  /**
   * Performs the split of the message payload in the current event.
   *
   * @param event the event to be split.
   * @return a list of values with the result of the split.
   * @throws MuleException if an error occurs doing splitting.
   */
  protected abstract List<?> splitMessage(CoreEvent event) throws MuleException;

}
