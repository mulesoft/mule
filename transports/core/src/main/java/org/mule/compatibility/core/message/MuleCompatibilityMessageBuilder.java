/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.message;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.message.Correlation;
import org.mule.runtime.core.message.DefaultMuleMessageBuilder;

/**
 * Adds functionality that was available in {@link MuleMessage} in Mule 3.
 *
 * @since 4.0
 */
public class MuleCompatibilityMessageBuilder extends DefaultMuleMessageBuilder {

  private String correlationId;
  private Integer correlationSequence;
  private Integer correlationGroupSize;

  public MuleCompatibilityMessageBuilder() {
    super();
  }

  public MuleCompatibilityMessageBuilder(org.mule.runtime.api.message.MuleMessage message) {
    super(message);

    if (message instanceof MuleCompatibilityMessage) {
      correlationId = ((MuleCompatibilityMessage) message).getCorrelationId();
      correlationSequence = ((MuleCompatibilityMessage) message).getCorrelation().getSequence().orElse(null);
      correlationGroupSize = ((MuleCompatibilityMessage) message).getCorrelation().getGroupSize().orElse(null);
    }
  }

  @Override
  public MuleCompatibilityMessage build() {
    return new MuleCompatibilityMessage(super.build(), new Correlation(correlationGroupSize, correlationSequence), correlationId);
  }

  public MuleCompatibilityMessageBuilder correlationId(String correlationId) {
    this.correlationId = correlationId;
    return this;
  }

  public MuleCompatibilityMessageBuilder correlationSequence(Integer correlationSequence) {
    this.correlationSequence = correlationSequence;
    return this;
  }

  public MuleCompatibilityMessageBuilder correlationGroupSize(Integer correlationGroupSize) {
    this.correlationGroupSize = correlationGroupSize;
    return this;
  }
}
