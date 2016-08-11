/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.util.Optional.ofNullable;

import org.mule.runtime.core.api.MessageExecutionContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.source.MessageSource;

import java.io.Serializable;
import java.util.Date;
import java.util.Optional;

/**
 * Default immutable implementation of {@link MessageExecutionContext}.
 *
 * @since 4.0
 */
public class DefaultMessageExecutionContext implements MessageExecutionContext, Serializable {

  private static final long serialVersionUID = -3664490832964509653L;

  private final String id;
  private final String sourceCorrelationId;
  private final Date receivedDate = new Date();

  @Override
  public String getId() {
    return id;
  }

  @Override
  public Optional<String> getSourceCorrelationId() {
    return ofNullable(sourceCorrelationId);
  }

  @Override
  public Date getReceivedDate() {
    return receivedDate;
  }

  /**
   * Builds a new execution context with the given parameters.
   * 
   * @param id the unique id that identifies all {@link MuleEvent}s of the same context.
   * @param sourceCorrelationId the correlation id that was set by the {@link MessageSource} for the first {@link MuleEvent} of
   *        this context, if available.
   */
  public DefaultMessageExecutionContext(String id, String sourceCorrelationId) {
    this.id = id;
    this.sourceCorrelationId = sourceCorrelationId;
  }

  @Override
  public String toString() {
    return "DefaultMessageExecutionContext { id: " + id + "; sourceCorrelationId: " + sourceCorrelationId + " }";
  }
}
