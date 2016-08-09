/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.core.PropertyScope;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;

import java.util.Set;

public class RemovePropertyTransformer extends AbstractRemoveVariablePropertyTransformer {

  @Override
  protected void removeProperty(MuleEvent event, String propertyName) {
    event.setMessage(MuleMessage.builder(event.getMessage()).removeOutboundProperty(propertyName).build());
  }

  @Override
  protected Set<String> getPropertyNames(MuleEvent event) {
    return event.getMessage().getOutboundPropertyNames();
  }

  @Override
  protected String getScopeName() {
    return PropertyScope.OUTBOUND_NAME;
  }

}
