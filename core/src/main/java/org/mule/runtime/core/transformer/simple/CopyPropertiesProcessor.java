/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.internal.message.InternalMessage.Builder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.AttributeEvaluator;
import org.mule.runtime.core.api.util.WildcardAttributeEvaluator;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyPropertiesProcessor extends AbstractAnnotatedObject implements Processor, MuleContextAware, Initialisable {

  private static final Logger logger = LoggerFactory.getLogger(CopyPropertiesProcessor.class);

  private AttributeEvaluator propertyNameEvaluator;
  private WildcardAttributeEvaluator wildcardPropertyNameEvaluator;
  private MuleContext muleContext;

  @Override
  public void initialise() throws InitialisationException {
    this.propertyNameEvaluator.initialize(muleContext.getExpressionManager());
  }

  @Override
  public Event process(Event event) throws MuleException {
    final Event.Builder resultBuilder = Event.builder(event);
    Message message = event.getMessage();
    if (wildcardPropertyNameEvaluator.hasWildcards()) {
      final Builder builder = InternalMessage.builder(message);
      wildcardPropertyNameEvaluator
          .processValues(((InternalMessage) message).getInboundPropertyNames(),
                         matchedValue -> builder.addOutboundProperty(matchedValue,
                                                                     ((InternalMessage) message).getInboundProperty(matchedValue),
                                                                     ((InternalMessage) message)
                                                                         .getInboundPropertyDataType(matchedValue)));
      resultBuilder.message(builder.build());
    } else {
      Object keyValue = propertyNameEvaluator.resolveValue(event);
      if (keyValue != null) {
        String propertyName = keyValue.toString();
        Serializable propertyValue = ((InternalMessage) message).getInboundProperty(propertyName);
        if (propertyValue != null) {
          resultBuilder.message(InternalMessage.builder(message)
              .addOutboundProperty(propertyName, propertyValue,
                                   ((InternalMessage) message).getInboundPropertyDataType(propertyName))
              .build());
        } else {
          logger.info("Property value for is null, no property will be copied");
        }
      } else {
        logger.info("Key expression return null, no property will be copied");
      }
    }
    return resultBuilder.build();
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    CopyPropertiesProcessor clone = (CopyPropertiesProcessor) super.clone();
    clone.setPropertyName(this.propertyNameEvaluator.getRawValue());
    return clone;
  }

  public void setPropertyName(String propertyName) {
    if (propertyName == null) {
      throw new IllegalArgumentException("Null propertyName not supported");
    }
    this.propertyNameEvaluator = new AttributeEvaluator(propertyName);
    this.wildcardPropertyNameEvaluator = new WildcardAttributeEvaluator(propertyName);
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
