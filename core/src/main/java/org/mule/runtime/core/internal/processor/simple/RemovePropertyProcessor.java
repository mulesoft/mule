/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.simple;

import org.mule.runtime.core.PropertyScope;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.processor.simple.AbstractRemoveVariablePropertyProcessor;

import java.util.Set;

public class RemovePropertyProcessor extends AbstractRemoveVariablePropertyProcessor {

  @Override
  protected Event removeProperty(Event event, String propertyName) {
    return Event.builder(event).message(InternalMessage.builder(event.getMessage()).removeOutboundProperty(propertyName).build())
        .build();
  }

  @Override
  protected Set<String> getPropertyNames(Event event) {
    return ((InternalMessage) event.getMessage()).getOutboundPropertyNames();
  }

  @Override
  protected String getScopeName() {
    return PropertyScope.OUTBOUND_NAME;
  }

}
