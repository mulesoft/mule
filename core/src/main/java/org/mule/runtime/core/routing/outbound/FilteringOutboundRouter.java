/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.outbound;

import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.runtime.core.api.routing.RoutePathNotFoundException;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.routing.TransformingMatchable;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.api.util.TemplateParser;

import java.util.LinkedList;
import java.util.List;

/**
 * <code>FilteringRouter</code> is a router that accepts events based on a filter set.
 */

public class FilteringOutboundRouter extends AbstractOutboundRouter implements TransformingMatchable {

  protected MuleExpressionLanguage expressionManager;

  private List<Transformer> transformers = new LinkedList<>();

  private Filter filter;

  private boolean useTemplates = true;

  // We used Square templates as they can exist as part of an URI.
  protected TemplateParser parser = TemplateParser.createSquareBracesStyleParser();

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    expressionManager = muleContext.getExpressionManager();
  }

  @Override
  public Event route(Event event) throws RoutingException {
    Event result;

    Message message = event.getMessage();

    if (routes == null || routes.size() == 0) {
      throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), null);
    }

    Processor ep = getRoute(0, event);

    try {
      result = sendRequest(event, createEventToRoute(event, message), ep, true);
    } catch (RoutingException e) {
      throw e;
    } catch (MuleException e) {
      throw new CouldNotRouteOutboundMessageException(ep, e);
    }
    return result;
  }

  public Filter getFilter() {
    return filter;
  }

  public void setFilter(Filter filter) {
    this.filter = filter;
  }

  @Override
  public boolean isMatch(Event event, Event.Builder builder) throws MuleException {
    if (getFilter() == null) {
      return true;
    }

    Message message = muleContext.getTransformationService().applyTransformers(event.getMessage(), null, transformers);
    event = Event.builder(event).message(message).build();
    builder.message(message);

    return getFilter().accept(event, builder);
  }

  public List<Transformer> getTransformers() {
    return transformers;
  }

  public void setTransformers(List<Transformer> transformers) {
    this.transformers = transformers;
  }

  /**
   * Will Return the target at the given index and will resolve any template tags on the Endpoint URI if necessary
   * 
   * @param index the index of the endpoint to get
   * @param event the current event. This is required if template matching is being used
   * @return the endpoint at the index, with any template tags resolved
   * @throws CouldNotRouteOutboundMessageException if the template causs the endpoint to become illegal or malformed
   */
  public Processor getRoute(int index, Event event) throws CouldNotRouteOutboundMessageException {
    if (!useTemplates) {
      return routes.get(index);
    } else {
      return getTemplateRoute(index, event);
    }
  }

  protected Processor getTemplateRoute(int index, Event event) throws CouldNotRouteOutboundMessageException {
    return routes.get(index);
  }

  public boolean isUseTemplates() {
    return useTemplates;
  }

  public void setUseTemplates(boolean useTemplates) {
    this.useTemplates = useTemplates;
  }

  @Override
  public boolean isTransformBeforeMatch() {
    return !transformers.isEmpty();
  }

}
