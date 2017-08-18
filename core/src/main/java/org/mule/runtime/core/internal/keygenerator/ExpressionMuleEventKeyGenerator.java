/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.keygenerator;

import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEventKeyGenerator;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.util.AttributeEvaluator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link org.mule.runtime.core.api.MuleEventKeyGenerator} using the Mule expression language to generate the cache
 * keys.
 */
public class ExpressionMuleEventKeyGenerator implements MuleEventKeyGenerator, MuleContextAware {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  private AttributeEvaluator attributeEvaluator;

  @Override
  public String generateKey(InternalEvent event) {
    Object key = attributeEvaluator.resolveValue(event);

    if (logger.isDebugEnabled()) {
      logger.debug("Generated key for event: " + event + " key: " + key);
    }

    if (key instanceof String) {
      return (String) key;
    } else {
      throw new IllegalStateException("Generated key must a String but was " + (key != null ? key.getClass().getName() : "null"));
    }
  }

  public String getExpression() {
    return this.attributeEvaluator.getRawValue();
  }

  public void setExpression(String expression) {
    this.attributeEvaluator = new AttributeEvaluator(expression);
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    attributeEvaluator.initialize(muleContext.getExpressionManager());
  }
}
