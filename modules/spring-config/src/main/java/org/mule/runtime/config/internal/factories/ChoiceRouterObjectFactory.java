/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.factories;

import static java.util.Collections.emptyList;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.routing.ChoiceRouter;
import org.mule.runtime.core.internal.routing.ProcessorExpressionRoute;
import org.mule.runtime.core.internal.routing.ProcessorRoute;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;
import org.mule.runtime.tracer.customization.api.InitialSpanInfoProvider;

import java.util.Collection;

import javax.inject.Inject;

public class ChoiceRouterObjectFactory extends AbstractComponentFactory<ChoiceRouter> {

  @Inject
  private MuleContext muleContext;

  @Inject
  InitialSpanInfoProvider initialSpanInfoProvider;

  private Processor defaultProcessor;
  private Collection<ProcessorExpressionRoute> conditionalMessageProcessors = emptyList();

  public ChoiceRouterObjectFactory() {
    super();
  }

  public Class<?> getObjectType() {
    return ChoiceRouter.class;
  }

  public void setDefaultRoute(ProcessorRoute conditionalProcessor) {
    defaultProcessor = conditionalProcessor.getProcessor();
  }

  public void setRoutes(Collection<ProcessorExpressionRoute> conditionalMessageProcessors) {
    this.conditionalMessageProcessors = conditionalMessageProcessors;
  }

  @Override
  public ChoiceRouter doGetObject() throws Exception {
    final ChoiceRouter router = new ChoiceRouter(initialSpanInfoProvider);
    router.setAnnotations(getAnnotations());
    router.setDefaultRoute(defaultProcessor);
    router.setMuleContext(muleContext);

    for (final ProcessorExpressionRoute mper : conditionalMessageProcessors) {
      router.addRoute(mper.getExpression(), mper.getProcessor());
    }

    return router;
  }

}
