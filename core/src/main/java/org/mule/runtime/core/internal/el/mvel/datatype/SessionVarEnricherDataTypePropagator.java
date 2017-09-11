/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.el.mvel.datatype;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.internal.el.mvel.MessageVariableResolverFactory;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Propagates data type for session vars used for enrichment target
 */
public class SessionVarEnricherDataTypePropagator extends AbstractVariableEnricherDataTypePropagator {

  public SessionVarEnricherDataTypePropagator() {
    super(MessageVariableResolverFactory.SESSION_VARS);
  }

  @Override
  protected void addVariable(PrivilegedEvent event, PrivilegedEvent.Builder builder, TypedValue typedValue, String propertyName) {
    event.getSession().setProperty(propertyName, typedValue.getValue(), typedValue.getDataType());
  }

  @Override
  protected boolean containsVariable(PrivilegedEvent event, String propertyName) {
    return event.getSession().getPropertyNamesAsSet().contains(propertyName);
  }

}
