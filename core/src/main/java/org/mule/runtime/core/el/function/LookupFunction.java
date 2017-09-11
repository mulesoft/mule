/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.function;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_MAP;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.el.BindingContextUtils.ERROR;
import static org.mule.runtime.api.el.BindingContextUtils.MESSAGE;
import static org.mule.runtime.api.el.BindingContextUtils.VARS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.core.api.processor.MessageProcessors.processWithChildContext;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static reactor.core.publisher.Mono.from;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionFunction;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.FunctionParameter;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;

import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Implementation of lookup("myFlow", payload), a function which executes the desired flow with the specified payload and returns
 * its result. For now, only Java payloads will be supported.
 *
 * @since 4.0
 */
public class LookupFunction implements ExpressionFunction {

  private static final DataType TYPED_VALUE = fromType(TypedValue.class);

  private final ConfigurationComponentLocator componentLocator;

  public LookupFunction(ConfigurationComponentLocator componentLocator) {
    this.componentLocator = componentLocator;
  }

  @Override
  public Object call(Object[] parameters, BindingContext context) {
    String flowName = (String) parameters[0];
    Object payload = parameters[1];

    Location componentLocation = Location.builder().globalName(flowName).build();
    Component component = componentLocator.find(componentLocation)
        .orElseThrow(() -> new IllegalArgumentException(format("There is no component named '%s'.", flowName)));

    if (component instanceof Flow) {
      try {
        Message incomingMessage = lookupValue(context, MESSAGE, Message.builder().nullValue().build());
        Map<String, ?> incomingVariables = lookupValue(context, VARS, EMPTY_MAP);
        Error incomingError = lookupValue(context, ERROR, null);

        Message message = Message.builder(incomingMessage).value(payload).build();
        InternalEvent event = InternalEvent.builder(InternalEvent.getCurrentEvent().getContext())
            .variables(incomingVariables)
            .error(incomingError)
            .message(message)
            .build();
        MessagingExceptionHandler exceptionListener = ((Flow) component).getExceptionListener();

        InternalEvent result = from(processWithChildContext(event, (Flow) component, empty(), exceptionListener)).block();
        return result.getMessage().getPayload();
      } catch (Exception e) {
        MessagingException me = (MessagingException) unwrap(e);
        Error error = me.getEvent().getError().get();
        throw new MuleRuntimeException(createStaticMessage(format("Flow '%s' has failed with error '%s' (%s)",
                                                                  flowName,
                                                                  error.getErrorType(),
                                                                  error.getDescription())),
                                       error.getCause());
      }
    } else {
      throw new IllegalArgumentException(format("Component '%s' is not a flow.", flowName));
    }
  }

  @Override
  public Optional<DataType> returnType() {
    return of(TYPED_VALUE);
  }

  @Override
  public List<FunctionParameter> parameters() {
    return asList(new FunctionParameter("flowName", STRING),
                  new FunctionParameter("payload", OBJECT));
  }

  private <T> T lookupValue(BindingContext context, String binding, T fallback) {
    return context.lookup(binding).map(typedValue -> (T) typedValue.getValue()).orElse(fallback);
  }

}
