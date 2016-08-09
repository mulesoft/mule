/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.filters;

import static org.mule.runtime.core.util.ClassUtils.isConsumable;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.routing.filter.Filter;

/**
 * Filters messages that have a consumable payload.
 * <p/>
 * The filter accepts only {@link MuleMessage} instances that have a no consumable payload. Check is done using
 * {@link org.mule.runtime.core.util.ClassUtils#isConsumable()} method.
 */
public class ConsumableMuleMessageFilter implements Filter {

  @Override
  public boolean accept(MuleMessage message) {
    return !isConsumable(message.getDataType().getType());
  }
}
