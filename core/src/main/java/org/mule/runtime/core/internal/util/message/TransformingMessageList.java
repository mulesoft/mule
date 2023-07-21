/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.message;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.internal.util.collection.TransformingList;

import java.util.List;
import java.util.function.Function;

/**
 * Specialization of {@link TransformingList} which outputs instances of {@link Message}
 *
 * @since 4.4.0
 */
public final class TransformingMessageList extends TransformingList<Message> implements List<Message> {

  public TransformingMessageList(List<Object> delegate, Function<Object, Message> transformer) {
    super(delegate, transformer, Message.class);
  }
}
