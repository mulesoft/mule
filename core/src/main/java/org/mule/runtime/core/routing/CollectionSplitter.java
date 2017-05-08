/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.routing.outbound.AbstractMessageSequenceSplitter;
import org.mule.runtime.core.util.collection.EventToMessageSequenceSplittingStrategy;
import org.mule.runtime.core.util.collection.SplittingStrategy;

/**
 * Splits a message that has a Collection, Iterable, MessageSequence or Iterator payload invoking the next message processor one
 * for each item in it.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Sequencer.html">http ://www.eaipatterns.com/Sequencer.html</a>
 */
public class CollectionSplitter extends AbstractMessageSequenceSplitter implements Initialisable {

  private SplittingStrategy<Event, MessageSequence<?>> strategy;

  protected MessageSequence<?> splitMessageIntoSequence(Event event) {
    return this.strategy.split(event);
  }

  @Override
  public void initialise() throws InitialisationException {
    strategy = new EventToMessageSequenceSplittingStrategy(new ExpressionSplittingStrategy(muleContext.getExpressionManager()));
  }
}
