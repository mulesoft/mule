/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Optional.empty;

import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

class ForeachContext {

  private Object previousCounter;
  private Object previousRootMessage;
  private Message originalMessage;
  private Optional<ItemSequenceInfo> itemSequenceInfo;
  private Iterator<TypedValue<?>> iterator;
  private AtomicInteger elementNumber = new AtomicInteger();
  private Optional<DataType> batchDataType = empty();
  private Optional<Runnable> onComplete = empty();

  ForeachContext(Object previousCounter, Object previousRootMessage, Message message,
                 Optional<ItemSequenceInfo> itemSequenceInfo, Iterator<TypedValue<?>> iterator) {
    this.previousCounter = previousCounter;
    this.previousRootMessage = previousRootMessage;
    this.originalMessage = message;
    this.itemSequenceInfo = itemSequenceInfo;
    this.iterator = iterator;
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
