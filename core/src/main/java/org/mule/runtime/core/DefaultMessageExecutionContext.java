/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.util.Optional.ofNullable;

import org.mule.runtime.core.api.MessageExecutionContext;

import java.io.Serializable;
import java.util.Date;
import java.util.Optional;

/**
 * Default immutable implementation of {@link MessageExecutionContext}.
 *
 * @since 4.0
 */
public class DefaultMessageExecutionContext implements MessageExecutionContext, Serializable {

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

  public DefaultMessageExecutionContext(String id, String sourceCorrelationId) {
    this.id = id;
    this.sourceCorrelationId = sourceCorrelationId;
  }
}
