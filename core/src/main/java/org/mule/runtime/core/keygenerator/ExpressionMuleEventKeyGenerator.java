/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.keygenerator;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleEventKeyGenerator;

import java.io.NotSerializableException;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link org.mule.runtime.core.api.MuleEventKeyGenerator} using the Mule expression language to generate the cache
 * keys.
 */
public class ExpressionMuleEventKeyGenerator implements MuleEventKeyGenerator {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  private String expression;

  public Serializable generateKey(MuleEvent event) throws NotSerializableException {
    Object key = event.getMuleContext().getExpressionManager().evaluate(expression, event);

    if (logger.isDebugEnabled()) {
      logger.debug("Generated key for event: " + event + " key: " + key);
    }

    if (key instanceof Serializable) {
      return (Serializable) key;
    } else {
      throw new NotSerializableException("Generated key must a serializable object but was "
          + (key != null ? key.getClass().getName() : "null"));
    }
  }

  public String getExpression() {
    return expression;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }
}
