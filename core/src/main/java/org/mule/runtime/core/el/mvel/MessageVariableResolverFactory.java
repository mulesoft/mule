/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.mvel;

import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.integration.VariableResolver;
import org.mule.mvel2.integration.VariableResolverFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.el.context.FlowVariableMapContext;
import org.mule.runtime.core.el.context.MessageContext;
import org.mule.runtime.core.el.context.SessionVariableMapContext;

import java.util.Map;

public class MessageVariableResolverFactory extends MuleBaseVariableResolverFactory {

  private static final long serialVersionUID = -6819292692339684915L;

  private static final String MESSAGE = "message";
  private static final String EXCEPTION = "exception";
  public static final String PAYLOAD = "payload";
  public static final String MESSAGE_PAYLOAD = MESSAGE + "." + PAYLOAD;
  public static final String FLOW_VARS = "flowVars";
  public static final String SESSION_VARS = "sessionVars";

  protected MuleEvent event;
  protected MuleEvent.Builder eventBuilder;
  protected MuleContext muleContext;

  // TODO MULE-10471 Immutable event used in MEL/Scripting should be shared for consistency
  public MessageVariableResolverFactory(final ParserConfiguration parserConfiguration, final MuleContext muleContext,
                                        final MuleEvent event, final MuleEvent.Builder eventBuilder) {
    this.event = event;
    this.eventBuilder = eventBuilder;
    this.muleContext = muleContext;
  }

  /**
   * Convenience constructor to allow for more concise creation of VariableResolverFactory chains without and performance overhead
   * incurred by using a builder.
   * 
   * @param next
   */
  public MessageVariableResolverFactory(final ParserConfiguration parserConfiguration, final MuleContext muleContext,
                                        final MuleEvent event, final MuleEvent.Builder eventBuilder,
                                        final VariableResolverFactory next) {
    this(parserConfiguration, muleContext, event, eventBuilder);
    setNextFactory(next);
  }

  @Override
  public boolean isTarget(String name) {
    return MESSAGE.equals(name) || PAYLOAD.equals(name) || FLOW_VARS.equals(name) || EXCEPTION.equals(name)
        || SESSION_VARS.equals(name) || MVELExpressionLanguageContext.MULE_MESSAGE_INTERNAL_VARIABLE.equals(name);
  }

  @Override
  public VariableResolver getVariableResolver(String name) {
    if (event != null) {
      if (MESSAGE.equals(name)) {
        return new MuleImmutableVariableResolver<>(MESSAGE, new MessageContext(event, eventBuilder, muleContext), null);
      } else if (PAYLOAD.equals(name)) {
        return new MuleVariableResolver<>(PAYLOAD, new MessageContext(event, eventBuilder, muleContext).getPayload(), null,
                                          (name1, value, newValue) -> eventBuilder
                                              .message(MuleMessage.builder(event.getMessage()).payload(newValue).build()));
      } else if (FLOW_VARS.equals(name)) {
        return new MuleImmutableVariableResolver<Map<String, Object>>(FLOW_VARS, new FlowVariableMapContext(event, eventBuilder),
                                                                      null);
      } else if (EXCEPTION.equals(name)) {
        if (event.getError().isPresent()) {
          return new MuleImmutableVariableResolver<>(EXCEPTION, event.getError().get().getException(), null);
        } else if (event.getMessage().getExceptionPayload() != null) {
          return new MuleImmutableVariableResolver<>(EXCEPTION, event.getMessage().getExceptionPayload().getException(), null);
        } else {
          return new MuleImmutableVariableResolver<MuleMessage>(EXCEPTION, null, null);
        }
      } else if (SESSION_VARS.equals(name)) {
        return new MuleImmutableVariableResolver<Map<String, Object>>(SESSION_VARS,
                                                                      new SessionVariableMapContext(event.getSession()), null);
      } else if (MVELExpressionLanguageContext.MULE_MESSAGE_INTERNAL_VARIABLE.equals(name)) {
        return new MuleImmutableVariableResolver<>(MVELExpressionLanguageContext.MULE_MESSAGE_INTERNAL_VARIABLE,
                                                   event.getMessage(), null);
      }
    }
    return super.getNextFactoryVariableResolver(name);
  }

}
