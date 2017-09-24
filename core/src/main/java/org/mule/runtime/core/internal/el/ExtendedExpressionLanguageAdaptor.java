/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.internal.message.InternalMessage;

/**
 * Extends the {@link ExpressionLanguageAdaptor} supporting mutating operations and the propagation of more variables. Only meant
 * to distinguish the Mule 3 inherited behaviour from the current approach.
 *
 * @since 4.0
 */
public interface ExtendedExpressionLanguageAdaptor extends ExpressionLanguageAdaptor {


  /**
   * Execute the expression returning the result. The expression will be executed with MuleEvent context, meaning the expression
   * language implementation should provided access to the message. A Map of variables can be provided that will be able to the
   * expression when executed. Variable provided in the map will only be available if there are no conflict with context variables
   * provided by the expression language implementation.
   *
   * This version of {@code evaluate} allows {@link CoreEvent} or {@link InternalMessage} mutation performed within the expression to
   * be maintained post-evaluation via the use of a result {@link CoreEvent.Builder} which should be created
   * from the original event before being passed and then used to construct the post-evaluation event.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param eventBuilder event builder instance used to mutate the current message or event.
   * @param componentLocation the location of the component where the event is being processed
   * @param bindingContext the bindings to consider
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   * @deprecated Mutation via expressions is deprecated.
   */
  @Deprecated
  TypedValue evaluate(String expression, CoreEvent event, CoreEvent.Builder eventBuilder,
                      ComponentLocation componentLocation,
                      BindingContext bindingContext)
      throws ExpressionRuntimeException;

  /**
   * Enriches an event.
   *
   * This version of {@code enrich} allows {@link CoreEvent} or {@link InternalMessage} mutation performed within the expression to be
   * maintained post-evaluation via the use of a result {@link CoreEvent.Builder} which should be created
   * from the original event before being passed and then used to construct the post-evaluation event.
   *
   * @param expression a single expression i.e. header://foo that defines how the message should be enriched
   * @param event The event to be enriched
   * @param eventBuilder event builder instance used to mutate the current message or event.
   * @param componentLocation the location of the component where the event is being processed
   * @param object The object used for enrichment
   * @deprecated Mutation via expressions is deprecated.
   */
  @Deprecated
  void enrich(String expression, CoreEvent event, CoreEvent.Builder eventBuilder, ComponentLocation componentLocation,
              Object object);

  /**
   * Enriches an event using a typed value.
   *
   * This version of {@code enrich} allows {@link CoreEvent} or {@link InternalMessage} mutation performed within the expression to be
   * maintained post-evaluation via the use of a result {@link CoreEvent.Builder} which should be created
   * from the original event before being passed and then used to construct the post-evaluation event.
   *
   * @param expression a single expression i.e. header://foo that defines how the message should be enriched
   * @param event The event to be enriched
   * @param eventBuilder event builder instance used to mutate the current message or event.
   * @param componentLocation the location of the component where the event is being processed
   * @param value The typed value used for enrichment
   * @deprecated Mutation via expressions is deprecated.
   */
  @Deprecated
  void enrich(String expression, CoreEvent event, CoreEvent.Builder eventBuilder, ComponentLocation componentLocation,
              TypedValue value);

}
