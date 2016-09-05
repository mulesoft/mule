/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.simple;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.util.AttributeEvaluator;
import org.mule.runtime.core.util.WildcardAttributeEvaluator;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRemoveVariablePropertyProcessor extends SimpleMessageProcessor {

  private static final Logger logger = LoggerFactory.getLogger(AbstractRemoveVariablePropertyProcessor.class);

  private AttributeEvaluator identifierEvaluator;
  private WildcardAttributeEvaluator wildcardAttributeEvaluator;

  @Override
  public void initialise() throws InitialisationException {
    this.identifierEvaluator.initialize(muleContext.getExpressionLanguage());
  }

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    if (wildcardAttributeEvaluator.hasWildcards()) {
      AtomicReference<MuleEvent> resultEvent = new AtomicReference<>(event);
      wildcardAttributeEvaluator.processValues(getPropertyNames(event), matchedValue -> {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Removing property: '%s' from scope: '%s'", matchedValue, getScopeName()));
        }
        resultEvent.set(removeProperty(event, matchedValue));
      });
      return resultEvent.get();
    } else {
      Object keyValue = identifierEvaluator.resolveValue(event);
      if (keyValue != null) {
        return removeProperty(event, keyValue.toString());
      } else {
        logger.info("Key expression return null, no property will be removed");
        return event;
      }
    }
  }

  protected abstract Set<String> getPropertyNames(MuleEvent event);

  protected abstract MuleEvent removeProperty(MuleEvent event, String propertyName);

  @Override
  public Object clone() throws CloneNotSupportedException {
    AbstractRemoveVariablePropertyProcessor clone = (AbstractRemoveVariablePropertyProcessor) super.clone();
    clone.setIdentifier(this.identifierEvaluator.getRawValue());
    return clone;
  }

  public void setIdentifier(String identifier) {
    if (identifier == null) {
      throw new IllegalArgumentException("Remove with null identifier is not supported");
    }
    this.identifierEvaluator = new AttributeEvaluator(identifier);
    this.wildcardAttributeEvaluator = new WildcardAttributeEvaluator(identifier);
  }

  protected abstract String getScopeName();
}
