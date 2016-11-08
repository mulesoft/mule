/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.mvel;

import static java.lang.String.format;
import static org.mule.runtime.core.el.mvel.MVELExpressionLanguageContext.MULE_EVENT_INTERNAL_VARIABLE;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.integration.VariableResolver;
import org.mule.mvel2.integration.VariableResolverFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;

import org.slf4j.Logger;

public class EventVariableResolverFactory extends MessageVariableResolverFactory {

  private static final Logger logger = getLogger(EventVariableResolverFactory.class);
  private static final long serialVersionUID = -6819292692339684915L;

  private final String FLOW = "flow";
  private FlowConstruct flowConstruct;

  public EventVariableResolverFactory(ParserConfiguration parserConfiguration, MuleContext muleContext, Event event,
                                      Event.Builder eventBuilder, FlowConstruct flowConstruct) {
    super(parserConfiguration, muleContext, event, eventBuilder);
    this.flowConstruct = flowConstruct;
  }

  /**
   * Convenience constructor to allow for more concise creation of VariableResolverFactory chains without and performance overhead
   * incurred by using a builder.
   * 
   * @param next
   */
  public EventVariableResolverFactory(ParserConfiguration parserConfiguration, MuleContext muleContext, Event event,
                                      Event.Builder eventBuilder, FlowConstruct flowConstruct, VariableResolverFactory next) {
    this(parserConfiguration, muleContext, event, eventBuilder, flowConstruct);
    setNextFactory(next);
  }

  @Override
  public VariableResolver getVariableResolver(String name) {
    if (event != null) {
      if (FLOW.equals(name) && flowConstruct != null) {
        return new MuleImmutableVariableResolver<>(FLOW, (new FlowContext(flowConstruct.getName())), null);
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
      logger.warn(format("Variable %s has been removed from MEL and will not work outside of compatibility mode.",
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
