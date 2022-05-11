/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.simple;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.processor.simple.AbstractRemoveVariablePropertyProcessor;

import java.util.Set;

import org.slf4j.Logger;

public class RemoveFlowVariableProcessor extends AbstractRemoveVariablePropertyProcessor {

  private static final String FLOW_VAR_NAME = "flow variables";
  private static final Logger LOGGER = getLogger(RemoveFlowVariableProcessor.class);

  @Override
  protected Set<String> getPropertyNames(PrivilegedEvent event) {
    return event.getVariables().keySet();
  }

  @Override
  protected PrivilegedEvent removeProperty(PrivilegedEvent event, String propertyName) {
    if (event.getVariables().containsKey(propertyName)) {
      return PrivilegedEvent.builder(event).removeVariable(propertyName).build();
    } else {
      LOGGER.warn("There is no variable named '{}'. Check the 'variableName' parameter in the 'remove-variable' component at {}",
                  propertyName, this.getLocation().getLocation());
      return event;
    }
  }

  @Override
  protected String getScopeName() {
    return FLOW_VAR_NAME;
  }
}
