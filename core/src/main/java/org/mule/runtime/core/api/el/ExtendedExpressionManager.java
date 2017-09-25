/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.el;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.internal.message.InternalMessage;

/**
 * Extends the {@link ExpressionManager} supporting mutating operations and the propagation of more variables.
 * <p>
 * Only meant to distinguish the Mule 3 inherited behaviour from the current approach.
 *
 * @since 4.0
 */
public interface ExtendedExpressionManager extends ExpressionManager {

  /**
   * Execute the expression returning the result. The expression will be executed with MuleEvent context, meaning the expression
   * language implementation should provided access to the message.
   * <p>
   * This version of {@code evaluate} allows {@link CoreEvent} or {@link InternalMessage} mutation performed within the expression to
   * be maintained post-evaluation via the use of a result {@link CoreEvent.Builder} which should be created
   * from the original event before being passed and then used to construct the post-evaluation event.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param eventBuilder event builder instance used to mutate the current message or event.
   * @param componentLocation the location of the component where the event is being processed
   * @return the result of execution of the expression.
   * @deprecated Mutation via expressions is deprecated.
   */
  @Deprecated
  TypedValue evaluate(String expression, CoreEvent event, CoreEvent.Builder eventBuilder,
                      ComponentLocation componentLocation)
      throws ExpressionRuntimeException;

  /**
   * Enriches an event using a typed value.
   * <p>
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

  /**
   * Evaluates expressions in a given string. This method will iterate through each expression and evaluate it. If a user needs to
   * evaluate a single expression they can use {@link #evaluate(String, CoreEvent, FlowConstruct, BindingContext)}.
   * <p>
   * This version of {@code evaluate} performs expression evaulation on an immutable event. Any {@link CoreEvent} or
   * {@link InternalMessage} mutation performed within the expression will impact within the context of expression evaluation but
   * will not mutated the {@code event} parameter.
   *
   * @param expression one or more expressions ebedded in a literal string i.e. "Value is #[mel:xpath://foo] other value is
   *        #[mel:header:foo]."
   * @param event The current event being processed
   * @param componentLocation the location of the component where the event is being processed
   * @return the result of the evaluation
   * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and 'failIfNull is set
   *         to true.
   * @deprecated Parsing of expressions is deprecated. Use standard evaluation instead.
   */
  @Deprecated
  String parse(String expression, CoreEvent event, ComponentLocation componentLocation) throws ExpressionRuntimeException;

}
