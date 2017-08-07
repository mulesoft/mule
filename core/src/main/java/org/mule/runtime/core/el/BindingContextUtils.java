/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.fromType;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.core.api.Event;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides a reusable way for creating {@link BindingContext}s.
 * 
 * @since 4.0
 */
public class BindingContextUtils {

  public static final String PAYLOAD = "payload";
  public static final String DATA_TYPE = "dataType";
  public static final String ATTRIBUTES = "attributes";
  public static final String ERROR = "error";
  public static final String CORRELATION_ID = "correlationId";
  public static final String VARS = "vars";
  public static final String PROPERTIES = "properties";
  public static final String PARAMETERS = "parameters";
  public static final String AUTHENTICATION = "authentication";

  public static final BindingContext NULL_BINDING_CONTEXT = BindingContext.builder().build();

  private BindingContextUtils() {
    // Nothing to do
  }

  /**
   * Creates a new {@link BindingContext} that contains the bindings from {@code baseContext} and the bindings that belong to the
   * given {@code event}.
   * 
   * @param event the event to build the new bindings for. Not-null.
   * @param baseContext the context whose copy the event bindings will be added to. Not-null.
   * @return a new {@link BindingContext} that contains the bindings from {@code baseContext} and the bindings that belong to the
   *         given {@code event}.
   */
  public static BindingContext addEventBindings(Event event, BindingContext baseContext) {
    requireNonNull(event);
    requireNonNull(baseContext);

    BindingContext.Builder contextBuilder = BindingContext.builder(baseContext);

    Map<String, TypedValue<?>> flowVars = new HashMap<>();
    event.getVariables().keySet().forEach(name -> {
      TypedValue<?> value = event.getVariables().get(name);
      flowVars.put(name, value);
    });
    contextBuilder.addBinding(VARS,
                              new TypedValue<>(unmodifiableMap(flowVars), DataType.builder()
                                  .mapType(flowVars.getClass())
                                  .keyType(String.class)
                                  .valueType(TypedValue.class)
                                  .build()));
    contextBuilder.addBinding(PROPERTIES,
                              new TypedValue<>(unmodifiableMap(event.getProperties()),
                                               fromType(event.getProperties().getClass())));
    contextBuilder.addBinding(PARAMETERS,
                              new TypedValue<>(unmodifiableMap(event.getParameters()),
                                               fromType(event.getParameters().getClass())));

    contextBuilder.addBinding(CORRELATION_ID, new TypedValue<>(event.getContext().getCorrelationId(), STRING));

    Message message = event.getMessage();
    contextBuilder.addBinding(ATTRIBUTES, message.getAttributes());
    contextBuilder.addBinding(PAYLOAD, message.getPayload());
    contextBuilder.addBinding(DATA_TYPE, new TypedValue<>(message.getPayload().getDataType(), fromType(DataType.class)));
    Error error = event.getError().isPresent() ? event.getError().get() : null;
    contextBuilder.addBinding(ERROR, new TypedValue<>(error, fromType(Error.class)));

    Authentication authentication = event.getSecurityContext() != null ? event.getSecurityContext().getAuthentication() : null;
    contextBuilder.addBinding(AUTHENTICATION, new TypedValue<>(authentication, fromType(Authentication.class)));

    return contextBuilder.build();
  }

}
