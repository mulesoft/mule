/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.client;

import static org.mule.functional.client.TestConnectorConfig.DEFAULT_CONFIG_ID;
import static org.mule.runtime.api.metadata.DataType.fromType;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.util.AttributeEvaluator;

/**
 * Writes {@link BaseEvent} to a test connector's queue.
 */
public class QueueWriterMessageProcessor extends AbstractComponent implements Processor, MuleContextAware, Initialisable {

  private MuleContext muleContext;
  private String name;
  private AttributeEvaluator attributeEvaluator;
  private String content;
  private Class contentJavaType;

  @Override
  public BaseEvent process(BaseEvent event) throws MuleException {
    TestConnectorConfig connectorConfig = muleContext.getRegistry().lookupObject(DEFAULT_CONFIG_ID);
    BaseEvent copy;
    if (attributeEvaluator == null) {
      copy = BaseEvent.builder(event)
          // Queue works based on MuleEvent for testing purposes. A real operation
          // would not be aware of the error field and just the plain message would be sent.
          .error(null)
          .build();
    } else {
      Object payloadValue = attributeEvaluator.resolveTypedValue(event).getValue();
      copy = BaseEvent.builder(event).message(Message.builder(event.getMessage()).value(payloadValue).build())
          // Queue works based on MuleEvent for testing purposes. A real operation
          // would not be aware of the error field and just the plain message would be sent.
          .error(null)
          .build();
    }

    connectorConfig.write(name, copy);

    return event;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    muleContext = context;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setContentJavaType(Class contentJavaType) {
    this.contentJavaType = contentJavaType;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (attributeEvaluator == null) {
      String attribute = content == null ? "#[payload]" : content;
      if (contentJavaType != null) {
        attributeEvaluator = new AttributeEvaluator(attribute, fromType(contentJavaType));
      } else {
        attributeEvaluator = new AttributeEvaluator(attribute);
      }
    }
    attributeEvaluator.initialize(muleContext.getExpressionManager());
  }
}
