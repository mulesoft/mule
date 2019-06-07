/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.routing.ChoiceRouter;
import org.mule.runtime.core.internal.routing.ProcessorExpressionRoute;
import org.mule.runtime.core.internal.routing.ProcessorRoute;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import java.util.Collection;

import javax.inject.Inject;

public class ChoiceRouterObjectFactory extends AbstractComponentFactory<ChoiceRouter> {

  @Inject
  private MuleContext muleContext;

  private Processor defaultProcessor;
  private Collection<ProcessorExpressionRoute> conditionalMessageProcessors;

  public ChoiceRouterObjectFactory() {
    super();
  }

  public Class<?> getObjectType() {
    return ChoiceRouter.class;
  }

  public void setDefaultRoute(ProcessorRoute conditionalProcessor) {
    defaultProcessor = conditionalProcessor.getMessageProcessor();
  }

  public void setRoutes(Collection<ProcessorExpressionRoute> conditionalMessageProcessors) {
    this.conditionalMessageProcessors = conditionalMessageProcessors;
  }

  @Override
  public ChoiceRouter doGetObject() throws Exception {
    final ChoiceRouter router = new ChoiceRouter();
    router.setAnnotations(getAnnotations());
    router.setDefaultRoute(defaultProcessor);
    router.setMuleContext(muleContext);

    for (final ProcessorExpressionRoute mper : conditionalMessageProcessors) {
      router.addRoute(mper.getExpression(), mper.getMessageProcessor());
    }

    return router;
  }

}
