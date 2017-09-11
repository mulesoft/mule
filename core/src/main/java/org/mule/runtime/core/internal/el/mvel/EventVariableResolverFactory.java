/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel;

import static org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguageContext.MULE_EVENT_INTERNAL_VARIABLE;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.integration.VariableResolver;
import org.mule.mvel2.integration.VariableResolverFactory;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import org.slf4j.Logger;

public class EventVariableResolverFactory extends MessageVariableResolverFactory {

  private static final Logger logger = getLogger(EventVariableResolverFactory.class);
  private static final long serialVersionUID = -6819292692339684915L;

  private final String FLOW = "flow";
  private String flowName;

  public EventVariableResolverFactory(ParserConfiguration parserConfiguration, MuleContext muleContext, PrivilegedEvent event,
                                      PrivilegedEvent.Builder eventBuilder, ComponentLocation componentLocation) {
    super(parserConfiguration, muleContext, event, eventBuilder);
    this.flowName = componentLocation != null ? componentLocation.getRootContainerName() : null;
  }

  /**
   * Convenience constructor to allow for more concise creation of VariableResolverFactory chains without and performance overhead
   * incurred by using a builder.
   */
  public EventVariableResolverFactory(ParserConfiguration parserConfiguration, MuleContext muleContext, PrivilegedEvent event,
                                      PrivilegedEvent.Builder eventBuilder, ComponentLocation componentLocation,
                                      VariableResolverFactory next) {
    this(parserConfiguration, muleContext, event, eventBuilder, componentLocation);
    setNextFactory(next);
  }

  @Override
  public VariableResolver getVariableResolver(String name) {
    if (event != null) {
      if (FLOW.equals(name) && flowName != null) {
        return new MuleImmutableVariableResolver<>(FLOW, (new FlowContext(flowName)), null);
      } else if (MULE_EVENT_INTERNAL_VARIABLE.equals(name)) {
        return new MuleImmutableVariableResolver<>(MULE_EVENT_INTERNAL_VARIABLE, event, null);
      }
    }
    return super.getVariableResolver(name);
  }

  @Override
  public boolean isTarget(String name) {
    boolean isDeprecatedVariable = MULE_EVENT_INTERNAL_VARIABLE.equals(name);
    if (isDeprecatedVariable) {
      logger.warn(String.format("Variable %s has been removed from MEL and will not work outside of compatibility mode.",
                                MULE_EVENT_INTERNAL_VARIABLE));
    }
    return isDeprecatedVariable || FLOW.equals(name) || super.isTarget(name);
  }

  public static class FlowContext {

    private final String flowConstructName;

    public FlowContext(String flowConstructName) {
      this.flowConstructName = flowConstructName;
    }

    public String getName() {
      return flowConstructName;
    }
  }
}
