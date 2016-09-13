/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.message;

import org.mule.runtime.core.message.DefaultMessageBuilder;
import org.mule.runtime.core.message.GroupCorrelation;

/**
 * Adds functionality that was available in {@link Message} in Mule 3.
 *
 * @since 4.0
 */
public class MuleCompatibilityMessageBuilder extends DefaultMessageBuilder {

  private String correlationId;
  private Integer correlationSequence;
  private Integer correlationGroupSize;

  public MuleCompatibilityMessageBuilder() {
    super();
  }

  public MuleCompatibilityMessageBuilder(org.mule.runtime.api.message.Message message) {
    super(message);

    if (message instanceof CompatibilityMessage) {
      correlationId = ((CompatibilityMessage) message).getCorrelationId();
      correlationSequence = ((CompatibilityMessage) message).getCorrelation().getSequence().orElse(null);
      correlationGroupSize = ((CompatibilityMessage) message).getCorrelation().getGroupSize().orElse(null);
    }
  }

  @Override
  public CompatibilityMessage build() {
    return new CompatibilityMessage(super.build(), new GroupCorrelation(correlationGroupSize, correlationSequence),
                                    correlationId);
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
