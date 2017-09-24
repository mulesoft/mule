/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.simple;

import static org.mule.runtime.api.metadata.DataType.STRING;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.util.AttributeEvaluator;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.api.util.WildcardAttributeEvaluator;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractRemoveVariablePropertyProcessor extends SimpleMessageProcessor {

  private static final Logger logger = LoggerFactory.getLogger(AbstractRemoveVariablePropertyProcessor.class);

  private AttributeEvaluator identifierEvaluator;
  private WildcardAttributeEvaluator wildcardAttributeEvaluator;

  @Override
  public void initialise() throws InitialisationException {
    this.identifierEvaluator.initialize(muleContext.getExpressionManager());
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    if (wildcardAttributeEvaluator.hasWildcards()) {
      AtomicReference<CoreEvent> resultEvent = new AtomicReference<>(event);
      wildcardAttributeEvaluator.processValues(getPropertyNames((PrivilegedEvent) event), matchedValue -> {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Removing property: '%s' from scope: '%s'", matchedValue, getScopeName()));
        }
        resultEvent.set(removeProperty((PrivilegedEvent) event, matchedValue));
      });
      return resultEvent.get();
    } else {
      String key = identifierEvaluator.resolveValue(event);
      if (key != null) {
        return removeProperty((PrivilegedEvent) event, key);
      } else {
        logger.info("Key expression return null, no property will be removed");
        return event;
      }
    }
  }

  protected abstract Set<String> getPropertyNames(PrivilegedEvent event);

  protected abstract PrivilegedEvent removeProperty(PrivilegedEvent event, String propertyName);

  @Override
  public Object clone() throws CloneNotSupportedException {
    AbstractRemoveVariablePropertyProcessor clone = (AbstractRemoveVariablePropertyProcessor) super.clone();
    clone.setIdentifier(this.identifierEvaluator.getRawValue());
    return clone;
  }

  public void setIdentifier(String identifier) {
    if (StringUtils.isBlank(identifier)) {
      throw new IllegalArgumentException("Remove with null identifier is not supported");
    }
    this.identifierEvaluator = new AttributeEvaluator(identifier, STRING);
    this.wildcardAttributeEvaluator = new WildcardAttributeEvaluator(identifier);
  }

  protected abstract String getScopeName();
}
