/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Optional.empty;

class ForeachContext {

  private Object previousCounter;
  private Object previousRootMessage;
  private Message originalMessage;
  private Optional<ItemSequenceInfo> itemSequenceInfo;
  private Iterator<TypedValue<?>> iterator;
  private AtomicInteger elementNumber = new AtomicInteger();
  private Optional<DataType> batchDataType = empty();
  private Optional<Runnable> onComplete = empty();

  ForeachContext(Object previousCounter, Object previousRootMessage, Message message, Optional<ItemSequenceInfo> itemSequenceInfo) {
    this.previousCounter = previousCounter;
    this.previousRootMessage = previousRootMessage;
    this.originalMessage = message;
    this.itemSequenceInfo = itemSequenceInfo;
  }

  Object getPreviousCounter() {
    return previousCounter;
  }

  Object getPreviousRootMessage() {
    return previousRootMessage;
  }

  Message getOriginalMessage() {
    return originalMessage;
  }

  public Iterator<TypedValue<?>> getIterator() {
    return iterator;
  }

  public void setIterator(Iterator<TypedValue<?>> iterator) {
    this.iterator = iterator;
  }

  AtomicInteger getElementNumber() {
    return elementNumber;
  }

  Optional<DataType> getBatchDataType() {
    return batchDataType;
  }

  void setBatchDataType(Optional<DataType> batchDataType) {
    this.batchDataType = batchDataType;
  }

  public Optional<Runnable> getOnComplete() {
    return onComplete;
  }

  public void setOnComplete(Runnable onCompleteConsumer) {
    this.onComplete = Optional.of(onCompleteConsumer);
  }

  public Optional<ItemSequenceInfo> getItemSequenceInfo() {
    return itemSequenceInfo;
  }
}
