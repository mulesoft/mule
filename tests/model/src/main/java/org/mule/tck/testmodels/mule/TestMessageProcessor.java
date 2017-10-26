/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.meta.NameableObject;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.ObjectUtils;

public class TestMessageProcessor extends AbstractComponent implements Processor, NameableObject {

  /** Simple label string to be appended to the payload. */
  private String label;

  /** Bean name used by Spring */
  private String name;

  public TestMessageProcessor() {
    // For IoC
  }

  public TestMessageProcessor(String label) {
    this.label = label;
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    if (event != null && event.getMessage() != null) {
      return CoreEvent.builder(event)
          .message(Message.builder(event.getMessage()).value(event.getMessage().getPayload().getValue() + ":" + label)
              .build())
          .build();
    }
    return event;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }
}


