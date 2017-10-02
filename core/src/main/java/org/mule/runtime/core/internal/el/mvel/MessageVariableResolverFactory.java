/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel;

import static org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguageContext.MULE_MESSAGE_INTERNAL_VARIABLE;

import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.integration.VariableResolver;
import org.mule.mvel2.integration.VariableResolverFactory;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.el.context.MessageContext;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.el.context.EventVariablesMapContext;
import org.mule.runtime.core.privileged.el.context.SessionVariableMapContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import java.util.Map;

public class MessageVariableResolverFactory extends MuleBaseVariableResolverFactory {

  private static final long serialVersionUID = -6819292692339684915L;

  private static final String MESSAGE = "message";
  private static final String EXCEPTION = "exception";
  private static final String ERROR = "error";
  public static final String PAYLOAD = "payload";
  public static final String ATTRIBUTES = "attributes";
  public static final String MESSAGE_PAYLOAD = MESSAGE + "." + PAYLOAD;
  public static final String FLOW_VARS = "flowVars";
  public static final String SESSION_VARS = "sessionVars";

  protected PrivilegedEvent event;
  protected PrivilegedEvent.Builder eventBuilder;
  protected MuleContext muleContext;

  // TODO MULE-10471 Immutable event used in MEL/Scripting should be shared for consistency
  public MessageVariableResolverFactory(final ParserConfiguration parserConfiguration, final MuleContext muleContext,
                                        final PrivilegedEvent event, final PrivilegedEvent.Builder eventBuilder) {
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
                                        final PrivilegedEvent event, final PrivilegedEvent.Builder eventBuilder,
                                        final VariableResolverFactory next) {
    this(parserConfiguration, muleContext, event, eventBuilder);
    setNextFactory(next);

  }

  @Override
  public boolean isTarget(String name) {
    return MESSAGE.equals(name) || PAYLOAD.equals(name) || ATTRIBUTES.equals(name) || FLOW_VARS.equals(name)
        || EXCEPTION.equals(name) || ERROR.equals(name) || SESSION_VARS.equals(name)
        || MVELExpressionLanguageContext.MULE_MESSAGE_INTERNAL_VARIABLE.equals(name);
  }

  @Override
  public VariableResolver getVariableResolver(String name) {
    if (event != null) {
      if (MESSAGE.equals(name)) {
        return new MuleImmutableVariableResolver<>(MESSAGE, new MessageContext(event, eventBuilder, muleContext), null);
      } else if (PAYLOAD.equals(name)) {
        return new MuleVariableResolver<>(PAYLOAD, new MessageContext(event, eventBuilder, muleContext).getPayload(), null,
                                          (name1, value, newValue) -> eventBuilder
                                              .message(Message.builder(event.getMessage()).value(newValue).build()));
      } else if (ATTRIBUTES.equals(name)) {
        return new MuleImmutableVariableResolver<>(ATTRIBUTES, event.getMessage().getAttributes().getValue(), null);
      } else if (FLOW_VARS.equals(name)) {
        return new MuleImmutableVariableResolver<Map<String, Object>>(FLOW_VARS,
                                                                      new EventVariablesMapContext(event, eventBuilder),
                                                                      null);
      } else if (EXCEPTION.equals(name)) {
        if (event.getError().isPresent()) {
          Throwable exception = event.getError().get().getCause();
          return new MuleImmutableVariableResolver<>(EXCEPTION, wrapIfNecessary(event, exception), null);
        } else if (((InternalMessage) event.getMessage()).getExceptionPayload() != null) {
          Throwable exception = ((InternalMessage) event.getMessage()).getExceptionPayload().getException();
          return new MuleImmutableVariableResolver<>(EXCEPTION, wrapIfNecessary(event, exception), null);
        } else {
          return new MuleImmutableVariableResolver<Message>(EXCEPTION, null, null);
        }
      } else if (ERROR.equals(name)) {
        if (event.getError().isPresent()) {
          return new MuleImmutableVariableResolver<>(ERROR, event.getError().get(), null);
        } else {
          return new MuleImmutableVariableResolver<>(ERROR, null, null);
        }
      } else if (SESSION_VARS.equals(name)) {
        return new MuleImmutableVariableResolver<Map<String, Object>>(SESSION_VARS,
                                                                      new SessionVariableMapContext(event
                                                                          .getSession()),
                                                                      null);
      } else if (MULE_MESSAGE_INTERNAL_VARIABLE.equals(name)) {
        return new MuleImmutableVariableResolver<>(MULE_MESSAGE_INTERNAL_VARIABLE,
                                                   event.getMessage(), null);
      }
    }
    return super.getNextFactoryVariableResolver(name);
  }

  private MessagingException wrapIfNecessary(CoreEvent event, Throwable exception) {
    if (exception instanceof MessagingException) {
      return (MessagingException) exception;
    } else {
      return new MessagingException(event, exception);
    }
  }

}
