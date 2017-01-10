/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.message;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Set;

/**
 * Wraps a {@link Set} of {@link Result} instances and exposes
 * its contents as {@link Message} instances.
 *
 * This allows to avoid preemptive transformations of an entire Set
 * of {@link Result} to {@link Message}
 *
 * @since 4.0
 */
public final class ResultsToMessageSet extends ResultsToMessageCollection implements Set<Message> {

  public ResultsToMessageSet(Set<Result> delegate, MediaType mediaType) {
    super(delegate, mediaType);
  }
}
