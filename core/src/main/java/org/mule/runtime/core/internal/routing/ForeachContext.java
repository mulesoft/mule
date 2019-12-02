/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;

import java.util.Iterator;

public class ForeachContext {

  Object previousCounter;
  Object previousRootMessage;
  Message originalMessage;
  Iterator<TypedValue<?>> iterator;


  public ForeachContext(Object previousCounter, Object previousRootMessage, Message message, Iterator<TypedValue<?>> iterator) {
    this.previousCounter = previousCounter;
    this.previousRootMessage = previousRootMessage;
    this.originalMessage = message;
    this.iterator = iterator;
  }

  public Object getPreviousCounter() {
    return previousCounter;
  }

  public Object getPreviousRootMessage() {
    return previousRootMessage;
  }

  public Message getOriginalMessage() {
    return originalMessage;
  }

  public Iterator<TypedValue<?>> getIterator() {
    return iterator;
  }
}
