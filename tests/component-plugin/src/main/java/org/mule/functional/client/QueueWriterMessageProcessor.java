/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.client;

import static org.mule.runtime.api.metadata.DataType.fromType;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.util.AttributeEvaluator;

import javax.inject.Inject;

/**
 * Writes {@link CoreEvent} to a test connector's queue.
 */
public class QueueWriterMessageProcessor extends AbstractComponent implements Processor, Initialisable, Startable, Stoppable {

  @Inject
  private ExtendedExpressionManager expressionManager;

  private volatile boolean started = false;

  private String name;
  private AttributeEvaluator attributeEvaluator;
  private String content;
  private Class contentJavaType;
  private TestConnectorQueueHandler queueHandler;

  @Inject
  private Registry registry;

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    if (!this.started) {
      throw new IllegalStateException("`test:queue` component not started.");
    }

    CoreEvent copy;
    if (attributeEvaluator == null) {
      copy = CoreEvent.builder(event)
          // Queue works based on MuleEvent for testing purposes. A real operation
          // would not be aware of the error field and just the plain message would be sent.
          .error(null)
          .build();
    } else {
      Object payloadValue = attributeEvaluator.resolveTypedValue(event).getValue();
      copy = CoreEvent.builder(event).message(Message.builder(event.getMessage()).value(payloadValue).build())
          // Queue works based on MuleEvent for testing purposes. A real operation
          // would not be aware of the error field and just the plain message would be sent.
          .error(null)
          .build();
    }

    queueHandler.write(name, copy);

    return event;
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
    attributeEvaluator.initialize(expressionManager);
    queueHandler = new TestConnectorQueueHandler(registry);
  }

  @Override
  public synchronized void start() throws MuleException {
    this.started = true;
  }

  @Override
  public synchronized void stop() throws MuleException {
    this.started = false;
  }

}
