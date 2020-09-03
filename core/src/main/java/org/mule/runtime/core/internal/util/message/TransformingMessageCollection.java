/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import org.mule.runtime.api.message.Message;

import java.util.Collection;
import java.util.function.Function;

/**
 * Specialization of {@link TransformingCollection} which outputs instances of {@link Message}
 * <p>
 * This allows to avoid preemptive transformations of an entire collection.
 *
 * @since 4.4.0
 */
abstract class TransformingMessageCollection extends TransformingCollection<Message> {

  public TransformingMessageCollection(Collection<Object> delegate, Function<Object, Message> transformer) {
    super(delegate, Message.class, transformer);
  }
}
