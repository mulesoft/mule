/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.util.AttributeEvaluator;
import org.mule.runtime.core.util.WildcardAttributeEvaluator;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyPropertiesProcessor implements MessageProcessor, MuleContextAware, Initialisable {

  private static final Logger logger = LoggerFactory.getLogger(CopyPropertiesProcessor.class);

  private AttributeEvaluator propertyNameEvaluator;
  private WildcardAttributeEvaluator wildcardPropertyNameEvaluator;
  private MuleContext muleContext;

  @Override
  public void initialise() throws InitialisationException {
    this.propertyNameEvaluator.initialize(muleContext.getExpressionLanguage());
  }

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    final MuleEvent.Builder resultBuilder = MuleEvent.builder(event);
    MuleMessage message = event.getMessage();
    if (wildcardPropertyNameEvaluator.hasWildcards()) {
      final Builder builder = MuleMessage.builder(message);
      wildcardPropertyNameEvaluator
          .processValues(message.getInboundPropertyNames(),
                         matchedValue -> builder.addOutboundProperty(matchedValue, message.getInboundProperty(matchedValue),
                                                                     message.getInboundPropertyDataType(matchedValue)));
      resultBuilder.message(builder.build());
    } else {
      Object keyValue = propertyNameEvaluator.resolveValue(event);
      if (keyValue != null) {
        String propertyName = keyValue.toString();
        Serializable propertyValue = message.getInboundProperty(propertyName);
        if (propertyValue != null) {
          resultBuilder.message(MuleMessage.builder(message)
              .addOutboundProperty(propertyName, propertyValue, message.getInboundPropertyDataType(propertyName)).build());
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
